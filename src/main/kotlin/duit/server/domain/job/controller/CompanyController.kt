package duit.server.domain.job.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.job.dto.JobCompanyResponse
import duit.server.domain.job.service.CompanyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/companies")
@Tag(name = "Company", description = "회사 정보 관련 API")
class CompanyController(
    private val companyService: CompanyService,
) {

    @GetMapping("bookmarked")
    @Operation(
        summary = "내가 북마크한 회사 목록 조회",
        description = "현재 로그인한 사용자가 북마크한 회사 목록을 최신 북마크 순으로 조회합니다."
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getBookmarkedCompanies(): List<JobCompanyResponse> = companyService.getBookmarkedCompanies()

    @GetMapping("{companyId}")
    @Operation(
        summary = "회사 상세 조회",
        description = "회사 상세 정보를 조회합니다. 인증된 사용자가 호출하면 isBookmarked 가 채워집니다."
    )
    @ResponseStatus(HttpStatus.OK)
    fun getCompanyDetail(@PathVariable companyId: Long): JobCompanyResponse =
        companyService.getCompanyDetail(companyId)
}
