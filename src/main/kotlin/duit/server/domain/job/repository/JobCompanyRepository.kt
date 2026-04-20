package duit.server.domain.job.repository

import duit.server.domain.job.entity.JobCompany
import org.springframework.data.jpa.repository.JpaRepository

interface JobCompanyRepository : JpaRepository<JobCompany, Long> {

    fun findByBusinessNumber(businessNumber: String): JobCompany?

    fun findByCorpNm(corpNm: String): JobCompany?
}
