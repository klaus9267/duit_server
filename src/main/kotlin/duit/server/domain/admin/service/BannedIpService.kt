package duit.server.domain.admin.service

import duit.server.domain.admin.entity.BannedIp
import duit.server.domain.admin.repository.BannedIpRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class BannedIpService(
    private val bannedIpRepository: BannedIpRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleLoginFailure(ip: String) {
        bannedIpRepository.findByIpAddress(ip)
            ?.apply { recordFailure() }
            ?.also { bannedIpRepository.save(it) }
            ?: BannedIp(ipAddress = ip, failureCount = 1)
                .also { bannedIpRepository.save(it) }
    }
}
