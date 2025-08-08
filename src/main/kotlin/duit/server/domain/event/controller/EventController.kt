package duit.server.domain.event.controller

import duit.server.domain.event.dto.Event4CalendarRequest
import duit.server.domain.event.dto.EventPaginationParam
import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.service.EventService
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.service.HostService
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
    private val eventService: EventService,
    private val hostService: HostService
) {

    @PostMapping
    @Operation(summary = "개발용 더미 이벤트 생성 (랜덤 데이터)")
    @ResponseStatus(HttpStatus.CREATED)
    fun createEvent() {
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

        // Host 찾거나 생성
        val host = hostService.findOrCreateHost(
            HostRequest(name = randomHost, thumbnail = null)
        )

        // EventRequest 생성 (필수값만 사용)
        val eventRequest = EventRequest(
            title = randomTitle,
            startAt = today.plusDays((1..60).random().toLong()),
            endAt = null,
            recruitmentStartAt = null,
            recruitmentEndAt = null,
            uri = "https://example.com/events/${randomTitle.hashCode().toString().takeLast(6)}",
            thumbnail = null,
            eventType = randomEventType,
            host = host
        )

        // 이벤트 생성 (View도 자동 생성됨)
        val createdEvent = eventService.createEvent(eventRequest)
    }

    @GetMapping
    @Operation(summary = "행사 목록 조회")
    @ResponseStatus(HttpStatus.OK)
    fun getEvents(
        @Parameter(description = "행사 승인 여부", example = "true")
        isApproved: Boolean? = true,
        @Valid @ParameterObject
        param: EventPaginationParam
    ) = eventService.getEvents(param, isApproved)

    @GetMapping("calendar")
    @Operation(summary = "현재 로그인한 사용자가 북마크한 년,월별 행사 목록 조회")
    @ResponseStatus(HttpStatus.OK)
    fun getEvents4Calendar(
        @Valid @ParameterObject
        param: Event4CalendarRequest
    ) = eventService.getEvents4Calendar(param)
}