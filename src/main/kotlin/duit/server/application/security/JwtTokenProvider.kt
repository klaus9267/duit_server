package duit.server.application.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    
    @Value("\${jwt.access-token-validity}")
    private val accessTokenValidityInMilliseconds: Long
) {
    
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
    }

    // JwtParser 는 불변·스레드 안전이라 요청마다 새로 만들지 않는다
    private val parser: JwtParser by lazy {
        Jwts.parser().verifyWith(key).build()
    }
    
    /**
     * JWT AccessToken 생성
     */
    fun createAccessToken(userId: Long): String {
        val now = Date()
        val validity = Date(now.time + accessTokenValidityInMilliseconds)
        
        return Jwts.builder()
            .subject(userId.toString())
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(validity)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }
    
    /**
     * 서명·만료를 검증하고 userId 를 주체로 하는 인증 객체를 만든다. 유효하지 않으면 null.
     * 검증과 userId 추출을 한 번의 파싱으로 한다 — 둘을 따로 하면 요청마다 서명 검증을 두 번 한다 (performance H-17)
     */
    fun getAuthenticationOrNull(token: String): Authentication? {
        val claims = try {
            getClaims(token)
        } catch (e: JwtException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        val userId = claims.get("userId", Integer::class.java).toLong()
        return UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    }

    /**
     * JWT 토큰에서 Claims 추출
     */
    private fun getClaims(token: String): Claims {
        return parser.parseSignedClaims(token).payload
    }
}
