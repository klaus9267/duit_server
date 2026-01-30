package duit.server.domain.common.extensions

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.CrudRepository

inline fun <reified T, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T =
    findById(id).orElseThrow {
        EntityNotFoundException("${T::class.simpleName}을(를) 찾을 수 없습니다: $id")
    }
