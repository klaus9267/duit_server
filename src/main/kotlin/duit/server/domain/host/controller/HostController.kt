package duit.server.domain.host.controller

import duit.server.application.common.RequireAdmin
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.host.dto.HostDeleteRequest
import duit.server.domain.host.dto.HostPaginationParam
import duit.server.domain.host.dto.HostResponse
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.host.service.HostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

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

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "주최측 생성 (관리자)", description = "새로운 주최 기관을 생성합니다")
    @RequireAdmin
    @ResponseStatus(HttpStatus.CREATED)
    fun createHost(
        @Parameter
        @NotBlank(message = "주최 기관명은 필수입니다")
        name: String,
        @RequestPart("thumbnail", required = false)
        @Parameter(description = "주최 기관 로고 이미지")
        thumbnail: MultipartFile?
    ): HostResponse = hostService.createHost(name, thumbnail)

    @PutMapping("{hostId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "주최측 수정 (관리자)", description = "주최 기관 정보를 수정합니다")
    @RequireAdmin
    @ResponseStatus(HttpStatus.OK)
    fun updateHost(
        @PathVariable hostId: Long,
        @Valid @RequestPart("data") request: HostUpdateRequest,
        @RequestPart("thumbnail", required = false)
        @Parameter(description = "주최 기관 로고 이미지")
        thumbnail: MultipartFile?
    ): HostResponse = hostService.updateHost(hostId, request, thumbnail)

    @DeleteMapping("{hostId}")
    @Operation(summary = "주최측 삭제 (관리자)", description = "주최 기관을 삭제합니다. 연결된 행사가 있으면 409 Conflict를 반환합니다.")
    @RequireAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteHost(@PathVariable hostId: Long) = hostService.deleteHost(hostId)

    @DeleteMapping
    @Operation(
        summary = "주최측 일괄 삭제 (관리자)",
        description = "여러 주최 기관을 일괄 삭제합니다. 연결된 행사가 있는 주최 기관은 스킵되어 응답의 blockedHosts에 포함됩니다."
    )
    @RequireAdmin
    @ResponseStatus(HttpStatus.OK)
    fun deleteHosts(@Valid @RequestBody request: HostDeleteRequest): Map<String, Any> =
        hostService.deleteHosts(request.hostIds)
}