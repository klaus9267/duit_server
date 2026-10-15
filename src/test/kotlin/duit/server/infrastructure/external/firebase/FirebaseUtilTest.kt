package duit.server.infrastructure.external.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FirebaseUtil 단위 테스트")
class FirebaseUtilTest {

    @Test
    fun `Firebase 가 초기화되지 않은 환경에서는 계정 삭제를 건너뛴다`() {
        assertTrue(FirebaseApp.getApps().isEmpty())
        val firebaseAuth = mockk<FirebaseAuth>()

        FirebaseUtil(firebaseAuth, "test-key").deleteUserQuietly("firebase-uid")

        verify(exactly = 0) { firebaseAuth.deleteUser(any()) }
    }
}
