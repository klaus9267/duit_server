package duit.server.domain.job.entity

/**
 * 17개 광역자치단체 기반 근무지역.
 * location(원문 주소 문자열)과 분리하여 필터링 전용으로 사용.
 * API 응답의 지역 코드 또는 주소 문자열에서 파싱하여 저장.
 */
enum class WorkRegion(val displayName: String) {
    SEOUL("서울"),
    BUSAN("부산"),
    DAEGU("대구"),
    INCHEON("인천"),
    GWANGJU("광주"),
    DAEJEON("대전"),
    ULSAN("울산"),
    SEJONG("세종"),
    GYEONGGI("경기"),
    GANGWON("강원"),
    CHUNGBUK("충북"),
    CHUNGNAM("충남"),
    JEONBUK("전북"),
    JEONNAM("전남"),
    GYEONGBUK("경북"),
    GYEONGNAM("경남"),
    JEJU("제주"),
    ETC("기타"),
}
