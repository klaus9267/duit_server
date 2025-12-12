package duit.server.domain.event.controller

import duit.server.domain.event.dto.EventCursorPaginationParam
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
@Tag(name = "Event")
class EventControllerV2(
    private val eventService: EventService
) {
    @GetMapping
    @Operation(
        summary = "행사 목록 조회 V2",
        description = """
            커서 기반 페이지네이션으로 행사 목록을 조회합니다.

            **사용법:**
            1. 첫 페이지: cursor 없이 요청
            2. 다음 페이지: 응답의 pageInfo.nextCursor를 cursor 파라미터로 전달
            3. 마지막 페이지: pageInfo.hasNext = false

            **주의사항:**
            - 페이지 번호 점프는 지원하지 않습니다 (순차 탐색만 가능)
            - status와 statusGroup 중 하나만 사용 가능합니다
        """
    )
    @ResponseStatus(HttpStatus.OK)
    fun getEvents(
        @Valid @ParameterObject
        param: EventCursorPaginationParam
    ) = eventService.getEvents(param)

    @GetMapping("count")
    @Operation(
        summary = "승인 행사 총 갯수 조회",
        description = "미승인 행사를 제외한 승인된 행사들의 총 갯수를 조회합니다."
    )
    @ResponseStatus(HttpStatus.OK)
    fun getTotalCount() = eventService.countActiveEvents()
}