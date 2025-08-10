package duit.server.application.docs.event

import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.dto.EventResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
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
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = PageResponse::class),
                examples = [ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "title": "응급실 간호실무 향상 워크숍",
                                "startAt": "2024-12-15",
                                "endAt": "2024-12-16",
                                "recruitmentStartAt": "2024-12-01T09:00:00",
                                "recruitmentEndAt": "2024-12-10T18:00:00",
                                "uri": "https://example.com/event/123",
                                "thumbnail": "https://example.com/image.jpg",
                                "eventType": "WORKSHOP",
                                "host": {
                                    "id": 1,
                                    "name": "서울대학교병원",
                                    "thumbnail": "https://example.com/logo.jpg"
                                },
                                "isApproved": true
                            }
                        ],
                        "pageInfo": {
                            "currentPage": 0,
                            "totalPages": 5,
                            "totalElements": 100,
                            "pageSize": 20,
                            "hasNext": true,
                            "hasPrevious": false
                        }
                    }
                    """
                )]
            )]
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
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = EventResponse::class)),
                examples = [ExampleObject(
                    name = "성공 응답",
                    value = """
                    [
                        {
                            "id": 1,
                            "title": "응급실 간호실무 향상 워크숍",
                            "startAt": "2024-12-15",
                            "endAt": "2024-12-16",
                            "eventType": "WORKSHOP",
                            "host": {
                                "name": "서울대학교병원"
                            }
                        }
                    ]
                    """
                )]
            )]
        )
    ]
)
annotation class GetEventsForCalendarApi