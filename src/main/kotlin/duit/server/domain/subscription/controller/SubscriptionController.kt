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
        description = """
현재 인증 사용자의 구독을 1건 생성한다.

**type 별 body 필수 필드:**

| type | 필수 필드 | 알림 트리거 |
|---|---|---|
| EVENT_KEYWORD | keyword (1~50자) | 행사 제목에 키워드 부분일치 (대소문자 무시) |
| EVENT_HOST    | hostId | 해당 주최가 새 행사 등록/승인 시 |
| EVENT_TYPE    | eventType | 해당 유형의 새 행사 등록/승인 시 |
| JOB_KEYWORD   | keyword (1~50자) | 채용공고 제목에 키워드 부분일치 (대소문자 무시) |
| JOB_COMPANY   | companyId | 해당 회사의 새 채용공고 수집 시 |

type 이 사용하지 않는 다른 필드는 무시된다 — 클라이언트가 같은 Form 재사용 가능. 자기 type 의 필수 필드(빈 keyword / null hostId 등) 누락만 400.

**응답 (201 Created):** 생성된 구독 — 폴리모픽 [SubscriptionResponse] 자식 (oneOf 5종).
클라이언트는 응답의 type 필드로 어떤 자식인지 분기해 해당 필드 접근.

**예외:**
- 400 INVALID_REQUEST: 자기 type 의 필수 필드 누락 / 동일 (사용자, type, 대상) 중복 구독
- 404 NOT_FOUND: hostId / companyId 가 존재하지 않는 경우
- 401 UNAUTHORIZED: 인증 없음
        """,
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    fun createSubscription(
        @Valid @RequestBody request: SubscriptionCreateRequest,
    ): SubscriptionResponse = subscriptionService.createSubscription(request)

    @GetMapping
    @Operation(
        summary = "구독 목록 조회",
        description = """
현재 인증 사용자의 구독을 ID 내림차순(최신순)으로 반환한다.

**응답 (200 OK):** 배열. 각 원소는 [SubscriptionResponse] sealed interface 의 자식 (oneOf):
- `EventKeywordSubscriptionResponse`  — { id, type, keyword, createdAt }
- `EventHostSubscriptionResponse`     — { id, type, host: { id, name }, createdAt }
- `EventTypeSubscriptionResponse`     — { id, type, eventType, createdAt }
- `JobKeywordSubscriptionResponse`    — { id, type, keyword, createdAt }
- `JobCompanySubscriptionResponse`    — { id, type, company: { id, name }, createdAt }

클라이언트는 원소의 type 필드로 분기해서 자기 type 의 필드를 읽으면 됨.
페이지네이션은 없음

**예외:**
- 401 UNAUTHORIZED: 인증 없음
        """,
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.OK)
    fun getSubscriptions(
        @RequestParam(required = false) type: SubscriptionType?,
    ): List<SubscriptionResponse> = subscriptionService.getSubscriptions(type)

    @DeleteMapping("{subscriptionId}")
    @Operation(
        summary = "구독 삭제",
        description = """
본인 소유 구독만 삭제 가능. 삭제된 구독은 더 이상 알림을 트리거하지 않음.
기존 [Alarm] 은 그대로 유지된다 (구독이 삭제됐다고 과거 알람도 사라지지는 않음).

**Path:**
- subscriptionId: 삭제 대상 구독 ID

**응답 (204 No Content):** 본문 없음.

**예외:**
- 400 INVALID_REQUEST: 본인 소유 아님 / 존재하지 않는 ID
  (다른 사용자 구독 존재 여부 노출을 막기 위해 둘 다 동일 메시지로 400 응답)
- 401 UNAUTHORIZED: 인증 없음
        """,
    )
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSubscription(@PathVariable subscriptionId: Long) =
        subscriptionService.deleteSubscription(subscriptionId)
}
