package duit.server.domain.common.dto.pagination

enum class PaginationField(val displayName: String) {
    ID("id"),
    NAME("name"),
    
    START_DATE("startAt"), // 행사 시작일 임박
    RECRUITMENT_DEADLINE("recruitmentEndAt"), // 모집 마감 임박  
    VIEW_COUNT("view.count"), // 조회수 많은순
    CREATED_AT("createdAt"), // 최신 등록순
    ;
}