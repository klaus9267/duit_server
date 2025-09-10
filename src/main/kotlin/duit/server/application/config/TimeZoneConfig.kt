package duit.server.application.config

import org.springframework.context.annotation.Configuration
import java.util.*
import javax.annotation.PostConstruct

@Configuration
class TimeZoneConfig {
    
    @PostConstruct
    fun init() {
        // 애플리케이션 전체 기본 시간대를 한국 시간으로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}