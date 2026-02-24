package duit.server.domain.auth.dto

data class FirebaseTokenClaims(
    val uid: String,
    val email: String?,
    val name: String?,
    val claims: Map<String, Any>
)
