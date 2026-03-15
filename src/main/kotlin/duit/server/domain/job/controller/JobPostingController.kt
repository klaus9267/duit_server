package duit.server.domain.job.controller

import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.service.JobPostingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/job-postings")
@Tag(name = "Job Posting", description = "채용공고 관련 API")
class JobPostingController(private val jobPostingService: JobPostingService) {

    @GetMapping
    @Operation(
        summary = "채용공고 목록 조회",
        description = """
            커서 기반 페이지네이션으로 채용공고 목록을 조회합니다.

            **사용법:**
            1. 첫 페이지: cursor 없이 요청
            2. 다음 페이지: 응답의 pageInfo.nextCursor를 cursor 파라미터로 전달
            3. 마지막 페이지: pageInfo.hasNext = false

            **주의사항:**
            - 페이지 번호 점프는 지원하지 않습니다 (순차 탐색만 가능)
        """
    )
    @ResponseStatus(HttpStatus.OK)
    fun getJobPostings(@Valid @ParameterObject param: JobPostingCursorPaginationParam) =
        jobPostingService.getJobPostings(param)

    @GetMapping("{jobPostingId}")
    @Operation(summary = "채용공고 상세 조회", description = "채용공고 상세 정보를 조회합니다.")
    @ResponseStatus(HttpStatus.OK)
    fun getJobPostingDetail(@PathVariable jobPostingId: Long) =
        jobPostingService.getJobPostingDetail(jobPostingId)
}
