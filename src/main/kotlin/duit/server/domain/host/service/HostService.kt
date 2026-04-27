package duit.server.domain.host.service

import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import duit.server.infrastructure.external.file.FileStorageService
import jakarta.persistence.EntityExistsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class HostService(
    private val hostRepository: HostRepository,
    private val eventRepository: EventRepository,
    private val fileStorageService: FileStorageService
) {

    fun getHost(hostId: Long): Host = hostRepository.findByIdOrThrow(hostId)

    @Transactional
    fun createHost(name: String, thumbnail: MultipartFile?): HostResponse {
        if (hostRepository.findByName(name) != null) {
            throw EntityExistsException("이미 존재하는 호스트입니다 $name")
        }

        val thumbnailUrl = thumbnail?.let { fileStorageService.uploadFile(it, "hosts") }

        val host = Host(
            name = name,
            thumbnail = thumbnailUrl
        )

        return HostResponse.from(hostRepository.save(host))
    }

    @Transactional
    fun findOrCreateHost(request: HostRequest): Host {
        return hostRepository.findByName(request.name)
            ?: hostRepository.save(request.toEntity())
    }

    fun getHosts(param: HostPaginationParam): PageResponse<HostResponse> {
        val hostPage = hostRepository.findAll(param.toPageable())
            .map { HostResponse.from(it) }

        return PageResponse(
            content = hostPage.content,
            pageInfo = PageInfo.from(hostPage)
        )
    }

    @Transactional
    fun updateHost(
        hostId: Long,
        request: HostUpdateRequest,
        thumbnail: MultipartFile?
    ): HostResponse = getHost(hostId).let { host ->
        val thumbnailUrl = when {
            request.deleteThumbnail -> {
                host.thumbnail?.let { fileStorageService.deleteFile(it) }
                null
            }
            thumbnail != null -> {
                host.thumbnail?.let { fileStorageService.deleteFile(it) }
                fileStorageService.uploadFile(thumbnail, "hosts")
            }
            else -> host.thumbnail
        }

        host.name = request.name
        host.thumbnail = thumbnailUrl

        HostResponse.from(hostRepository.save(host))
    }

    @Transactional
    fun deleteHost(hostId: Long) {
        val host = hostRepository.findByIdOrThrow(hostId)
        check(!eventRepository.existsByHostId(hostId)) {
            "연결된 행사가 존재하여 주최 기관을 삭제할 수 없습니다."
        }
        host.thumbnail?.let { fileStorageService.deleteFile(it) }
        hostRepository.delete(host)
    }

    @Transactional
    fun deleteHosts(hostIds: List<Long>): Map<String, Any> {
        val hosts = hostRepository.findAllById(hostIds)
        val blockedIds = eventRepository.findHostIdsWithEvents(hostIds).toSet()
        val (blocked, deletable) = hosts.partition { it.id in blockedIds }

        deletable.forEach { host ->
            host.thumbnail?.let { fileStorageService.deleteFile(it) }
        }
        hostRepository.deleteAll(deletable)

        return mapOf(
            "deletedCount" to deletable.size,
            "blockedHosts" to blocked.map { mapOf("id" to it.id!!, "name" to it.name) },
        )
    }
}