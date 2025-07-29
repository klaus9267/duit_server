package duit.server.application.controller.dto.googleform

import com.fasterxml.jackson.annotation.JsonProperty

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
