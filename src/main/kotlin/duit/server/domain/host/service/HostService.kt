package duit.server.domain.host.service

import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.common.extensions.findByIdOrThrow
import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import duit.server.infrastructure.external.file.FileStorageService
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional(readOnly = true)
class HostService(
    private val hostRepository: HostRepository,
    private val fileStorageService: FileStorageService
) {

    fun getHost(hostId: Long): Host =
        hostRepository.findByIdOrThrow(hostId, "주최 기관")

    @Transactional
    fun createHost(name: String, thumbnail: MultipartFile?): HostResponse {
        require(hostRepository.findByName(name) == null) {
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
        val host = hostRepository.findById(hostId)
            .orElseThrow { EntityNotFoundException("주최 기관을 찾을 수 없습니다: $hostId") }

        hostRepository.delete(host)
    }

    @Transactional
    fun deleteHosts(hostIds: List<Long>) {
        hostIds.forEach { hostId ->
            hostRepository.findById(hostId).ifPresent { host ->
                host.thumbnail?.let { fileStorageService.deleteFile(it) }
                hostRepository.delete(host)
            }
        }
    }
}