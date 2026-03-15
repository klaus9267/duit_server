package duit.server.domain.job.entity

enum class EmploymentType(val displayName: String) {
    FULL_TIME("정규직"),
    CONTRACT("계약직"),
    PART_TIME("파트타임"),
    DISPATCH("파견직"),
    INTERN("인턴"),
    ETC("기타"),
}
