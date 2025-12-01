package duit.server.domain.alarm.controller

import duit.server.application.common.RequireAuth
import duit.server.application.security.SecurityUtil
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponse
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.common.dto.pagination.PageResponse
import duit.server.domain.event.service.EventService
import duit.server.domain.user.service.UserService
import duit.server.infrastructure.external.firebase.FCMService
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
    private val fcmService: FCMService,
    private val securityUtil: SecurityUtil,
    private val userService: UserService,
    private val alarmService: AlarmService,
    private val eventService: EventService
) {

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징하여 조회합니다 (isRead 필터링 가능)")
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

    @PostMapping("/test/custom")
    @Operation(summary = "커스텀 푸시 알림 테스트", description = "사용자 디바이스로 커스텀 푸시 알림을 전송합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun sendCustomTestNotification(
        @RequestParam title: String,
        @RequestParam body: String,
    ) {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = userService.findUserById(currentUserId)

        if (user.deviceToken.isNullOrBlank()) {
            throw RuntimeException("디바이스 토큰이 없습니다")
        }

        fcmService.sendAlarms(
            deviceTokens = listOf(user.deviceToken!!),
            title = title,
            body = body,
            data = mapOf("type" to "custom_test")
        )
    }

    @PostMapping("/test/custom2")
    @Operation(summary = "행사 푸시 알림 테스트", description = "특정 행사에 대한 푸시 알림을 전송합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun sendCustomTestNotification2(
        @RequestParam alarmType: AlarmType,
        @RequestParam eventId: Long,
    ) {
        val currentUserId = securityUtil.getCurrentUserId()
        val user = userService.findUserById(currentUserId)

        if (user.deviceToken.isNullOrBlank()) {
            throw RuntimeException("디바이스 토큰이 없습니다")
        }

        val event = eventService.getEvent(eventId)

        alarmService.createAlarms(alarmType, event.id!!)
    }
}