package duit.server.domain.host.dto

data class HostUpdateRequest(
    val name: String,
    val thumbnail: String? = null
)
