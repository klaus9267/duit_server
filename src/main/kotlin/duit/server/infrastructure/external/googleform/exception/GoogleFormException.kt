package duit.server.infrastructure.external.googleform.exception

import duit.server.domain.common.exception.DomainException
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class PastDateException(
    val date: LocalDate
) : DomainException("입력된 시간은 미래여야 합니다. 입력값: $date")

class PastTimeException(
    val date: LocalDateTime
) : DomainException("입력된 시간은 미래여야 합니다. 입력값: $date")

class InvalidURI(
    val uri: URI
) : DomainException("잘못된 URI입니다. 입력값: $uri")
