package duit.server.domain.host.repository

import duit.server.domain.host.entity.Host
import org.springframework.data.jpa.repository.JpaRepository

interface HostRepository : JpaRepository<Host, Long> {
}