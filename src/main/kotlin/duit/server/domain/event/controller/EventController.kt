package duit.server.domain.event.controller

import duit.server.domain.common.docs.AuthApiResponses
import duit.server.domain.common.docs.CommonApiResponses
import duit.server.domain.event.controller.docs.*
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.service.EventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("api/v1/events")
@Tag(name = "Event", description = "행사 관련 API")
class EventController(
    private val eventService: EventService
) {
    @PostMapping
    @CreateEventApi
    @CommonApiResponses
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent(
        @Valid @RequestBody eventRequest: EventRequest,
    ) = eventService.createEvent4Admin(eventRequest)

    @GetMapping
    @GetEventsApi
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun getEvents(
        @Parameter(description = "행사 승인 여부", example = "true")
        isApproved: Boolean? = true,
        @Parameter(description = "종료된 행사 포함 여부", example = "false")
        includeFinished: Boolean? = true,
        @Parameter(description = "북마크된 행사 출력 여부", example = "false")
        isBookmarked: Boolean? = false,
        @Valid @ParameterObject
        param: EventPaginationParam
    ) = eventService.getEvents(param, isApproved, isBookmarked, includeFinished)

    @GetMapping("calendar")
    @GetEventsForCalendarApi
    @AuthApiResponses
    @CommonApiResponses
    @ResponseStatus(HttpStatus.OK)
    fun getEvents4Calendar(
        @Valid @ParameterObject
        param: Event4CalendarRequest
    ): List<EventResponse> = eventService.getEvents4Calendar(param)

    @PatchMapping("{eventId}/approve")
    @ApproveEventApi
    @CommonApiResponses
    @AuthApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveEvent(@PathVariable eventId: Long) = eventService.approveEvent(eventId)

    @DeleteMapping("{eventId}")
    @Operation(summary = "행사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable eventId: Long) = eventService.deleteEvent(eventId)
}