package duit.server.domain.alarm.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponse
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.common.dto.pagination.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/alarms")
@Tag(name = "Alarm", description = "알림 관련 API")
class AlarmController(
    private val alarmService: AlarmService,
) {

    @GetMapping
    @Operation(
        summary = "알림 목록 조회 (V1)",
        description = "사용자의 **이벤트 알림만** 페이징하여 조회합니다 (isRead 필터링 가능). " +
            "채용공고 알림은 본 엔드포인트에서 제외되며 V2 (`GET /api/v2/alarms`) 에서 폴리모픽 응답으로 조회 가능합니다."
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getAlarms(
        @Valid @ParameterObject
        param: AlarmPaginationParam
    ): PageResponse<AlarmResponse> {
        return alarmService.getAlarms(param)
    }

    @PatchMapping("{alarmId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAsRead(@PathVariable alarmId: Long) {
        alarmService.markAsRead(alarmId)
    }

    @PatchMapping("read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "현재 사용자의 모든 알림을 읽음 처리합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllAsRead() {
        alarmService.markAllAsRead()
    }

    @DeleteMapping("{alarmId}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAlarm(@PathVariable alarmId: Long) {
        alarmService.deleteAlarm(alarmId)
    }

    @DeleteMapping
    @Operation(
        summary = "알림 전체 삭제",
        description = "알림을 삭제합니다. readOnly=true면 읽은 알림만, false면 전체 삭제"
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAlarms(
        @RequestParam(defaultValue = "false") readOnly: Boolean
    ) {
        alarmService.deleteAlarms(readOnly)
    }
}
