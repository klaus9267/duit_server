package duit.server.domain.job.dto

import duit.server.domain.job.entity.JobPosting
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "채용공고 응답")
data class JobPostingResponse(
    @Schema(description = "채용공고 ID", example = "1")
    val id: Long,

    @Schema(description = "고용24 구인인증번호", example = "K123456")
    val wantedAuthNo: String,

    @Schema(description = "공고 활성 여부", example = "true")
    val isActive: Boolean,

    @Schema(description = "기업 정보")
    val company: JobCompanyResponse?,

    @Schema(description = "모집직종명", example = "간호사")
    val jobsNm: String?,

    @Schema(description = "구인제목", example = "병동 간호사 모집")
    val wantedTitle: String?,

    @Schema(description = "관련 직종명", example = "간호조무사")
    val relJobsNm: String?,

    @Schema(description = "직무내용")
    val jobCont: String?,

    @Schema(description = "접수마감일 원문", example = "2026-04-30")
    val receiptCloseDt: String?,

    @Schema(description = "고용형태명", example = "기간의 정함이 없는 근로계약")
    val empTpNm: String?,

    @Schema(description = "모집인원", example = "2명")
    val collectPsncnt: String?,

    @Schema(description = "임금조건명", example = "연봉 5,000만원 이상")
    val salTpNm: String?,

    @Schema(description = "경력조건명", example = "경력")
    val enterTpNm: String?,

    @Schema(description = "학력명", example = "학사")
    val eduNm: String?,

    @Schema(description = "외국어 요구사항")
    val forLang: String?,

    @Schema(description = "전공 요건")
    val major: String?,

    @Schema(description = "자격면허 요건")
    val certificate: String?,

    @Schema(description = "병역특례 채용 희망 조건")
    val mltsvcExcHope: String?,

    @Schema(description = "컴퓨터 활용 능력 요건")
    val compAbl: String?,

    @Schema(description = "우대조건")
    val pfCond: String?,

    @Schema(description = "기타 우대조건")
    val etcPfCond: String?,

    @Schema(description = "전형방법")
    val selMthd: String?,

    @Schema(description = "접수방법")
    val rcptMthd: String?,

    @Schema(description = "제출서류 준비물")
    val submitDoc: String?,

    @Schema(description = "기타 안내")
    val etcHopeCont: String?,

    @Schema(description = "근무예정지 원문", example = "서울특별시 강남구")
    val workRegion: String?,

    @Schema(description = "인근 전철역")
    val nearLine: String?,

    @Schema(description = "근무시간/형태")
    val workdayWorkhrCont: String?,

    @Schema(description = "4대 보험 및 연금 정보")
    val fourIns: String?,

    @Schema(description = "퇴직금 정보")
    val retirepay: String?,

    @Schema(description = "기타 복리후생")
    val etcWelfare: String?,

    @Schema(description = "장애인 편의시설")
    val disableCvntl: String?,

    @Schema(description = "회사소개 첨부파일 URL")
    val attachFileUrl: String?,

    @Schema(description = "제출서류 양식 첨부파일 URL 목록")
    val corpAttachList: List<String>,

    @Schema(description = "검색 키워드 목록")
    val keywordList: List<String>,

    @Schema(description = "상세모집내용 URL")
    val dtlRecrContUrl: String?,

    @Schema(description = "직종코드", example = "304000")
    val jobsCd: String?,

    @Schema(description = "최소학력코드", example = "05")
    val minEdubgIcd: String?,

    @Schema(description = "최대학력코드", example = "07")
    val maxEdubgIcd: String?,

    @Schema(description = "근무지역코드", example = "11680")
    val regionCd: String?,

    @Schema(description = "고용형태코드", example = "10")
    val empTpCd: String?,

    @Schema(description = "경력조건코드", example = "E")
    val enterTpCd: String?,

    @Schema(description = "임금형태코드", example = "Y")
    val salTpCd: String?,

    @Schema(description = "근무지 지하철 지역코드")
    val staAreaRegionCd: String?,

    @Schema(description = "근무지 지하철 호선코드")
    val lineCd: String?,

    @Schema(description = "근무지 지하철역코드")
    val staNmCd: String?,

    @Schema(description = "근무지 지하철역 출구번호")
    val exitNoCd: String?,

    @Schema(description = "근무지 지하철역 출구거리코드")
    val walkDistCd: String?,

    @Schema(description = "채용 담당 부서")
    val empChargerDpt: String?,

    @Schema(description = "채용 문의 전화번호")
    val contactTelno: String?,

    @Schema(description = "채용 문의 팩스번호")
    val chargerFaxNo: String?,

    @Schema(description = "현재 사용자의 북마크 여부", example = "false")
    val isBookmarked: Boolean,

    @Schema(description = "생성 시각", example = "2026-04-16T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각", example = "2026-04-16T10:30:00")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(jobPosting: JobPosting, isBookmarked: Boolean = false) = JobPostingResponse(
            id = jobPosting.id!!,
            wantedAuthNo = jobPosting.wantedAuthNo,
            isActive = jobPosting.isActive,
            company = jobPosting.company?.let(JobCompanyResponse::from),
            jobsNm = jobPosting.jobsNm,
            wantedTitle = jobPosting.wantedTitle,
            relJobsNm = jobPosting.relJobsNm,
            jobCont = jobPosting.jobCont,
            receiptCloseDt = jobPosting.receiptCloseDt,
            empTpNm = jobPosting.empTpNm,
            collectPsncnt = jobPosting.collectPsncnt,
            salTpNm = jobPosting.salTpNm,
            enterTpNm = jobPosting.enterTpNm,
            eduNm = jobPosting.eduNm,
            forLang = jobPosting.forLang,
            major = jobPosting.major,
            certificate = jobPosting.certificate,
            mltsvcExcHope = jobPosting.mltsvcExcHope,
            compAbl = jobPosting.compAbl,
            pfCond = jobPosting.pfCond,
            etcPfCond = jobPosting.etcPfCond,
            selMthd = jobPosting.selMthd,
            rcptMthd = jobPosting.rcptMthd,
            submitDoc = jobPosting.submitDoc,
            etcHopeCont = jobPosting.etcHopeCont,
            workRegion = jobPosting.workRegion,
            nearLine = jobPosting.nearLine,
            workdayWorkhrCont = jobPosting.workdayWorkhrCont,
            fourIns = jobPosting.fourIns,
            retirepay = jobPosting.retirepay,
            etcWelfare = jobPosting.etcWelfare,
            disableCvntl = jobPosting.disableCvntl,
            attachFileUrl = jobPosting.attachFileUrl,
            corpAttachList = jobPosting.corpAttachList.toList(),
            keywordList = jobPosting.keywordList.toList(),
            dtlRecrContUrl = jobPosting.dtlRecrContUrl,
            jobsCd = jobPosting.jobsCd,
            minEdubgIcd = jobPosting.minEdubgIcd,
            maxEdubgIcd = jobPosting.maxEdubgIcd,
            regionCd = jobPosting.regionCd,
            empTpCd = jobPosting.empTpCd,
            enterTpCd = jobPosting.enterTpCd,
            salTpCd = jobPosting.salTpCd,
            staAreaRegionCd = jobPosting.staAreaRegionCd,
            lineCd = jobPosting.lineCd,
            staNmCd = jobPosting.staNmCd,
            exitNoCd = jobPosting.exitNoCd,
            walkDistCd = jobPosting.walkDistCd,
            empChargerDpt = jobPosting.empChargerDpt,
            contactTelno = jobPosting.contactTelno,
            chargerFaxNo = jobPosting.chargerFaxNo,
            isBookmarked = isBookmarked,
            createdAt = jobPosting.createdAt,
            updatedAt = jobPosting.updatedAt,
        )
    }
}
