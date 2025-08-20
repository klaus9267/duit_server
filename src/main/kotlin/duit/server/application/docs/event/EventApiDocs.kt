package duit.server.application.docs.event

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "개발용 더미 이벤트 생성",
    description = """
개발 및 테스트 용도로 랜덤한 더미 이벤트를 생성합니다.

**주의사항:**
- 이 API는 개발 환경에서만 사용하세요
- 랜덤한 제목, 주최기관, 행사 유형으로 이벤트가 생성됩니다
- 생성된 이벤트는 자동으로 View(조회수) 엔티티도 함께 생성됩니다
"""
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "201",
            description = "이벤트 생성 성공"
        )
    ]
)
annotation class CreateEventApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "행사 목록 조회",
    description = """
행사 목록을 페이지네이션으로 조회합니다.

**조회 조건:**
- 행사 승인 여부 필터링 가능
- 행사 유형별 필터링 가능
- 주최기관별 필터링 가능
- 페이지 단위 조회 (기본 20개)

**정렬 기준:**
- 기본: 행사 시작일 내림차순
"""
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "행사 목록 조회 성공",
            useReturnTypeSchema = true
        )
    ]
)
annotation class GetEventsApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    summary = "북마크한 행사 달력 조회",
    description = """
현재 로그인한 사용자가 북마크한 행사들을 월별 달력 형태로 조회합니다.

**기능:**
- 특정 년월의 행사만 필터링
- 북마크한 행사만 조회
- 행사 유형별 필터링 가능

**사용 목적:**
- 달력 UI에서 북마크한 행사 표시
- 개인 일정 관리
""",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "달력 행사 조회 성공",
            useReturnTypeSchema = true
        )
    ]
)
annotation class GetEventsForCalendarApi