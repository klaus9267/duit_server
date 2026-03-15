package duit.server.domain.job.entity

enum class EducationLevel(val displayName: String) {
    NONE("학력무관"),
    HIGH_SCHOOL("고졸"),
    ASSOCIATE("전문대졸"),   // 2~3년제
    BACHELOR("4년제졸"),
    MASTER("석사졸"),
    DOCTOR("박사졸"),
}
