package duit.server.domain.alarm.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class AlarmPaginationParam(
    @field:Schema(description = "페이지 번호", example = "0", required = false)
    val page: Int = 0,

    @field:Schema(description = "페이지 크기", example = "10", required = false)
    val size: Int = 10
) {
    fun toPageable(): Pageable = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "createdAt")
    )
}
