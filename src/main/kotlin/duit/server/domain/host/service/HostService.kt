package duit.server.domain.host.service

import duit.server.application.controller.dto.host.HostPaginationParam
import duit.server.application.controller.dto.host.HostRequest
import duit.server.application.controller.dto.host.HostResponse
import duit.server.application.controller.dto.pagination.PageInfo
import duit.server.application.controller.dto.pagination.PageResponse
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import org.springframework.stereotype.Service

@Service
class HostService(private val hostRepository: HostRepository) {

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
}