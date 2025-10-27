package duit.server.domain.host.service

import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.common.dto.pagination.PageInfo
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class HostService(private val hostRepository: HostRepository) {

    fun getHost(hostId: Long): Host =
        hostRepository.findById(hostId)
            .orElseThrow { EntityNotFoundException("주최 기관을 찾을 수 없습니다: $hostId") }

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
    fun updateHost(hostId: Long, request: HostUpdateRequest): HostResponse {
        val host = hostRepository.findById(hostId)
            .orElseThrow { EntityNotFoundException("주최 기관을 찾을 수 없습니다: $hostId") }

        host.name = request.name
        host.thumbnail = request.thumbnail

        return HostResponse.from(hostRepository.save(host))
    }

    @Transactional
    fun deleteHost(hostId: Long) {
        val host = hostRepository.findById(hostId)
            .orElseThrow { EntityNotFoundException("주최 기관을 찾을 수 없습니다: $hostId") }

        hostRepository.delete(host)
    }
}