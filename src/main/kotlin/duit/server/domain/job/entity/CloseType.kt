package duit.server.domain.job.entity

enum class CloseType(val displayName: String) {
    /** 특정 마감일이 명시된 공고 */
    FIXED("마감일"),

    /** 채용 확정 시 즉시 마감 — expiresAt null */
    ON_HIRE("채용시"),

    /** 상시 채용 — expiresAt null */
    ONGOING("상시"),
}
