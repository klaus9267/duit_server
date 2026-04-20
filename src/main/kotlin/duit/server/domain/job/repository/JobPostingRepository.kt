package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobPosting
import org.springframework.data.jpa.repository.JpaRepository

interface JobPostingRepository : JpaRepository<JobPosting, Long>, JobPostingRepositoryCustom {

    fun findByWantedAuthNo(wantedAuthNo: String): JobPosting?
}
