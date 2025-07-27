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
            .signWith(key, Jwts.SIG.HS512)
            .compact()
    }
    
    /**
     * JWT 토큰에서 userId 추출
     */
    fun getUserId(token: String): Long {
        return getClaims(token)
            .get("userId", Integer::class.java)
            .toLong()
    }
    
    /**
     * JWT 토큰에서 Authentication 객체 생성
     */
    fun getAuthentication(token: String): Authentication {
        val userId = getUserId(token)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        
        return UsernamePasswordAuthenticationToken(userId, null, authorities)
    }
    
    /**
     * JWT 토큰 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * JWT 토큰에서 Claims 추출
     */
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
