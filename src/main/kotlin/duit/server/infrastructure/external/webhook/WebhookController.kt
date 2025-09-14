package duit.server.infrastructure.external.webhook

import duit.server.infrastructure.external.webhook.dto.GoogleFormResult
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhook", description = "외부 서비스 Webhook API")
class WebhookController(
    private val googleFormProcessor: GoogleFormProcessor
) {

    @PostMapping("/google/form")
    @Operation(summary = "Google Forms 응답 Webhook", description = "Google Forms 응답 시 호출되는 Webhook 엔드포인트")
    @Hidden
    fun handleGoogleFormResult(@RequestBody result: GoogleFormResult) = googleFormProcessor.handleGoogleFormResult(result)
}