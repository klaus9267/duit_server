package duit.server.application.exception

/**
 * 애플리케이션 계층 예외 - 순수함 (HTTP 의존성 없음)
 */
abstract class ApplicationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 외부 서비스 연동 예외
 */
class ExternalServiceException(
    val serviceName: String,
    message: String,
    cause: Throwable? = null
) : ApplicationException("External service '$serviceName' error: $message", cause)

/**
 * 데이터 접근 예외
 */
class DataAccessException(
    val operation: String,
    message: String,
    cause: Throwable? = null
) : ApplicationException("Data access error during $operation: $message", cause)

/**
 * 파일 처리 예외
 */
class FileProcessingException(
    val fileName: String,
    val operation: String,
    cause: Throwable? = null
) : ApplicationException("File processing error for '$fileName' during $operation", cause)
