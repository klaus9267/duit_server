package duit.server.application.controller.dto.host

import duit.server.domain.host.entity.Host

data class HostRequest(
    val name: String,
    val thumbnail: String? = null
) {
    fun toEntity() = Host(
        name = name,
        thumbnail = thumbnail
    )
}
