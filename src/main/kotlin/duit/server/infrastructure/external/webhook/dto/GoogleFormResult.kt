package duit.server.infrastructure.external.webhook.dto

import com.fasterxml.jackson.annotation.JsonProperty
import duit.server.infrastructure.external.webhook.dto.FileInfo

data class GoogleFormResult(
    @JsonProperty("timestamp")
    val timestamp: String,

    @JsonProperty("formData")
    val formData: Map<String, String>,

    @JsonProperty("fileData")
    val fileData: Map<String, List<FileInfo>>? = null,

    @JsonProperty("respondentEmail")
    val respondentEmail: String?
)