package duit.server.support

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
}
