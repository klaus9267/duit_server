package duit.server.domain.event.controller

import duit.server.domain.common.docs.AuthApiResponses
import duit.server.domain.common.docs.CommonApiResponses
import duit.server.domain.event.controller.docs.CreateEventApi
import duit.server.domain.event.controller.docs.CreateRandomEventApi
import duit.server.domain.event.controller.docs.GetEventsApi
import duit.server.domain.event.controller.docs.GetEventsForCalendarApi
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

    @PostMapping("random")
    @CreateRandomEventApi
    @CommonApiResponses
    @ResponseStatus(HttpStatus.CREATED)
    fun createRandomEvent() {
        // 랜덤 더미 데이터 생성
        val dummyTitles = listOf(
            "응급실 간호실무 향상 워크숍",
            "중환자실 최신 가이드라인 세미나",
            "소아과 간호 케어 교육",
            "수술실 감염관리 실습",
            "노인간호 전문과정",
            "정신간호 상담기법 워크숍",
            "간호연구 방법론 강의",
            "환자안전 품질관리 컨퍼런스",
            "호스피스 완화의료 교육",
            "모성간호 실무과정"
        )

        val dummyHosts = listOf(
            "서울대학교병원", "삼성서울병원", "서울아산병원",
            "세브란스병원", "대한간호협회", "한국간호교육학회",
            "대한중환자간호학회", "분당서울대학교병원"
        )

        val eventTypes = EventType.entries
        val randomTitle = dummyTitles.random()
        val randomHost = dummyHosts.random()
        val randomEventType = eventTypes.random()
        val today = LocalDate.now()

        // EventRequest 생성 (필수값만 사용)
        val eventRequest = EventRequest(
            title = randomTitle,
            startAt = today.plusDays((1..60).random().toLong()),
            endAt = null,
            recruitmentStartAt = null,
            recruitmentEndAt = null,
            uri = "https://example.com/events/${randomTitle.hashCode().toString().takeLast(6)}",
            eventThumbnail = null,
            eventType = randomEventType,
            hostName = randomHost,
            hostThumbnail = null
        )

        eventService.createEvent(eventRequest)
    }

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

    @DeleteMapping("{eventId}")
    @Operation(summary = "행사 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteEvent(@PathVariable eventId: Long) = eventService.deleteEvent(eventId)
}