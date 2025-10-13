package duit.server.infrastructure.external.webhook

import duit.server.domain.event.dto.EventRequest
import duit.server.domain.event.service.EventService
import duit.server.domain.host.service.HostService
import duit.server.infrastructure.external.webhook.dto.GoogleFormResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoogleFormProcessor(
    private val hostService: HostService,
    private val eventService: EventService
) {
    private val logger = LoggerFactory.getLogger(GoogleFormProcessor::class.java)

    @Transactional
    fun handleGoogleFormResult(result: GoogleFormResult) {
        val formData = result.formData
        val fileData = result.fileData

        val eventThumbnail = fileData?.get("행사 썸네일")?.firstOrNull()
        val hostThumbnail = fileData?.get("주최 기관 로고")?.firstOrNull()

        val eventRequest = EventRequest.from(formData, eventThumbnail, hostThumbnail)
        eventService.createEvent(eventRequest)
    }
}