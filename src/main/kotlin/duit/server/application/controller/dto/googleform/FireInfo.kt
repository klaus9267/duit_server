package duit.server.application.controller.dto.googleform

data class FileInfo(
    val name: String,
    val mimeType: String,
    val size: Long,
    val data: String? = null, // Base64 인코딩된 파일 데이터
    val id: String? = null,   // Google Drive ID (기존 호환성)
    val downloadUrl: String? = null,
    val directDownloadUrl: String? = null
)
