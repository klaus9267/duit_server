package duit.server.domain.common.exception

/**
 * 도메인 계층의 최상위 예외 클래스 - 순수한 도메인 예외 (HTTP 의존성 없음)
 */
abstract class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 비즈니스 룰 위반 예외
 */
class BusinessRuleViolationException(message: String) : DomainException(message)

/**
 * 엔티티를 찾을 수 없는 예외
 */
class EntityNotFoundException(entityType: String, id: Any) : 
    DomainException("$entityType with id $id not found")

/**
 * 중복된 엔티티 예외
 */
class DuplicateEntityException(entityType: String, field: String, value: Any) : 
    DomainException("$entityType with $field '$value' already exists")

/**
 * 권한 부족 예외
 */
class InsufficientPermissionException(message: String = "Insufficient permission") : 
    DomainException(message)

/**
 * 잘못된 도메인 상태 예외
 */
class InvalidDomainStateException(message: String) : DomainException(message)

/**
 * 도메인 값 검증 예외
 */
class DomainValidationException(field: String, value: Any?, message: String) : 
    DomainException("Validation failed for field '$field' with value '$value': $message")
