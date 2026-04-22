package duit.server.domain.job.entity

import jakarta.persistence.*
import org.hibernate.annotations.Comment
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Comment("채용공고")
@Table(name = "job_postings")
class JobPosting(
    @Comment("채용공고 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Comment("고용24 구인인증번호")
    @Column(name = "wanted_auth_no", nullable = false, updatable = false, unique = true)
    val wantedAuthNo: String,

    @Comment("서비스 노출용 활성 여부")
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Comment("생성 시각")
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Comment("수정 시각")
    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "jobPosting", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: List<JobBookmark> = emptyList()

    @Comment("연결된 기업 정보 ID")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinColumn(name = "company_id")
    var company: Company? = null
        protected set

    @Comment("모집직종명")
    @Column(name = "jobs_nm")
    var jobsNm: String? = null

    @Comment("구인제목")
    @Column(name = "wanted_title", columnDefinition = "TEXT")
    var wantedTitle: String? = null

    @Comment("관련직종명")
    @Column(name = "rel_jobs_nm", columnDefinition = "TEXT")
    var relJobsNm: String? = null

    @Comment("직무내용")
    @Column(name = "job_cont", columnDefinition = "TEXT")
    var jobCont: String? = null

    @Comment("접수마감일 원문")
    @Column(name = "receipt_close_dt")
    var receiptCloseDt: String? = null

    @Comment("고용형태명")
    @Column(name = "emp_tp_nm")
    var empTpNm: String? = null

    @Comment("모집인원")
    @Column(name = "collect_psncnt")
    var collectPsncnt: String? = null

    @Comment("임금조건명")
    @Column(name = "sal_tp_nm")
    var salTpNm: String? = null

    @Comment("경력조건명")
    @Column(name = "enter_tp_nm")
    var enterTpNm: String? = null

    @Comment("학력")
    @Column(name = "edu_nm")
    var eduNm: String? = null

    @Comment("외국어 요건")
    @Column(name = "for_lang", columnDefinition = "TEXT")
    var forLang: String? = null

    @Comment("전공 요건")
    @Column(columnDefinition = "TEXT")
    var major: String? = null

    @Comment("자격면허")
    @Column(columnDefinition = "TEXT")
    var certificate: String? = null

    @Comment("병역특례채용희망")
    @Column(name = "mltsvc_exc_hope", columnDefinition = "TEXT")
    var mltsvcExcHope: String? = null

    @Comment("컴퓨터활용능력")
    @Column(name = "comp_abl", columnDefinition = "TEXT")
    var compAbl: String? = null

    @Comment("우대조건")
    @Column(name = "pf_cond", columnDefinition = "TEXT")
    var pfCond: String? = null

    @Comment("기타 우대조건")
    @Column(name = "etc_pf_cond", columnDefinition = "TEXT")
    var etcPfCond: String? = null

    @Comment("전형방법")
    @Column(name = "sel_mthd", columnDefinition = "TEXT")
    var selMthd: String? = null

    @Comment("접수방법")
    @Column(name = "rcpt_mthd", columnDefinition = "TEXT")
    var rcptMthd: String? = null

    @Comment("제출서류 준비물")
    @Column(name = "submit_doc", columnDefinition = "TEXT")
    var submitDoc: String? = null

    @Comment("기타안내")
    @Column(name = "etc_hope_cont", columnDefinition = "TEXT")
    var etcHopeCont: String? = null

    @Comment("근무예정지 원문")
    @Column(name = "work_region", columnDefinition = "TEXT")
    var workRegion: String? = null

    @Comment("인근 전철역")
    @Column(name = "near_line", columnDefinition = "TEXT")
    var nearLine: String? = null

    @Comment("근무시간/형태")
    @Column(name = "workday_workhr_cont", columnDefinition = "TEXT")
    var workdayWorkhrCont: String? = null

    @Comment("연금 및 4대보험")
    @Column(name = "four_ins", columnDefinition = "TEXT")
    var fourIns: String? = null

    @Comment("퇴직금")
    @Column(columnDefinition = "TEXT")
    var retirepay: String? = null

    @Comment("기타복리후생")
    @Column(name = "etc_welfare", columnDefinition = "TEXT")
    var etcWelfare: String? = null

    @Comment("장애인 편의시설")
    @Column(name = "disable_cvntl", columnDefinition = "TEXT")
    var disableCvntl: String? = null

    @Comment("회사소개 첨부파일 URL")
    @Column(name = "attach_file_url", length = 1000)
    var attachFileUrl: String? = null

    @Comment("상세모집내용 URL")
    @Column(name = "dtl_recr_cont_url", length = 1000)
    var dtlRecrContUrl: String? = null

    @Comment("직종코드")
    @Column(name = "jobs_cd")
    var jobsCd: String? = null

    @Comment("최소학력코드")
    @Column(name = "min_edubg_icd")
    var minEdubgIcd: String? = null

    @Comment("최대학력코드")
    @Column(name = "max_edubg_icd")
    var maxEdubgIcd: String? = null

    @Comment("근무지역코드")
    @Column(name = "region_cd")
    var regionCd: String? = null

    @Comment("고용형태코드")
    @Column(name = "emp_tp_cd")
    var empTpCd: String? = null

    @Comment("경력조건코드")
    @Column(name = "enter_tp_cd")
    var enterTpCd: String? = null

    @Comment("임금형태코드")
    @Column(name = "sal_tp_cd")
    var salTpCd: String? = null

    @Comment("근무지 지하철 지역코드")
    @Column(name = "sta_area_region_cd")
    var staAreaRegionCd: String? = null

    @Comment("근무지 지하철 호선코드")
    @Column(name = "line_cd")
    var lineCd: String? = null

    @Comment("근무지 지하철역코드")
    @Column(name = "sta_nm_cd")
    var staNmCd: String? = null

    @Comment("근무지 지하철역 출구번호")
    @Column(name = "exit_no_cd")
    var exitNoCd: String? = null

    @Comment("근무지 지하철역 출구거리코드")
    @Column(name = "walk_dist_cd")
    var walkDistCd: String? = null

    @Comment("채용부서")
    @Column(name = "emp_charger_dpt", columnDefinition = "TEXT")
    var empChargerDpt: String? = null

    @Comment("전화번호")
    @Column(name = "contact_telno")
    var contactTelno: String? = null

    @Comment("팩스번호")
    @Column(name = "charger_fax_no")
    var chargerFaxNo: String? = null

    @Comment("제출서류 양식 첨부파일 URL")
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "job_posting_corp_attach_list",
        joinColumns = [JoinColumn(name = "job_posting_id")],
    )
    @Column(name = "attach_file_url", length = 1000)
    val corpAttachList: MutableList<String> = mutableListOf()

    @Comment("검색 키워드")
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "job_posting_keyword_list",
        joinColumns = [JoinColumn(name = "job_posting_id")],
    )
    @Column(name = "srch_keyword_nm")
    val keywordList: MutableList<String> = mutableListOf()

    fun updateWork24Detail(
        detail: JobPostingWork24Detail,
        company: Company? = this.company,
    ) {
        jobsNm = detail.jobsNm
        wantedTitle = detail.wantedTitle
        relJobsNm = detail.relJobsNm
        jobCont = detail.jobCont
        receiptCloseDt = detail.receiptCloseDt
        empTpNm = detail.empTpNm
        collectPsncnt = detail.collectPsncnt
        salTpNm = detail.salTpNm
        enterTpNm = detail.enterTpNm
        eduNm = detail.eduNm
        forLang = detail.forLang
        major = detail.major
        certificate = detail.certificate
        mltsvcExcHope = detail.mltsvcExcHope
        compAbl = detail.compAbl
        pfCond = detail.pfCond
        etcPfCond = detail.etcPfCond
        selMthd = detail.selMthd
        rcptMthd = detail.rcptMthd
        submitDoc = detail.submitDoc
        etcHopeCont = detail.etcHopeCont
        workRegion = detail.workRegion
        nearLine = detail.nearLine
        workdayWorkhrCont = detail.workdayWorkhrCont
        fourIns = detail.fourIns
        retirepay = detail.retirepay
        etcWelfare = detail.etcWelfare
        disableCvntl = detail.disableCvntl
        attachFileUrl = detail.attachFileUrl
        dtlRecrContUrl = detail.dtlRecrContUrl
        jobsCd = detail.jobsCd
        minEdubgIcd = detail.minEdubgIcd
        maxEdubgIcd = detail.maxEdubgIcd
        regionCd = detail.regionCd
        empTpCd = detail.empTpCd
        enterTpCd = detail.enterTpCd
        salTpCd = detail.salTpCd
        staAreaRegionCd = detail.staAreaRegionCd
        lineCd = detail.lineCd
        staNmCd = detail.staNmCd
        exitNoCd = detail.exitNoCd
        walkDistCd = detail.walkDistCd
        empChargerDpt = detail.empChargerDpt
        contactTelno = detail.contactTelno
        chargerFaxNo = detail.chargerFaxNo

        corpAttachList.apply {
            clear()
            addAll(detail.corpAttachList.distinct())
        }
        keywordList.apply {
            clear()
            addAll(detail.keywordList.distinct())
        }

        this.company = company
    }

    fun changeCompany(company: Company?) {
        this.company = company
    }
}

data class JobPostingWork24Detail(
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
    val attachFileUrl: String? = null,
    val corpAttachList: List<String> = emptyList(),
    val keywordList: List<String> = emptyList(),
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
    val empChargerDpt: String? = null,
    val contactTelno: String? = null,
    val chargerFaxNo: String? = null,
)
