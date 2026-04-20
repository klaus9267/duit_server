package duit.server.domain.job.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.job.dto.JobPostingCursor
import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.entity.*
import org.springframework.stereotype.Repository

@Repository
class JobPostingRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : JobPostingRepositoryCustom {

    private val jobPosting = QJobPosting.jobPosting
    private val jobBookmark = QJobBookmark.jobBookmark
    private val jobCompany = QJobCompany.jobCompany
    private val jobPostingCompany = Expressions.path(JobCompany::class.java, jobPosting, "company")
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
            conditions += param.workRegions
                .map { jobPosting.workRegion.startsWith(it.displayName) }
                .reduceOrNull(BooleanExpression::or)
        }

        if (!param.employmentTypes.isNullOrEmpty()) {
            conditions += param.employmentTypes
                .map { jobPosting.empTpNm.containsIgnoreCase(it.displayName) }
                .reduceOrNull(BooleanExpression::or)
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
                    duit.server.domain.job.entity.CloseType.FIXED -> jobPosting.receiptCloseDt.isNotNull
                        .and(jobPosting.receiptCloseDt.containsIgnoreCase("채용시까지").not())
                        .and(jobPosting.receiptCloseDt.containsIgnoreCase("상시").not())
                    duit.server.domain.job.entity.CloseType.ON_HIRE -> jobPosting.receiptCloseDt.containsIgnoreCase("채용시까지")
                    duit.server.domain.job.entity.CloseType.ONGOING -> jobPosting.receiptCloseDt.containsIgnoreCase("상시")
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

    private fun buildCursorCondition(cursor: JobPostingCursor?): BooleanExpression? {
        if (cursor == null) return null

        return jobPosting.id.lt(cursor.id)
    }
}
