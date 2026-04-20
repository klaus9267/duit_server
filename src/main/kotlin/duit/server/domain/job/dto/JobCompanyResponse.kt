package duit.server.domain.job.dto

import duit.server.domain.job.entity.JobCompany
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "채용공고 기업 정보 응답")
data class JobCompanyResponse(
    @Schema(description = "기업 정보 ID", example = "1")
    val id: Long,

    @Schema(description = "회사명", example = "테스트병원")
    val corpNm: String?,

    @Schema(description = "대표자명", example = "홍길동")
    val reperNm: String?,

    @Schema(description = "근로자 수", example = "120")
    val totPsncnt: Long?,

    @Schema(description = "자본금")
    val capitalAmt: Long?,

    @Schema(description = "연매출액")
    val yrSalesAmt: Long?,

    @Schema(description = "업종명")
    val indTpCdNm: String?,

    @Schema(description = "주요사업내용")
    val busiCont: String?,

    @Schema(description = "회사주소")
    val corpAddr: String?,

    @Schema(description = "회사 홈페이지 URL")
    val homePg: String?,

    @Schema(description = "회사규모")
    val busiSize: String?,

    @Schema(description = "생성 시각", example = "2026-04-16T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각", example = "2026-04-16T10:30:00")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(company: JobCompany) = JobCompanyResponse(
            id = company.id!!,
            corpNm = company.corpNm,
            reperNm = company.reperNm,
            totPsncnt = company.totPsncnt,
            capitalAmt = company.capitalAmt,
            yrSalesAmt = company.yrSalesAmt,
            indTpCdNm = company.indTpCdNm,
            busiCont = company.busiCont,
            corpAddr = company.corpAddr,
            homePg = company.homePg,
            busiSize = company.busiSize,
            createdAt = company.createdAt,
            updatedAt = company.updatedAt,
        )
    }
}
