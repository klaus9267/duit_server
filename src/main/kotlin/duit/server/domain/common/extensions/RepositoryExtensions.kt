package duit.server.domain.common.extensions

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.CrudRepository

fun <T> CrudRepository<T, Long>.findByIdOrThrow(
    id: Long,
    entityName: String
): T = findById(id).orElseThrow {
    EntityNotFoundException("${entityName}을(를) 찾을 수 없습니다: $id")
}
