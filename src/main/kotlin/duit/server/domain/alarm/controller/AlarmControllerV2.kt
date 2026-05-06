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
@Tag(name = "Alarm V2", description = "알림 V2 — 이벤트/채용 폴리모픽 응답")
class AlarmControllerV2(
    private val alarmService: AlarmService,
) {

    @GetMapping
    @Operation(
        summary = "알림 목록 조회 (V2)",
        description = "이벤트 + 채용공고 알람을 모두 반환. `target.targetType` 으로 클라이언트가 분기."
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
