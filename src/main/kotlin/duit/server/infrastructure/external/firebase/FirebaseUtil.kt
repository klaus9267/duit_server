package duit.server.infrastructure.external.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class FirebaseUtil(
    private val firebaseAuth: FirebaseAuth,
    @Value("\${firebase.web-api-key}")
    private val firebaseWebApiKey: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    /**
     * Email로 Firebase ID Token 생성
     * @param uid 사용자 이메일
     * @return Firebase ID Token
     */
    fun createIdTokenByUid(uid: String): String {
        try {
            // 1. Firebase에서 이메일로 사용자 조회
            val userRecord = firebaseAuth.getUser(uid)
            logger.info("Found Firebase user: ${userRecord.uid}")

            // 2. Custom Token 생성
            val customToken = firebaseAuth.createCustomToken(userRecord.uid)
            logger.info("Created custom token for user: ${userRecord.uid}")

            // 3. Custom Token을 ID Token으로 변환
            val idToken = exchangeCustomTokenForIdToken(customToken)
            logger.info("Successfully created ID token for user: ${userRecord.uid}")

            return idToken
        } catch (e: Exception) {
            logger.error("Failed to create ID token for email: $uid", e)
            throw IllegalStateException("Firebase ID 토큰 생성 실패: ${e.message}", e)
        }
    }

    /**
     * 회원 탈퇴 시 Firebase Auth 계정 삭제.
     * 탈퇴를 막지 않도록 실패는 로그만 남기고, Firebase 가 초기화되지 않은 환경(테스트)에서는 건너뛴다.
     */
    fun deleteUserQuietly(uid: String) {
        if (FirebaseApp.getApps().isEmpty()) return

        runCatching { firebaseAuth.deleteUser(uid) }
            .onFailure { e ->
                if ((e as? FirebaseAuthException)?.authErrorCode == AuthErrorCode.USER_NOT_FOUND) return@onFailure
                logger.warn("Firebase 사용자 삭제 실패 (탈퇴는 계속 진행): uid={}***", uid.take(4), e)
            }
    }

    /**
     * Custom Token을 ID Token으로 변환
     * Firebase Auth REST API 사용
     */
    private fun exchangeCustomTokenForIdToken(customToken: String): String {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=$firebaseWebApiKey"

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestBody = mapOf(
            "token" to customToken,
            "returnSecureToken" to true
        )

        val request = HttpEntity(requestBody, headers)

        try {
            val response = restTemplate.postForEntity(url, request, Map::class.java)
            val responseBody = response.body as? Map<*, *>
                ?: throw IllegalStateException("Invalid response from Firebase")

            return responseBody["idToken"] as? String
                ?: throw IllegalStateException("ID token not found in response")
        } catch (e: Exception) {
            logger.error("Failed to exchange custom token for ID token", e)
            throw IllegalStateException("Custom Token 변환 실패: ${e.message}", e)
        }
    }
}