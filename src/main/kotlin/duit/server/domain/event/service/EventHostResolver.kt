package duit.server.domain.event.service

import duit.server.domain.event.dto.EventHostRequest
import duit.server.domain.event.dto.EventHostSource
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.infrastructure.external.file.FileStorageService
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * 행사 생성/수정 요청의 주최 입력을 순서가 유지된 [Host] 목록으로 변환한다.
 *
 * - 신규: hosts + hostThumbnails (thumbnailIndex 로 로고 매핑)
 * - 레거시: hostId 또는 hostName + hostThumbnail (단일 주최)
 *
 * 두 방식을 섞으면 400. 로고는 hostName 으로 주최가 새로 생성될 때만 업로드한다.
 */
@Component
class EventHostResolver(
    private val hostService: HostService,
    private val fileStorageService: FileStorageService,
) {

    /**
     * @param legacyThumbnailFallback 레거시 hostName 경로에서 새 주최가 생성되는데 로고 파일이 없을 때 쓸 URL (행사 수정 전용)
     */
    fun resolve(
        source: EventHostSource,
        hostThumbnail: MultipartFile?,
        hostThumbnails: List<MultipartFile>,
        legacyThumbnailFallback: String? = null,
    ): List<Host> =
        source.hosts
            ?.let { resolveHosts(source, it, hostThumbnail, hostThumbnails) }
            ?: listOf(resolveLegacyHost(source, hostThumbnail, hostThumbnails, legacyThumbnailFallback))

    private fun resolveHosts(
        source: EventHostSource,
        requests: List<EventHostRequest>,
        hostThumbnail: MultipartFile?,
        hostThumbnails: List<MultipartFile>,
    ): List<Host> {
        require(source.hostId == null && source.hostName == null) {
            "hosts 와 hostId/hostName 은 함께 사용할 수 없습니다"
        }
        require(hostThumbnail == null) {
            "hosts 사용 시 hostThumbnail 대신 hostThumbnails 와 thumbnailIndex 를 사용하세요"
        }
        require(requests.isNotEmpty()) { "주최는 최소 1개 이상이어야 합니다" }
        requests.forEach { it.validate(hostThumbnails.size) }

        val thumbnailIndexes = requests.mapNotNull { it.thumbnailIndex }
        require(thumbnailIndexes.size == thumbnailIndexes.toSet().size) {
            "같은 thumbnailIndex 를 여러 주최에 사용할 수 없습니다"
        }

        return requests.map { request ->
            request.hostId?.let(hostService::getHost)
                ?: hostService.findOrCreateHost(request.hostName!!.trim()) {
                    request.thumbnailIndex?.let { upload(hostThumbnails[it]) }
                }
        }
    }

    private fun resolveLegacyHost(
        source: EventHostSource,
        hostThumbnail: MultipartFile?,
        hostThumbnails: List<MultipartFile>,
        thumbnailFallback: String?,
    ): Host {
        require(hostThumbnails.isEmpty()) { "hostThumbnails 는 hosts 와 함께 사용해야 합니다" }

        return source.hostId?.let(hostService::getHost)
            ?: source.hostName?.let { name ->
                hostService.findOrCreateHost(name) { hostThumbnail?.let(::upload) ?: thumbnailFallback }
            }
            ?: throw IllegalArgumentException("hosts 또는 hostId/hostName 중 하나는 필수입니다")
    }

    private fun upload(file: MultipartFile): String = fileStorageService.uploadFile(file, "hosts")
}
