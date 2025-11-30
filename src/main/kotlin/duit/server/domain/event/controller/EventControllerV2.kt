package duit.server.domain.event.controller

import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventPaginationParamV2
import duit.server.domain.event.service.EventService
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
@RequestMapping("api/v2/events")
@Tag(name = "Event", description = "행사 관련 API")
class EventControllerV2(
    private val eventService: EventService
) {
    @GetMapping
    @Operation(
        summary = "행사 목록 조회 v2",
        description = "행사 목록을 페이지네이션으로 조회합니다. keyword 파라미터로 검색 가능"
    )
    @ResponseStatus(HttpStatus.OK)
    fun getEvents(
        @Valid @ParameterObject
        param: EventPaginationParamV2
    ) = eventService.getEventsV2(param)
}