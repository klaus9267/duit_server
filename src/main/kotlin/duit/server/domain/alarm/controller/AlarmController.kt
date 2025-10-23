package duit.server.domain.alarm.controller

import duit.server.application.security.SecurityUtil
import duit.server.domain.alarm.dto.AlarmPaginationParam
import duit.server.domain.alarm.dto.AlarmResponse
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.common.docs.AuthApiResponses
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
    @Operation(
        summary = "알람 목록 조회",
        description = "현재 로그인한 사용자의 알람 목록을 페이징하여 조회합니다. 최신순으로 정렬됩니다."
    )
    @AuthApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun getAlarms(
        @Valid @ParameterObject
        param: AlarmPaginationParam
    ): PageResponse<AlarmResponse> {
        return alarmService.getAlarms(param)
    }

    @PostMapping("/test/custom")
    @Operation(
        summary = "커스텀 푸시 알림 테스트",
        description = "현재 로그인한 사용자의 디바이스로 지정한 제목과 내용의 테스트 푸시 알림을 전송합니다."
    )
    @AuthApiResponses
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
    @Operation(
        summary = "푸시 알림 테스트",
        description = "현재 로그인한 사용자의 디바이스로 특정 행사 푸시 알림을 전송합니다."
    )
    @AuthApiResponses
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

        alarmService.createAlarms(alarmType, event)
    }
}