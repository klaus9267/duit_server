package duit.server.infrastructure.external.job.work24

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "wantedRoot")
data class Work24ApiResponse(
    val total: String? = null,
    val startPage: String? = null,
    val display: String? = null,
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "wanted")
    val wanted: List<WantedItem>? = null
) {
    data class WantedItem(
        val wantedAuthNo: String? = null,
        val company: String? = null,
        val busino: String? = null,
        val indTpNm: String? = null,
        val title: String? = null,
        val salTpNm: String? = null,
        val sal: String? = null,
        val minSal: String? = null,
        val maxSal: String? = null,
        val region: String? = null,
        val holidayTpNm: String? = null,
        val minEdubg: String? = null,
        val maxEdubg: String? = null,
        val career: String? = null,
        val regDt: String? = null,
        val closeDt: String? = null,
        val infoSvc: String? = null,
        val wantedInfoUrl: String? = null,
        val wantedMobileInfoUrl: String? = null,
        val zipCd: String? = null,
        val strtnmCd: String? = null,
        val basicAddr: String? = null,
        val detailAddr: String? = null,
        val empTpCd: String? = null,
        val jobsCd: String? = null,
        val smodifyDtm: String? = null
    )
}

/** 상세 API (callTp=D) 응답 — 루트 `wantedDtl` 아래 corpInfo / wantedInfo / empchargeInfo */
@JacksonXmlRootElement(localName = "wantedDtl")
data class Work24DetailResponse(
    val wantedAuthNo: String? = null,
    val corpInfo: CorpInfo? = null,
    val wantedInfo: WantedInfo? = null,
    val empchargeInfo: EmpchargeInfo? = null,
    /** 정보 없음 응답일 때 사용되는 필드 (있을 수 있음) */
    val message: String? = null,
    val messageCd: String? = null,
) {
    data class CorpInfo(
        val corpNm: String? = null,
        val reperNm: String? = null,
        val totPsncnt: String? = null,
        val capitalAmt: String? = null,
        val yrSalesAmt: String? = null,
        val indTpCdNm: String? = null,
        val busiCont: String? = null,
        val corpAddr: String? = null,
        val homePg: String? = null,
        val busiSize: String? = null,
    )

    data class WantedInfo(
        val jobsNm: String? = null,
        val wantedTitle: String? = null,
        val relJobsNm: String? = null,
        val jobCont: String? = null,
        val receiptCloseDt: String? = null,
        val empTpNm: String? = null,
        val collectPsncnt: String? = null,
        val salTpNm: String? = null,
        val enterTpNm: String? = null,
        val eduNm: String? = null,
        val forLang: String? = null,
        val major: String? = null,
        val certificate: String? = null,
        val mltsvcExcHope: String? = null,
        val compAbl: String? = null,
        val pfCond: String? = null,
        val etcPfCond: String? = null,
        val selMthd: String? = null,
        val rcptMthd: String? = null,
        val submitDoc: String? = null,
        val etcHopeCont: String? = null,
        val workRegion: String? = null,
        val nearLine: String? = null,
        val workdayWorkhrCont: String? = null,
        val fourIns: String? = null,
        val retirepay: String? = null,
        val etcWelfare: String? = null,
        val disableCvntl: String? = null,
        val dtlRecrContUrl: String? = null,
        val jobsCd: String? = null,
        val minEdubgIcd: String? = null,
        val maxEdubgIcd: String? = null,
        val regionCd: String? = null,
        val empTpCd: String? = null,
        val enterTpCd: String? = null,
        val salTpCd: String? = null,
        val staAreaRegionCd: String? = null,
        val lineCd: String? = null,
        val staNmCd: String? = null,
        val exitNoCd: String? = null,
        val walkDistCd: String? = null,
    )

    data class EmpchargeInfo(
        val empChargerDpt: String? = null,
        val contactTelno: String? = null,
        val chargerFaxNo: String? = null,
    )
}
