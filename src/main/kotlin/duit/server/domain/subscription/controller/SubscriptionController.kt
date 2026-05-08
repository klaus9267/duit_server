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
@Tag(
    name = "Subscription",
    description = """
        구독 관련 API.

        사용자가 관심 있는 행사/채용공고에 대한 알림을 받기 위해 5종 구독을 관리한다.
        구독이 매칭되면 자동으로 [Alarm] 이 생성되고 FCM 푸시가 발송된다 (등록 디바이스 토큰 기준).

        **5종 구독 타입:**
        - `EVENT_KEYWORD` — 행사 제목 부분일치
        - `EVENT_HOST`    — 특정 주최 기관
        - `EVENT_TYPE`    — 특정 행사 유형 (CONFERENCE/SEMINAR/...)
        - `JOB_KEYWORD`   — 채용공고 제목 부분일치
        - `JOB_COMPANY`   — 특정 회사

        응답은 type 별 자식 클래스로 폴리모픽 (sealed). 클라이언트는 `type` 필드로 분기.

        조회 결과는 페이지네이션 없이 List 반환 — 사용자당 구독 수가 보통 수십 건 이내.
    """,
)
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
            | `EVENT_KEYWORD` | `keyword` (1~50자) | 행사 제목에 키워드 부분일치 (대소문자 무시) |
            | `EVENT_HOST`    | `hostId` | 해당 주최가 새 행사 등록/승인 시 |
            | `EVENT_TYPE`    | `eventType` | 해당 유형의 새 행사 등록/승인 시 |
            | `JOB_KEYWORD`   | `keyword` (1~50자) | 채용공고 제목에 키워드 부분일치 (대소문자 무시) |
            | `JOB_COMPANY`   | `companyId` | 해당 회사의 새 채용공고 수집 시 |

            type 이 요구하지 않는 다른 대상 필드를 동시에 채우면 400. 빈 keyword/누락된 ID 도 400.

            **응답 (201 Created):** 생성된 구독 — 폴리모픽 [SubscriptionResponse] 자식 (oneOf 5종).
            클라이언트는 응답의 `type` 필드로 어떤 자식인지 분기해 해당 필드 접근.

            **예외:**
            - 400 INVALID_REQUEST: type 별 필수 필드 누락 / 잘못된 조합 / 동일 (사용자, type, 대상) 중복 구독
            - 404 NOT_FOUND: `hostId` / `companyId` 가 존재하지 않는 경우
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

            **쿼리:**
            - `type` (선택): 지정 시 해당 [SubscriptionType] 만 필터. 미지정 시 본인 전체 구독.

            **응답 (200 OK):** 배열. 각 원소는 [SubscriptionResponse] sealed interface 의 자식 (oneOf):
            - `EventKeywordSubscriptionResponse`  — `{ id, type, keyword, createdAt }`
            - `EventHostSubscriptionResponse`     — `{ id, type, host: { id, name }, createdAt }`
            - `EventTypeSubscriptionResponse`     — `{ id, type, eventType, createdAt }`
            - `JobKeywordSubscriptionResponse`    — `{ id, type, keyword, createdAt }`
            - `JobCompanySubscriptionResponse`    — `{ id, type, company: { id, name }, createdAt }`

            클라이언트는 원소의 `type` 필드로 분기해서 자기 type 의 필드를 읽으면 됨.
            페이지네이션은 없음 — 사용자당 구독은 보통 수십 건 이내.

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
            - `subscriptionId`: 삭제 대상 구독 ID

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
