package duit.server.domain.event.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.event.dto.*
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
@RequestMapping("api/v2/events")
@Tag(name = "Event V2", description = "행사 관련 API V2")
class EventControllerV2 (
    private val eventService: EventService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "행사 생성", description = "행사를 생성합니다 (관리자 승인 필요)")
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
    @RequireAuth
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
    @Operation(summary = "행사 목록 조회", description = "행사 목록을 페이지네이션으로 조회합니다")
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
    ) = eventService.getEventsV2(param, isApproved, isBookmarked, includeFinished)


    @GetMapping("calendar")
    @Operation(summary = "북마크한 행사 달력 조회", description = "북마크한 행사들을 월별 달력 형태로 조회합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getEvents4Calendar(
        @Valid @ParameterObject
        param: Event4CalendarRequest
    ): List<EventResponse> = eventService.getEvents4Calendar(param)

    @PatchMapping("{eventId}/approve")
    @Operation(summary = "행사 승인", description = "행사를 승인합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun approveEvent(@PathVariable eventId: Long) = eventService.approveEvent(eventId)

    @DeleteMapping("{eventId}")
    @Operation(summary = "행사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireAuth
    fun deleteEvent(@PathVariable eventId: Long) = eventService.deleteEvent(eventId)

    @PutMapping("{eventId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "행사 수정 (관리자)", description = "관리자가 행사를 수정합니다")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun updateEvent(
        @PathVariable eventId: Long,
        @Valid @RequestPart("data") updateRequest: EventUpdateRequest,
        @RequestPart("eventThumbnail", required = false)
        @Parameter(description = "행사 썸네일 이미지")
        eventThumbnail: MultipartFile?,
        @RequestPart("hostThumbnail", required = false)
        @Parameter(description = "주최 기관 로고 이미지")
        hostThumbnail: MultipartFile?
    ): EventResponse = eventService.updateEvent(eventId, updateRequest, eventThumbnail, hostThumbnail)

    @DeleteMapping("batch")
    @Operation(summary = "행사 목록 삭제(관리자)")
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun deleteEvents(
        @RequestParam eventIds: List<Long>
    ) = eventService.deleteEvents(eventIds)
}