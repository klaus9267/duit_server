package duit.server.support

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.SendResponse
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestFirebaseConfig {

    @Bean
    @Primary
    fun firebaseApp(): FirebaseApp = Mockito.mock(FirebaseApp::class.java)

    @Bean
    @Primary
    fun firebaseAuth(): FirebaseAuth = Mockito.mock(FirebaseAuth::class.java)

    // 실제로 보내지 않고 모두 성공한 것으로 응답한다
    @Bean
    @Primary
    fun firebaseMessaging(): FirebaseMessaging = Mockito.mock(FirebaseMessaging::class.java).also { messaging ->
        Mockito.`when`(messaging.sendEach(Mockito.anyList())).thenAnswer { invocation ->
            val messageCount = invocation.getArgument<List<*>>(0).size
            object : BatchResponse {
                override fun getResponses(): List<SendResponse> = emptyList()
                override fun getSuccessCount(): Int = messageCount
                override fun getFailureCount(): Int = 0
            }
        }
    }
}
