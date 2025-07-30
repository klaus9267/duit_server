package duit.server.application.controller.dto.host

import duit.server.application.controller.dto.googleform.FileInfo
import duit.server.domain.host.entity.Host

data class HostRequest(
    val name: String,
    val thumbnail: String? = null
) {
    fun toEntity() = Host(
        name = name,
        thumbnail = thumbnail
    )

    companion object {
        fun from(formData: Map<String, String>, fileInfo: FileInfo?) = HostRequest(
            name = formData["주최 기관명"]!!,
            thumbnail = fileInfo?.directDownloadUrl
        )
    }
}
