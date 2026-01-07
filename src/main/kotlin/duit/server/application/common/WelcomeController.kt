package duit.server.application.common

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Welcome")
class WelcomeController {
    @GetMapping
    @Operation()
    @ResponseStatus(HttpStatus.OK)
    fun welcome(): String = "welcome!! :)"
}