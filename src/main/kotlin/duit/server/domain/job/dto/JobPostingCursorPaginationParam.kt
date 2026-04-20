package duit.server.domain.job.dto

import duit.server.domain.job.entity.CloseType
import duit.server.domain.job.entity.EducationLevel
import duit.server.domain.job.entity.EmploymentType
import duit.server.domain.job.entity.SalaryType
import duit.server.domain.job.entity.WorkRegion
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "채용공고 커서 기반 페이지네이션 파라미터")
data class JobPostingCursorPaginationParam(
    @get:Parameter(description = "다음 페이지 커서 (Base64 인코딩). 첫 페이지는 null")
    val cursor: String? = null,

    @get:Parameter(
        description = "페이지 크기 (1~100)",
        example = "10"
    )
    @get:Schema(minimum = "1", maximum = "100", defaultValue = "10")
    val size: Int = 10,

    @get:Parameter(description = """근무 지역 필터 (다중 선택 가능)
- SEOUL: 서울
- BUSAN: 부산
- DAEGU: 대구
- INCHEON: 인천
- GWANGJU: 광주
- DAEJEON: 대전
- ULSAN: 울산
- SEJONG: 세종
- GYEONGGI: 경기
- GANGWON: 강원
- CHUNGBUK: 충북
- CHUNGNAM: 충남
- JEONBUK: 전북
- JEONNAM: 전남
- GYEONGBUK: 경북
- GYEONGNAM: 경남
- JEJU: 제주
- ETC: 기타""")
    val workRegions: List<WorkRegion>? = null,

    @get:Parameter(description = """고용 형태 필터 (다중 선택 가능)
- FULL_TIME: 정규직
- CONTRACT: 계약직
- PART_TIME: 파트타임
- DISPATCH: 파견직
- INTERN: 인턴
- ETC: 기타""")
    val employmentTypes: List<EmploymentType>? = null,

    @get:Parameter(description = """학력 필터 (단일 선택)
- NONE: 학력무관
- HIGH_SCHOOL: 고졸
- ASSOCIATE: 전문대졸
- BACHELOR: 4년제졸
- MASTER: 석사졸
- DOCTOR: 박사졸""")
    val educationLevel: EducationLevel? = null,

    @get:Parameter(description = """급여 유형 필터 (단일 선택)
- ANNUAL: 연봉
- MONTHLY: 월급
- HOURLY: 시급""")
    val salaryType: SalaryType? = null,

    @get:Parameter(description = """마감 유형 필터 (다중 선택 가능)
- FIXED: 마감일
- ON_HIRE: 채용시
- ONGOING: 상시""")
    val closeTypes: List<CloseType>? = null,

    @get:Parameter(description = "검색 키워드 (공고 제목 + 회사명 검색)")
    val searchKeyword: String? = null,

    @get:Parameter(
        description = "북마크한 공고만 조회 (로그인 필요)",
        example = "false"
    )
    @get:Schema(defaultValue = "false")
    val bookmarked: Boolean = false,
) {
    init {
        require(size in 1..100) {
            "size는 1 이상 100 이하여야 합니다 (현재: $size)"
        }
    }
}
