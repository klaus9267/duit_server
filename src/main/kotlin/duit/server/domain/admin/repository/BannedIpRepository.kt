package duit.server.domain.admin.repository

import duit.server.domain.admin.entity.BannedIp
import org.springframework.data.jpa.repository.JpaRepository

interface BannedIpRepository : JpaRepository<BannedIp, Long> {
    fun findByIpAddress(ipAddress: String): BannedIp?
}
