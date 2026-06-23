package duit.server.domain.job.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.job.dto.JobPostingCursor
import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.Company
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.QCompany
import duit.server.domain.job.entity.QJobBookmark
import duit.server.domain.job.entity.QJobPosting
import duit.server.domain.job.entity.WorkRegion
import org.springframework.stereotype.Repository

@Repository
class JobPostingRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : JobPostingRepositoryCustom {

    private val jobPosting = QJobPosting.jobPosting
    private val jobBookmark = QJobBookmark.jobBookmark
    private val jobCompany = QCompany.company
    private val jobPostingCompany = Expressions.path(Company::class.java, jobPosting, "company")
    private val jobPostingCompanyId = Expressions.numberPath(Long::class.javaObjectType, jobPostingCompany, "id")

    override fun findJobPostings(param: JobPostingCursorPaginationParam, currentUserId: Long?): List<JobPosting> {
        val cursor = param.cursor?.let(JobPostingCursor::decode)

        val query = queryFactory
            .selectFrom(jobPosting)
            .apply {
                if (param.bookmarked && currentUserId != null) {
                    join(jobPosting.bookmarks, jobBookmark)
                        .on(jobBookmark.user().id.eq(currentUserId))
                }
            }

        val conditions = mutableListOf<BooleanExpression?>()
        conditions += jobPosting.isActive.isTrue

        if (!param.workRegions.isNullOrEmpty()) {
            conditions += buildWorkRegionCondition(param.workRegions)
        }

        if (!param.employmentTypes.isNullOrEmpty()) {
            conditions += buildEmploymentTypeCondition(param.employmentTypes)
        }

        param.educationLevel?.let {
            conditions += jobPosting.eduNm.containsIgnoreCase(it.displayName)
        }

        param.salaryType?.let {
            conditions += jobPosting.salTpNm.containsIgnoreCase(it.displayName)
        }

        if (!param.closeTypes.isNullOrEmpty()) {
            conditions += param.closeTypes.map {
                when (it) {
                    CloseType.FIXED -> jobPosting.receiptCloseDt.isNotNull
                        .and(jobPosting.receiptCloseDt.containsIgnoreCase("채용시까지").not())
                        .and(jobPosting.receiptCloseDt.containsIgnoreCase("상시").not())
                    CloseType.ON_HIRE -> jobPosting.receiptCloseDt.containsIgnoreCase("채용시까지")
                    CloseType.ONGOING -> jobPosting.receiptCloseDt.containsIgnoreCase("상시")
                }
            }.reduceOrNull(BooleanExpression::or)
        }

        param.searchKeyword?.let { keyword ->
            conditions += jobPosting.wantedTitle.containsIgnoreCase(keyword)
                .or(jobPosting.jobsNm.containsIgnoreCase(keyword))
                .or(jobPosting.jobCont.containsIgnoreCase(keyword))
                .or(
                    JPAExpressions
                        .selectOne()
                        .from(jobCompany)
                        .where(
                            jobCompany.id.eq(jobPostingCompanyId),
                            jobCompany.corpNm.containsIgnoreCase(keyword),
                        )
                        .exists()
                )
        }

        conditions += buildCursorCondition(cursor)

        return query
            .where(*conditions.filterNotNull().toTypedArray())
            .orderBy(jobPosting.id.desc())
            .limit(param.size.toLong() + 1)
            .fetch()
    }

    private fun buildWorkRegionCondition(workRegions: List<WorkRegion>): BooleanExpression? =
        workRegions.flatMap { region ->
            val codeConditions = workRegionCodePrefixes[region].orEmpty()
                .map { jobPosting.regionCd.startsWith(it) }
            val missingCodeCondition = jobPosting.regionCd.isNull.or(jobPosting.regionCd.eq(""))
            val textConditions = workRegionAliases[region].orEmpty()
                .map { missingCodeCondition.and(buildWorkRegionTextCondition(it)) }

            codeConditions + textConditions
        }.reduceOrNull(BooleanExpression::or)

    private fun buildEmploymentTypeCondition(employmentTypes: List<EmploymentType>): BooleanExpression? =
        employmentTypes.flatMap { employmentType ->
            val codeConditions = employmentTypeCodeValues[employmentType].orEmpty()
                .plus(employmentType.name)
                .map { jobPosting.empTpCd.equalsIgnoreCase(it) }
            val missingCodeCondition = jobPosting.empTpCd.isNull.or(jobPosting.empTpCd.eq(""))
            val textConditions = employmentTypeAliases[employmentType].orEmpty()
                .map { missingCodeCondition.and(jobPosting.empTpNm.containsIgnoreCase(it)) }

            codeConditions + textConditions
        }.reduceOrNull(BooleanExpression::or)

    private fun buildWorkRegionTextCondition(alias: String): BooleanExpression =
        listOf(
            jobPosting.workRegion.startsWith(alias),
            jobPosting.workRegion.like("(_____)$alias%"),
            jobPosting.workRegion.like("(_____) $alias%"),
            jobPosting.workRegion.like("(_____)  $alias%"),
            jobPosting.workRegion.like("_____$alias%"),
            jobPosting.workRegion.like("_____ $alias%"),
            jobPosting.workRegion.like("_____  $alias%"),
        ).reduce(BooleanExpression::or)

    private fun buildCursorCondition(cursor: JobPostingCursor?): BooleanExpression? {
        if (cursor == null) return null

        return jobPosting.id.lt(cursor.id)
    }
}

private val workRegionCodePrefixes = mapOf(
    WorkRegion.SEOUL to listOf("11"),
    WorkRegion.BUSAN to listOf("26"),
    WorkRegion.DAEGU to listOf("27"),
    WorkRegion.INCHEON to listOf("28"),
    WorkRegion.GWANGJU to listOf("29"),
    WorkRegion.DAEJEON to listOf("30"),
    WorkRegion.ULSAN to listOf("31"),
    WorkRegion.SEJONG to listOf("36"),
    WorkRegion.GYEONGGI to listOf("41"),
    WorkRegion.GANGWON to listOf("42", "51"),
    WorkRegion.CHUNGBUK to listOf("43"),
    WorkRegion.CHUNGNAM to listOf("44"),
    WorkRegion.JEONBUK to listOf("45", "52"),
    WorkRegion.JEONNAM to listOf("46"),
    WorkRegion.GYEONGBUK to listOf("47"),
    WorkRegion.GYEONGNAM to listOf("48"),
    WorkRegion.JEJU to listOf("50"),
)

private val workRegionAliases = mapOf(
    WorkRegion.SEOUL to listOf("서울", "서울특별시"),
    WorkRegion.BUSAN to listOf("부산", "부산광역시"),
    WorkRegion.DAEGU to listOf("대구", "대구광역시"),
    WorkRegion.INCHEON to listOf("인천", "인천광역시"),
    WorkRegion.GWANGJU to listOf("광주", "광주광역시"),
    WorkRegion.DAEJEON to listOf("대전", "대전광역시"),
    WorkRegion.ULSAN to listOf("울산", "울산광역시"),
    WorkRegion.SEJONG to listOf("세종", "세종특별자치시"),
    WorkRegion.GYEONGGI to listOf("경기", "경기도"),
    WorkRegion.GANGWON to listOf("강원", "강원도", "강원특별자치도"),
    WorkRegion.CHUNGBUK to listOf("충북", "충청북도"),
    WorkRegion.CHUNGNAM to listOf("충남", "충청남도"),
    WorkRegion.JEONBUK to listOf("전북", "전라북도", "전북특별자치도"),
    WorkRegion.JEONNAM to listOf("전남", "전라남도"),
    WorkRegion.GYEONGBUK to listOf("경북", "경상북도"),
    WorkRegion.GYEONGNAM to listOf("경남", "경상남도"),
    WorkRegion.JEJU to listOf("제주", "제주도", "제주특별자치도"),
    WorkRegion.ETC to listOf("기타"),
)

private val employmentTypeCodeValues = mapOf(
    EmploymentType.FULL_TIME to listOf("10", "11"),
    EmploymentType.CONTRACT to listOf("20", "21"),
    EmploymentType.DISPATCH to listOf("4"),
)

private val employmentTypeAliases = mapOf(
    EmploymentType.FULL_TIME to listOf("정규직", "기간의 정함이 없는 근로계약"),
    EmploymentType.CONTRACT to listOf("계약직", "기간의 정함이 있는 근로계약"),
    EmploymentType.PART_TIME to listOf("파트타임", "시간제", "아르바이트"),
    EmploymentType.DISPATCH to listOf("파견직"),
    EmploymentType.INTERN to listOf("인턴"),
    EmploymentType.ETC to listOf("기타"),
)
