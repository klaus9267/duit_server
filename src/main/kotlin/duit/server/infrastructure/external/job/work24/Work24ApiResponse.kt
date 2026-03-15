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
        val wantedInfoUrl: String? = null,
        val wantedMobileInfoUrl: String? = null,
        val empTpCd: String? = null,
        val jobsCd: String? = null,
        val smodifyDtm: String? = null
    )
}
