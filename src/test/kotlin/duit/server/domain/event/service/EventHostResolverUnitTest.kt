package duit.server.domain.event.service

import duit.server.domain.event.dto.EventHostRequest
import duit.server.domain.event.dto.EventHostSource
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.infrastructure.external.file.FileStorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile

@DisplayName("EventHostResolver 단위 테스트")
class EventHostResolverUnitTest {

    private lateinit var hostService: HostService
    private lateinit var fileStorageService: FileStorageService
    private lateinit var resolver: EventHostResolver

    private val existingHost = Host(id = 1L, name = "기존주최")

    private data class Source(
        override val hosts: List<EventHostRequest>? = null,
        override val hostId: Long? = null,
        override val hostName: String? = null,
    ) : EventHostSource

    @BeforeEach
    fun setUp() {
        hostService = mockk()
        fileStorageService = mockk()
        resolver = EventHostResolver(hostService, fileStorageService)
        every { hostService.getHost(1L) } returns existingHost
    }

    /** 이름이 없으면 새로 생성되는 것처럼 동작 — 로고 람다를 실제로 호출한다 */
    private fun stubHostCreation(name: String, id: Long) {
        every { hostService.findOrCreateHost(name, any()) } answers {
            Host(id = id, name = name, thumbnail = secondArg<() -> String?>().invoke())
        }
    }

    private fun assertInvalid(expectedMessagePart: String, block: () -> Unit) {
        val exception = assertThrows<IllegalArgumentException> { block() }
        assertTrue(exception.message!!.contains(expectedMessagePart), "message: ${exception.message}")
    }

    @Nested
    @DisplayName("hosts (신규 방식)")
    inner class HostsTests {

        @Test
        @DisplayName("기존 주최와 신규 주최를 요청 순서대로 반환하고, thumbnailIndex 로 로고를 매핑한다")
        fun resolvesInOrderWithThumbnailMapping() {
            val logo0 = mockk<MultipartFile>()
            val logo1 = mockk<MultipartFile>()
            stubHostCreation("신규A", id = 10L)
            stubHostCreation("신규B", id = 11L)
            every { fileStorageService.uploadFile(logo1, "hosts") } returns "uploads/hosts/b.png"

            val result = resolver.resolve(
                source = Source(
                    hosts = listOf(
                        EventHostRequest(hostName = "신규A"),
                        EventHostRequest(hostId = 1L),
                        EventHostRequest(hostName = "신규B", thumbnailIndex = 1),
                    )
                ),
                hostThumbnail = null,
                hostThumbnails = listOf(logo0, logo1),
            )

            assertEquals(listOf(10L, 1L, 11L), result.map { it.id })
            assertNull(result[0].thumbnail)
            assertEquals("uploads/hosts/b.png", result[2].thumbnail)
            verify(exactly = 0) { fileStorageService.uploadFile(logo0, any()) }
        }

        @Test
        @DisplayName("hostName 이 기존 주최와 일치하면 로고를 업로드하지 않는다")
        fun doesNotUploadWhenNameMatchesExistingHost() {
            val logo = mockk<MultipartFile>()
            every { hostService.findOrCreateHost("기존주최", any()) } returns existingHost

            val result = resolver.resolve(
                Source(hosts = listOf(EventHostRequest(hostName = "기존주최", thumbnailIndex = 0))),
                hostThumbnail = null,
                hostThumbnails = listOf(logo),
            )

            assertEquals(listOf(existingHost), result)
            verify(exactly = 0) { fileStorageService.uploadFile(any(), any()) }
        }

        @Test
        @DisplayName("hosts 와 hostId/hostName 을 함께 쓰면 400")
        fun rejectsMixedWithLegacyFields() = assertInvalid("함께 사용할 수 없습니다") {
            resolver.resolve(Source(hosts = listOf(EventHostRequest(hostId = 1L)), hostId = 1L), null, emptyList())
        }

        @Test
        @DisplayName("hosts 와 hostThumbnail(단일)을 함께 쓰면 400")
        fun rejectsLegacyThumbnailWithHosts() = assertInvalid("hostThumbnails") {
            resolver.resolve(Source(hosts = listOf(EventHostRequest(hostId = 1L))), mockk(), emptyList())
        }

        @Test
        @DisplayName("빈 hosts 는 400")
        fun rejectsEmptyHosts() = assertInvalid("최소 1개") {
            resolver.resolve(Source(hosts = emptyList()), null, emptyList())
        }

        @Test
        @DisplayName("항목에 hostId 와 hostName 이 둘 다 있거나 둘 다 없으면 400")
        fun rejectsAmbiguousItem() {
            assertInvalid("하나만") {
                resolver.resolve(Source(hosts = listOf(EventHostRequest(hostId = 1L, hostName = "x"))), null, emptyList())
            }
            assertInvalid("하나만") {
                resolver.resolve(Source(hosts = listOf(EventHostRequest(hostName = " "))), null, emptyList())
            }
        }

        @Test
        @DisplayName("hostId 항목에 thumbnailIndex 를 지정하면 400")
        fun rejectsThumbnailIndexForExistingHost() = assertInvalid("thumbnailIndex") {
            resolver.resolve(
                Source(hosts = listOf(EventHostRequest(hostId = 1L, thumbnailIndex = 0))),
                null,
                listOf(mockk()),
            )
        }

        @Test
        @DisplayName("thumbnailIndex 가 hostThumbnails 범위를 벗어나면 400")
        fun rejectsOutOfRangeThumbnailIndex() = assertInvalid("범위") {
            resolver.resolve(Source(hosts = listOf(EventHostRequest(hostName = "신규", thumbnailIndex = 1))), null, listOf(mockk()))
        }

        @Test
        @DisplayName("같은 thumbnailIndex 를 여러 주최가 쓰면 400")
        fun rejectsDuplicatedThumbnailIndex() = assertInvalid("같은 thumbnailIndex") {
            resolver.resolve(
                Source(
                    hosts = listOf(
                        EventHostRequest(hostName = "신규A", thumbnailIndex = 0),
                        EventHostRequest(hostName = "신규B", thumbnailIndex = 0),
                    )
                ),
                null,
                listOf(mockk()),
            )
        }
    }

    @Nested
    @DisplayName("hostId / hostName (레거시)")
    inner class LegacyTests {

        @Test
        @DisplayName("hostId 로 단일 주최를 반환한다")
        fun resolvesByHostId() {
            assertEquals(listOf(existingHost), resolver.resolve(Source(hostId = 1L), null, emptyList()))
        }

        @Test
        @DisplayName("hostName 으로 새 주최를 만들 때 hostThumbnail 을 업로드한다")
        fun uploadsLegacyThumbnailOnCreate() {
            val logo = mockk<MultipartFile>()
            stubHostCreation("신규", id = 10L)
            every { fileStorageService.uploadFile(logo, "hosts") } returns "uploads/hosts/new.png"

            val result = resolver.resolve(Source(hostName = "신규"), logo, emptyList())

            assertEquals("uploads/hosts/new.png", result.single().thumbnail)
        }

        @Test
        @DisplayName("hostName 으로 새 주최를 만들 때 로고 파일이 없으면 legacyThumbnailFallback 을 쓴다")
        fun usesFallbackWhenNoFile() {
            stubHostCreation("신규", id = 10L)

            val result = resolver.resolve(Source(hostName = "신규"), null, emptyList(), legacyThumbnailFallback = "old.png")

            assertEquals("old.png", result.single().thumbnail)
        }

        @Test
        @DisplayName("레거시 방식에 hostThumbnails 를 보내면 400")
        fun rejectsHostThumbnailsWithLegacy() = assertInvalid("hostThumbnails") {
            resolver.resolve(Source(hostId = 1L), null, listOf(mockk()))
        }

        @Test
        @DisplayName("주최 정보가 하나도 없으면 400")
        fun rejectsMissingHost() = assertInvalid("hosts 또는 hostId/hostName") {
            resolver.resolve(Source(), null, emptyList())
        }
    }
}
