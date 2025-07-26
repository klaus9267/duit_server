package duit.server.domain.user.exception

import duit.server.domain.common.exception.DomainException

/**
 * 사용자 관련 순수 도메인 예외들 (HTTP 의존성 없음)
 */
class UserNotFoundException(val userId: Long) : 
    DomainException("User with id $userId not found")

class UserEmailNotFoundException(val email: String) : 
    DomainException("User with email $email not found")

class DuplicateEmailException(val email: String) : 
    DomainException("User with email '$email' already exists")

class InvalidPasswordException(message: String = "Invalid password") : 
    DomainException(message)

class UserAccountLockedException(val userId: Long) : 
    DomainException("User account $userId is locked")

class UserAccountExpiredException(val userId: Long) : 
    DomainException("User account $userId has expired")

class InvalidUserRoleException(val role: String) : 
    DomainException("Invalid user role: $role")
