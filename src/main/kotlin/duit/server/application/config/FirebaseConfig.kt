package duit.server.application.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream


@Configuration
class FirebaseConfig {
    @Bean
    fun firebaseApp(): FirebaseApp {
        val serviceAccount = FileInputStream("src/main/resources/firebase-key.json")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance(firebaseApp())
    }
}