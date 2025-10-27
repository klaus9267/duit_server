package duit.server.domain.host.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.host.service.HostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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

    @PutMapping("{hostId}")
    @Operation(summary = "주최측 수정 (관리자)", description = "주최 기관 정보를 수정합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun updateHost(
        @PathVariable hostId: Long,
        @Valid @RequestBody request: HostUpdateRequest
    ): HostResponse = hostService.updateHost(hostId, request)

    @DeleteMapping("{hostId}")
    @Operation(summary = "주최측 삭제 (관리자)", description = "주최 기관을 삭제합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteHost(@PathVariable hostId: Long) = hostService.deleteHost(hostId)
}