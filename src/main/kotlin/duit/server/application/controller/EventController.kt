package duit.server.application.controller

import duit.server.domain.event.service.EventService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/events")
@Tag(name = "Event", description = "행사 관련 API")
class EventController(private val eventService: EventService) {

    fun getUnapprovedEvents() {

    }
}