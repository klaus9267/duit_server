package duit.server.domain.user.exception

import duit.server.domain.common.exception.DomainException

/**
 * 사용자 관련 순수 도메인 예외들 (HTTP 의존성 없음)
 */
class UserNotFoundException(val userId: Long) : 
    DomainException("User with id $userId not found")

class UserLoginIdNotFoundException(val loginId: String) : 
    DomainException("User with loginId $loginId not found")

class UserEmailNotFoundException(val email: String) : 
    DomainException("User with email $email not found")

class DuplicateEmailException(val email: String) : 
    DomainException("User with email '$email' already exists")

class DuplicateLoginIdException(val loginId: String) : 
    DomainException("User with loginId '$loginId' already exists")

class DuplicateNicknameException(val nickname: String) : 
    DomainException("User with nickname '$nickname' already exists")

class InvalidPasswordException(message: String = "Invalid password") : 
    DomainException(message)

class UnauthorizedException(message: String = "인증되지 않은 사용자입니다") : 
    DomainException(message)

class InvalidTokenException(message: String = "유효하지 않은 토큰입니다") : 
    DomainException(message)

class ExpiredTokenException(message: String = "만료된 토큰입니다") : 
    DomainException(message)
