package duit.server.domain.alarm.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponseV2
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.common.dto.pagination.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v2/alarms")
@Tag(
    name = "Alarm V2",
    description = """
        알림 V2 — 이벤트/채용공고 폴리모픽 응답.

        **V1 vs V2:**
        - V1 (`api/v1/alarms`): event 필드 non-null 알람만 반환 (이벤트 전용). 기존 클라이언트 후방호환.
        - V2 (`api/v2/alarms`): 이벤트 + 채용공고 알람 모두 반환. `target` 폴리모픽으로 클라이언트가 분기.

        **읽음 처리 / 삭제 등은 V1 (`api/v1/alarms`) 엔드포인트 공유** — type 무관.
    """,
)
class AlarmControllerV2(
    private val alarmService: AlarmService,
) {

    @GetMapping
    @Operation(
        summary = "알림 목록 조회 (V2)",
        description = """
            현재 인증 사용자의 알림을 페이징하여 반환한다.
            **이벤트 알림 + 채용공고 알림 모두 포함** (V1 은 이벤트 전용).

            **응답 (200 OK):** [PageResponse]<[AlarmResponseV2]>. 각 알람은 다음 구조:
            - `id`, `isRead`, `createdAt`
            - `type`: [AlarmType] — 알림 발생 사유
              - 북마크 기반: `EVENT_START` / `RECRUITMENT_START` / `RECRUITMENT_END`
              - 구독 기반: `EVENT_SUBSCRIPTION_KEYWORD` / `_HOST` / `_TYPE`, `JOB_SUBSCRIPTION_KEYWORD` / `_COMPANY`
            - `target`: [AlarmTargetResponse] sealed (oneOf):
              - `targetType=EVENT`        → `event: EventResponse` (행사 정보)
              - `targetType=JOB_POSTING`  → `jobPosting: JobPostingResponse` (채용공고 정보)

            클라이언트는 `target.targetType` 으로 분기해 해당 필드 접근.
            정렬: 페이지네이션 파라미터 (`field=ID`, `DESC` 기본) — 최신 알람 순.

            **쿼리** ([AlarmPaginationParam]):
            - `page` (기본 0), `size` (기본 10): 오프셋 페이징
            - `isRead` (선택): `true`=읽음만, `false`=안읽음만, 미지정=전체

            **관련 엔드포인트** (V1 공유, type 무관):
            - 단건 읽음: `PATCH /api/v1/alarms/{alarmId}/read`
            - 전체 읽음: `PATCH /api/v1/alarms/read-all`
            - 단건 삭제: `DELETE /api/v1/alarms/{alarmId}`
            - 전체 삭제: `DELETE /api/v1/alarms?readOnly=true|false`

            **예외:**
            - 401 UNAUTHORIZED: 인증 없음
        """,
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getAlarms(
        @Valid @ParameterObject
        param: AlarmPaginationParam,
    ): PageResponse<AlarmResponseV2> {
        return alarmService.getAlarmsV2(param)
    }
}
