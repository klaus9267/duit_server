package duit.server.infrastructure.external.job.dto

import duit.server.domain.job.entity.JobPostingWork24Detail

/** 외부 소스에서 수집한 공고 1건 */
data class JobFetchResult(
    val externalId: String,
    val isActive: Boolean,
    val detail: JobPostingWork24Detail,
    val company: CompanyFetchResult,
)

/** 외부 소스에서 수집한 회사 프로필 */
data class CompanyFetchResult(
    val businessNumber: String? = null,
    val corpNm: String? = null,
    val reperNm: String? = null,
    val totPsncnt: Long? = null,
    val capitalAmt: Long? = null,
    val yrSalesAmt: Long? = null,
    val indTpCdNm: String? = null,
    val busiCont: String? = null,
    val corpAddr: String? = null,
    val homePg: String? = null,
    val busiSize: String? = null,
)
