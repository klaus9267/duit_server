package duit.server.domain.subscription.controller

import duit.server.application.common.RequireAuth
import duit.server.domain.subscription.dto.SubscriptionCreateRequest
import duit.server.domain.subscription.dto.SubscriptionResponse
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.service.SubscriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/subscriptions")
@Tag(name = "Subscription", description = "구독 관련 API")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
) {

    @PostMapping
    @Operation(
        summary = "구독 생성",
        description = "type 별로 body 의 필수 필드가 다름 (SubscriptionCreateRequest 스키마 참조). 중복 구독은 400.",
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createSubscription(
        @Valid @RequestBody request: SubscriptionCreateRequest,
    ): SubscriptionResponse = subscriptionService.createSubscription(request)

    @GetMapping
    @Operation(
        summary = "구독 목록 조회",
        description = "type 미지정 시 본인 전체 구독 반환. 지정 시 해당 type 만 필터.",
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getSubscriptions(
        @RequestParam(required = false) type: SubscriptionType?,
    ): List<SubscriptionResponse> = subscriptionService.getSubscriptions(type)

    @DeleteMapping("{subscriptionId}")
    @Operation(summary = "구독 삭제", description = "본인 구독만 삭제 가능")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSubscription(@PathVariable subscriptionId: Long) =
        subscriptionService.deleteSubscription(subscriptionId)
}
