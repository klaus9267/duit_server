package duit.server.domain.job.repository

import duit.server.domain.job.entity.Company
import org.springframework.data.jpa.repository.JpaRepository

interface JobCompanyRepository : JpaRepository<Company, Long> {

    fun findByBusinessNumber(businessNumber: String): Company?

    fun findByCorpNm(corpNm: String): Company?
}
