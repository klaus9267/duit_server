package duit.server.domain.host.dto

import duit.server.domain.host.entity.Host

data class HostResponse(
    val id: Long,
    val name: String,
    val thumbnail: String? = null,
){
    companion object{
        fun from(host: Host) = HostResponse(
            id = host.id!!,
            name = host.name,
            thumbnail = host.thumbnail
        )
    }
}