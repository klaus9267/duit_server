package duit.server.domain.job.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.job.service.JobBookmarkService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/job-bookmarks")
@Tag(name = "Job Bookmark", description = "채용공고 북마크 관련 API")
class JobBookmarkController(private val jobBookmarkService: JobBookmarkService) {

    @PostMapping("{jobPostingId}")
    @RequireAuth
    @Operation(summary = "채용공고 북마크 토글", description = "채용공고를 북마크하거나 북마크를 해제합니다.")
    @ResponseStatus(HttpStatus.OK)
    fun toggleBookmark(@PathVariable jobPostingId: Long) =
        jobBookmarkService.toggleBookmark(jobPostingId)
}
