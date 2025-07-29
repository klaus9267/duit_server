package duit.server.domain.event.service

import duit.server.application.controller.dto.event.EventRequest
import duit.server.domain.event.repository.EventRepository
import org.springframework.stereotype.Service

@Service
class EventService(private val eventRepository: EventRepository) {

    fun createEvent(eventRequest: EventRequest) = eventRepository.save(eventRequest.toEntity())
}