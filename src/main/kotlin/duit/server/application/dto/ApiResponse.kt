package duit.server.application.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T? = null, message: String = "성공"): ApiResponse<T> {
            return ApiResponse(success = true, message = message, data = data)
        }
        
        fun <T> fail(message: String): ApiResponse<T> {
            return ApiResponse(success = false, message = message, data = null)
        }
    }
}
