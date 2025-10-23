package duit.server.domain.event.controller

import duit.server.domain.common.docs.AuthApiResponses
import duit.server.domain.event.controller.docs.ApproveEventApi
import duit.server.domain.event.controller.docs.CreateEventApi
import duit.server.domain.event.controller.docs.GetEventsApi
import duit.server.domain.event.controller.docs.GetEventsForCalendarApi
import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventCreateRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.event.service.EventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/v1/events")
@Tag(name = "Event", description = "행사 관련 API")
class EventController(
    private val eventService: EventService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @CreateEventApi
    @ResponseStatus(HttpStatus.CREATED)
    fun createEventByUser(
        @Valid @RequestPart("data") eventRequest: EventCreateRequest,
        @RequestPart("eventThumbnail", required = false)
        @Parameter(description = "행사 썸네일 이미지")
        eventThumbnail: MultipartFile?,
        @RequestPart("hostThumbnail", required = false)
        @Parameter(description = "주최 기관 로고 이미지")
        hostThumbnail: MultipartFile?
    ) = eventService.createEvent(eventRequest, eventThumbnail, hostThumbnail, isApproved = false)

    @PostMapping("/admin", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "관리자 행사 생성", description = "관리자가 행사를 생성합니다 (자동 승인)")
    @ResponseStatus(HttpStatus.CREATED)
    fun createEventByAdmin(
        @Valid @RequestPart("data") eventRequest: EventCreateRequest,
        @RequestPart("eventThumbnail", required = false)
        @Parameter(description = "행사 썸네일 이미지")
        eventThumbnail: MultipartFile?,
        @RequestPart("hostThumbnail", required = false)
        @Parameter(description = "주최 기관 로고 이미지")
        hostThumbnail: MultipartFile?
    ) = eventService.createEvent(eventRequest, eventThumbnail, hostThumbnail, isApproved = true)

    @GetMapping
    @GetEventsApi
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
    @ResponseStatus(HttpStatus.OK)
    fun getEvents4Calendar(
        @Valid @ParameterObject
        param: Event4CalendarRequest
    ): List<EventResponse> = eventService.getEvents4Calendar(param)

    @PatchMapping("{eventId}/approve")
    @ApproveEventApi
    @AuthApiResponses
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveEvent(@PathVariable eventId: Long) = eventService.approveEvent(eventId)

    @DeleteMapping("{eventId}")
    @Operation(summary = "행사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable eventId: Long) = eventService.deleteEvent(eventId)
}