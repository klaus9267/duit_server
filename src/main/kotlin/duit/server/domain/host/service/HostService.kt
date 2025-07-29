package duit.server.domain.host.service

import duit.server.application.controller.dto.host.HostRequest
import duit.server.application.controller.dto.host.HostResponse
import duit.server.application.controller.dto.pagination.PageInfo
import duit.server.application.controller.dto.pagination.PaginationParam
import duit.server.application.controller.dto.pagination.PaginationResponse
import duit.server.domain.host.repository.HostRepository
import org.springframework.stereotype.Service

@Service
class HostService(private val hostRepository: HostRepository) {

    fun createHost(request: HostRequest) = hostRepository.save(request.toEntity())

    fun getHosts(param: PaginationParam): PaginationResponse<HostResponse> {
        val hostPage = hostRepository.findAll(param.toPageable())
            .map { HostResponse.from(it) }

        return PaginationResponse(
            content = hostPage.content,
            pageInfo = PageInfo.from(hostPage)
        )
    }
}