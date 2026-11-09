package duit.server.domain.user.service

import duit.server.domain.user.repository.UserDeviceTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserDeviceTokenService(
    private val userDeviceTokenRepository: UserDeviceTokenRepository,
) {
    fun getTokens(userId: Long): List<String> = userDeviceTokenRepository.findTokensByUserId(userId)

    @Transactional
    fun deleteTokens(tokens: Collection<String>): Int =
        tokens.takeIf { it.isNotEmpty() }?.let(userDeviceTokenRepository::deleteAllByTokenIn) ?: 0
}
