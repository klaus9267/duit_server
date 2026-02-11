package duit.server.support

import com.fasterxml.jackson.databind.ObjectMapper
import duit.server.application.security.JwtTokenProvider
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var entityManager: EntityManager

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    protected fun authHeader(userId: Long): String =
        "Bearer ${jwtTokenProvider.createAccessToken(userId)}"
}
