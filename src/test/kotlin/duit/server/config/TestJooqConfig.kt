package duit.server.config

import duit.server.application.scheduler.EventAlarmScheduler
import duit.server.domain.event.repository.EventRepositoryCustom
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestJooqConfig {

    @Bean
    @Primary
    fun eventRepositoryCustom(): EventRepositoryCustom {
        return Mockito.mock(EventRepositoryCustom::class.java)
    }

    @Bean
    @Primary
    fun eventAlarmScheduler(): EventAlarmScheduler {
        return Mockito.mock(EventAlarmScheduler::class.java)
    }
}
