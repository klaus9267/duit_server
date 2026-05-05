package duit.server.domain.job.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.job.dto.CompanyBookmarkToggleResponse
import duit.server.domain.job.service.CompanyBookmarkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/company-bookmarks")
@Tag(name = "Company Bookmark", description = "회사 북마크 관련 API")
class CompanyBookmarkController(
    private val companyBookmarkService: CompanyBookmarkService,
) {

    @PostMapping("{companyId}")
    @Operation(summary = "회사 북마크 토글", description = "회사를 북마크하거나 북마크를 해제합니다.")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun toggleBookmark(@PathVariable companyId: Long): CompanyBookmarkToggleResponse =
        companyBookmarkService.toggleBookmark(companyId)
}
