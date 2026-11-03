package duit.server.domain.admin.controller

import duit.server.application.common.RequireAdmin
import duit.server.domain.admin.dto.AdminPaginationParam
import duit.server.domain.admin.dto.AdminResponse
import duit.server.domain.admin.service.AdminService
import duit.server.domain.common.dto.pagination.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/admins")
@Tag(name = "Admin", description = "관리자 계정 관리 API")
class AdminController(
    private val adminService: AdminService
) {

    @GetMapping
    @Operation(
        summary = "관리자 목록 조회 (관리자)",
        description = "관리자로 등록된 사용자를 페이징하여 조회합니다. 관리자 ID, 사용자 ID, 닉네임, 이메일, 등록 일시를 포함합니다."
    )
    @RequireAdmin
    @ResponseStatus(HttpStatus.OK)
    fun getAdmins(
        @Valid @ParameterObject param: AdminPaginationParam
    ): PageResponse<AdminResponse> = adminService.getAdmins(param)

    @DeleteMapping("{id}")
    @Operation(
        summary = "관리자 해제 (관리자)",
        description = "다른 관리자의 관리자 권한을 해제합니다. 사용자 계정은 유지됩니다. 본인은 해제할 수 없고(400), 존재하지 않으면 404를 반환합니다."
    )
    @RequireAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeAdmin(
        @Parameter(description = "관리자 목록 조회 응답의 id", example = "1")
        @PathVariable id: Long
    ) = adminService.revokeAdmin(id)
}
