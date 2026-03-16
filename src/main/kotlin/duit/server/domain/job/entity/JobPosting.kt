package duit.server.domain.job.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "job_postings")
class JobPosting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // ── 출처 ───────────────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    val sourceType: SourceType,

    /** 원본 API의 공고 ID. sourceType과 복합 unique. */
    @Column(nullable = false, updatable = false)
    val externalId: String,

    // ── 공고 기본 정보 ───────────────────────────────────────────────────────────────────
    var title: String,
    var companyName: String,

    /** 직종명. 예: "간호사", "간호조무사" */
    var jobCategory: String?,

    // ── 근무지 ───────────────────────────────────────────────────────────────────────────
    /** 원문 주소. UI 표시 전용. 예: "서웸특별시 강남구 역삼동" */
    var location: String?,

    /**
     * 필터링용 광역자치단체 단위 지역.
     * location 원문을 파싱하여 저장. null = 파싱 실패 또는 미제공.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var workRegion: WorkRegion?,

    /**
     * 필터링용 시/군/구 단위 지역. 예: "강남구", "분당구".
     * 사람인 loc_bcd 코드, 고용24 region 5자리 코드에서 파싱하여 저장.
     */
    var workDistrict: String?,

    // ── 근무 조건 ───────────────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var employmentType: EmploymentType?,

    /** 경력 최소 년수. null = 경력무관 또는 신입 포함. */
    var careerMin: Int?,

    /** 경력 최대 년수. null = 상한 없음. */
    var careerMax: Int?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var educationLevel: EducationLevel?,

    // ── 급여 ───────────────────────────────────────────────────────────────────────────
    /**
     * 급여 하한 (만원 단위).
     * null = 미공개 / "회사 내규" / "면접 후 결정".
     * 사람인 salary code → 만원 단위 변환, 고용24 sal 필드 직접 사용.
     */
    @Column(name = "salary_min")
    var salaryMin: Long?,

    /**
     * 급여 상한 (만원 단위).
     * 사람인체럼 범위 제공 시 상한값, 고용24체럼 단일값이면 salaryMin과 동일.
     * null = 상한 없음 또는 미공개.
     */
    @Column(name = "salary_max")
    var salaryMax: Long?,

    /**
     * 급여 유형.
     * 사람인은 항상 ANNUAL, 고용24는 ANNUAL/MONTHLY/HOURLY 혼재.
     * null = 미공개.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var salaryType: SalaryType?,

    @Column(columnDefinition = "TEXT", nullable = false)
    var postingUrl: String,

    // ── 일정 ───────────────────────────────────────────────────────────────────────────
    var postedAt: LocalDateTime?,

    /**
     * 마감일. closeType 이 ON_HIRE 또는 ONGOING 이면 null.
     * 스케줄러가 FIXED 공고의 만료 여부를 이 값으로 판단.
     */
    var expiresAt: LocalDateTime?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var closeType: CloseType,

    /**
     * 공고 활성 여부.
     * - FIXED: 스케줄러가 expiresAt 기준으로 주기적으로 갱신
     * - ON_HIRE / ONGOING: 소스 API 응답값으로 갱신
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    // ── 고용24 전용 ──────────────────────────────────────────────────────────────────────────
    /** 주 소정근로시간 (시간). */
    var workHoursPerWeek: Int?,

    // ── 감사 필드 ──────────────────────────────────────────────────────────────────────────
    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @OneToMany(mappedBy = "jobPosting", cascade = [CascadeType.ALL], orphanRemoval = true)
    val bookmarks: List<JobBookmark> = emptyList()

    // ── 도메인 로직 ──────────────────────────────────────────────────────────────────────────

    /**
     * 스케줄러 sync 시 변경 가능한 모든 필드를 한 번에 업데이트.
     * 불변 필드(sourceType, externalId)는 제외.
     */
    fun updateFromSource(
        title: String,
        companyName: String,
        jobCategory: String?,
        location: String?,
        workRegion: WorkRegion?,
        workDistrict: String?,
        employmentType: EmploymentType?,
        careerMin: Int?,
        careerMax: Int?,
        educationLevel: EducationLevel?,
        salaryMin: Long?,
        salaryMax: Long?,
        salaryType: SalaryType?,
        postingUrl: String,
        postedAt: LocalDateTime?,
        expiresAt: LocalDateTime?,
        closeType: CloseType,
        isActive: Boolean,
        workHoursPerWeek: Int?,
    ) {
        this.title = title
        this.companyName = companyName
        this.jobCategory = jobCategory
        this.location = location
        this.workRegion = workRegion
        this.workDistrict = workDistrict
        this.employmentType = employmentType
        this.careerMin = careerMin
        this.careerMax = careerMax
        this.educationLevel = educationLevel
        this.salaryMin = salaryMin
        this.salaryMax = salaryMax
        this.salaryType = salaryType
        this.postingUrl = postingUrl
        this.postedAt = postedAt
        this.expiresAt = expiresAt
        this.closeType = closeType
        this.isActive = isActive
        this.workHoursPerWeek = workHoursPerWeek
    }

    /**
     * FIXED 타입 공고의 만료 여부를 현재 시각 기준으로 갱신.
     * ON_HIRE / ONGOING 은 소스 API 응답값을 updateFromSource 에서 직접 반영하므로 여기서 처리하지 않음.
     */
    fun syncActiveStatus(now: LocalDateTime) {
        if (closeType == CloseType.FIXED) {
            isActive = expiresAt?.isAfter(now) ?: true
        }
    }

    /** 경력 조건 표시 문자열. DTO 변환 시 활용. */
    val careerDescription: String
        get() = when {
            careerMin == null && careerMax == null -> "경력무관"
            careerMin == null && careerMax != null -> "경력무관"
            careerMin == 0 && careerMax == null -> "신입"
            careerMin != null && careerMax == null -> "경력 ${careerMin}년 이상"
            else -> "경력 ${careerMin}~${careerMax}년"
        }

    /** 급여 표시 문자열. DTO 변환 시 활용. DB에는 원 단위로 저장되며, 만원 단위로 변환하여 표시. */
    val salaryDescription: String
        get() {
            if (salaryMin == null) return "급여 미공개"
            val type = salaryType?.displayName
            val effectiveMax = salaryMax?.takeIf { it > 0 }
            val amount = if (effectiveMax != null && salaryMin != effectiveMax) {
                "${toManWon(salaryMin!!)}~${toManWon(effectiveMax)}만원"
            } else {
                "${toManWon(salaryMin!!)}만원"
            }
            return if (type != null) "$type $amount" else amount
        }

    private fun toManWon(value: Long): Long = value / 10000
}