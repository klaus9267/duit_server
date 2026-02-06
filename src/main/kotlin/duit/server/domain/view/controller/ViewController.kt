package duit.server.domain.view.controller

import duit.server.domain.view.service.ViewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/views")
@Tag(name = "View", description = "조회수 관련 API")
class ViewController(
    private val viewService: ViewService
) {

    @PatchMapping("{eventId}")
    @Operation(summary = "조회수 증가", description = "행사 조회수를 증가시킵니다")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun increaseCount(
        @PathVariable eventId: Long
    ) = viewService.increaseCount(eventId)
}