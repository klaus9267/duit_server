package duit.server.domain.common

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.event.entity.EventType
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import jakarta.persistence.Column
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `@Enumerated(STRING)` 컬럼의 선언 길이 ↔ enum 상수 이름 길이 정합성 검증.
 *
 * 테스트는 H2(create-drop)로 돌아 실제 MySQL 스키마를 타지 않기 때문에,
 * "enum 에 값을 추가했는데 DB 컬럼이 못 받는" 문제를 통합 테스트가 잡아주지 못한다.
 * (실제로 alarms.type 이 레거시 ENUM('EVENT_START','RECRUITMENT_START','RECRUITMENT_END') 인 채로
 *  구독 알람 값이 추가돼 프로덕션에서 MySQL 1265 Data truncated 가 발생했다 → V3 마이그레이션에서 VARCHAR 전환)
 *
 * VARCHAR 전환 이후 남는 위험은 "enum 이름이 컬럼 길이를 넘는 경우" 하나뿐이므로 그걸 여기서 막는다.
 */
@DisplayName("enum 컬럼 길이 정합성")
class EnumColumnLengthTest {

    @Test
    fun `AlarmType 이름이 모두 alarms_type 선언 길이 안에 들어간다`() =
        assertFitsInColumn(Alarm::class.java, "type", AlarmType.entries.map { it.name })

    @Test
    fun `SubscriptionType 이름이 모두 subscriptions_type 선언 길이 안에 들어간다`() =
        assertFitsInColumn(Subscription::class.java, "type", SubscriptionType.entries.map { it.name })

    @Test
    fun `EventType 이름이 모두 subscriptions_event_type 선언 길이 안에 들어간다`() =
        assertFitsInColumn(Subscription::class.java, "eventType", EventType.entries.map { it.name })

    private fun assertFitsInColumn(entity: Class<*>, fieldName: String, names: List<String>) {
        val declaredLength = entity.getDeclaredField(fieldName)
            .getAnnotation(Column::class.java)
            .length

        val tooLong = names.filter { it.length > declaredLength }

        assertTrue(tooLong.isEmpty()) {
            "${entity.simpleName}.$fieldName 선언 길이(${declaredLength})를 초과하는 값: " +
                tooLong.joinToString { "$it(${it.length}자)" } +
                ". 컬럼을 넓히는 Flyway 마이그레이션을 함께 추가하세요."
        }
    }
}
