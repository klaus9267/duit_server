package duit.server.domain.host.controller

import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.host.service.HostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/hosts")
@Tag(name = "Host", description = "주최측 관련 API")
class HostController(
    private val hostService: HostService
) {

    @GetMapping
    @Operation(summary = "주최측 목록 조회", description = "주최 기관 목록을 페이징하여 조회합니다")
    @ResponseStatus(HttpStatus.OK)
    fun getHosts(
        @Valid @ParameterObject
        param: HostPaginationParam
    ): PageResponse<HostResponse> = hostService.getHosts(param)
}