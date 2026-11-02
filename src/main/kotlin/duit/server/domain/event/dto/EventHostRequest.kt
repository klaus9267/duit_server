package duit.server.domain.event.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "행사 주최 항목. hostId 와 hostName 중 하나만 입력")
data class EventHostRequest(
    @field:Schema(description = "기존 주최 기관 ID", example = "1")
    val hostId: Long? = null,

    @field:Schema(description = "주최 기관명. 같은 이름의 기관이 있으면 재사용하고, 없으면 새로 생성", example = "단국대학교 IT 대학")
    val hostName: String? = null,

    @field:Schema(
        description = "hostThumbnails 파트 중 이 주최 로고의 인덱스 (0부터). hostName 으로 새 기관이 생성될 때만 업로드됨",
        example = "0"
    )
    val thumbnailIndex: Int? = null,
) {
    fun validate(thumbnailCount: Int) {
        require((hostId != null) != !hostName.isNullOrBlank()) {
            "주최 항목마다 hostId 와 hostName 중 하나만 입력해야 합니다"
        }
        thumbnailIndex?.let { index ->
            require(hostId == null) { "hostId 로 지정한 기존 주최에는 thumbnailIndex 를 사용할 수 없습니다" }
            require(index in 0 until thumbnailCount) {
                "thumbnailIndex($index) 가 hostThumbnails 범위(0..${thumbnailCount - 1})를 벗어났습니다"
            }
        }
    }
}

/**
 * 행사 생성/수정 요청의 주최 입력. [hosts] 가 신규 방식이고 [hostId]/[hostName] 은 구 클라이언트용 단일 주최 방식.
 */
interface EventHostSource {
    val hosts: List<EventHostRequest>?
    val hostId: Long?
    val hostName: String?
}

const val MAX_EVENT_HOSTS = 20
