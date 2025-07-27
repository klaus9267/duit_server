package duit.server.application.config

import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 개발/테스트용 초기 데이터 생성
 */
@Component
class DataInitializer(
    private val userRepository: UserRepository
) : ApplicationRunner {
    
    override fun run(args: ApplicationArguments?) {
        // 테스트용 사용자들 생성
        if (userRepository.count() == 0L) {
            createTestUsers()
        }
    }
    
    private fun createTestUsers() {
        val testUsers = listOf(
            User(
                email = "test1@example.com",
                nickname = "테스트유저1",
                providerType = ProviderType.KAKAO,
                providerId = "google_test_1"
            ),
            User(
                email = "test2@example.com", 
                nickname = "테스트유저2",
                providerType = ProviderType.KAKAO,
                providerId = "kakao_test_2"
            ),
            User(
                email = "admin@example.com",
                nickname = "관리자",
                providerType = ProviderType.KAKAO,
                providerId = "google_admin"
            )
        )
        
        userRepository.saveAll(testUsers)
        println("✅ 테스트용 사용자 데이터 생성 완료!")
        println("   - ID 1: 테스트유저1 (test1@example.com)")
        println("   - ID 2: 테스트유저2 (test2@example.com)")  
        println("   - ID 3: 관리자 (admin@example.com)")
        println("   JWT 토큰 발급: POST /api/v1/auth/token?userId=1")
    }
}
