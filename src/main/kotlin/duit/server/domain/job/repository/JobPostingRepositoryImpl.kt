package duit.server.domain.job.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import duit.server.domain.job.dto.JobPostingCursor
import duit.server.domain.job.dto.JobPostingCursorPaginationParam
import duit.server.domain.job.dto.JobPostingSortField
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.job.entity.QJobBookmark
import duit.server.domain.job.entity.QJobPosting
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class JobPostingRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager
) : JobPostingRepositoryCustom {

    private val jobPosting = QJobPosting.jobPosting
    private val jobBookmark = QJobBookmark.jobBookmark

    override fun findJobPostings(param: JobPostingCursorPaginationParam, currentUserId: Long?): List<JobPosting> {
        val cursor = param.cursor?.let { JobPostingCursor.decode(it, param.field) }

        val query = queryFactory
            .selectFrom(jobPosting)
            .buildWhere(param, currentUserId, cursor)
            .buildOrderBy(param)

        return query.limit(param.size.toLong() + 1).fetch()
    }

    private fun <T> JPAQuery<T>.buildWhere(
        param: JobPostingCursorPaginationParam,
        currentUserId: Long?,
        cursor: JobPostingCursor?
    ): JPAQuery<T> {
        if (param.bookmarked && currentUserId != null) {
            this.join(jobPosting.bookmarks, jobBookmark)
                .on(jobBookmark.user().id.eq(currentUserId))
        }

        val conditions = mutableListOf<BooleanExpression?>()

        conditions.add(jobPosting.isActive.isTrue)

        if (!param.workRegions.isNullOrEmpty()) {
            conditions.add(jobPosting.workRegion.`in`(param.workRegions))
        }

        if (!param.employmentTypes.isNullOrEmpty()) {
            conditions.add(jobPosting.employmentType.`in`(param.employmentTypes))
        }

        param.educationLevel?.let {
            conditions.add(jobPosting.educationLevel.eq(it))
        }

        param.salaryType?.let {
            conditions.add(jobPosting.salaryType.eq(it))
        }

        if (!param.closeTypes.isNullOrEmpty()) {
            conditions.add(jobPosting.closeType.`in`(param.closeTypes))
        }

        param.searchKeyword?.let { keyword ->
            conditions.add(
                jobPosting.title.containsIgnoreCase(keyword)
                    .or(jobPosting.companyName.containsIgnoreCase(keyword))
            )
        }

        when (param.field) {
            JobPostingSortField.EXPIRES_AT -> conditions.add(jobPosting.expiresAt.isNotNull)
            JobPostingSortField.SALARY -> conditions.add(jobPosting.salaryMin.isNotNull)
            else -> Unit
        }

        conditions.add(buildCursorCondition(cursor))

        return this.where(*conditions.filterNotNull().toTypedArray())
    }

    private fun buildCursorCondition(cursor: JobPostingCursor?): BooleanExpression? {
        if (cursor == null) return null

        return when (cursor) {
            is JobPostingCursor.CreatedAtCursor ->
                jobPosting.createdAt.lt(cursor.createdAt)
                    .or(jobPosting.createdAt.eq(cursor.createdAt).and(jobPosting.id.lt(cursor.id)))

            is JobPostingCursor.ExpiresAtCursor ->
                jobPosting.expiresAt.gt(cursor.expiresAt)
                    .or(jobPosting.expiresAt.eq(cursor.expiresAt).and(jobPosting.id.lt(cursor.id)))

            is JobPostingCursor.SalaryCursor ->
                jobPosting.salaryMin.lt(cursor.salaryMin)
                    .or(jobPosting.salaryMin.eq(cursor.salaryMin).and(jobPosting.id.lt(cursor.id)))
        }
    }

    private fun <T> JPAQuery<T>.buildOrderBy(param: JobPostingCursorPaginationParam): JPAQuery<T> {
        val orderSpecifiers = when (param.field) {
            JobPostingSortField.CREATED_AT -> arrayOf(jobPosting.createdAt.desc(), jobPosting.id.desc())
            JobPostingSortField.EXPIRES_AT -> arrayOf(jobPosting.expiresAt.asc(), jobPosting.id.desc())
            JobPostingSortField.SALARY -> arrayOf(jobPosting.salaryMin.desc(), jobPosting.id.desc())
        }

        return this.orderBy(*orderSpecifiers)
    }
}
