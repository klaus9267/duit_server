package duit.server.domain.host.service

import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.dto.HostRequest
import duit.server.domain.host.dto.HostUpdateRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.repository.HostRepository
import duit.server.infrastructure.external.file.FileStorageService
import io.mockk.*
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.multipart.MultipartFile
import java.util.*

@DisplayName("HostService 단위 테스트")
class HostServiceUnitTest {

    private lateinit var hostRepository: HostRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var fileStorageService: FileStorageService
    private lateinit var hostService: HostService

    @BeforeEach
    fun setUp() {
        hostRepository = mockk()
        eventRepository = mockk()
        fileStorageService = mockk(relaxed = true)
        hostService = HostService(hostRepository, eventRepository, fileStorageService)
    }

    @Nested
    @DisplayName("createHost")
    inner class CreateHostTests {

        @Test
        @DisplayName("이름이 중복되지 않으면 생성에 성공한다")
        fun createSuccess() {
            every { hostRepository.findByName("새주최") } returns null
            val host = Host(id = 1L, name = "새주최")
            every { hostRepository.save(any<Host>()) } returns host

            val result = hostService.createHost("새주최", null)

            assertEquals("새주최", result.name)
            assertNull(result.thumbnail)
        }

        @Test
        @DisplayName("썸네일과 함께 생성하면 업로드 후 저장한다")
        fun createWithThumbnail() {
            val mockFile = mockk<MultipartFile>()
            every { hostRepository.findByName("썸네일주최") } returns null
            every { fileStorageService.uploadFile(mockFile, "hosts") } returns "uploads/hosts/logo.png"
            val savedHost = Host(id = 2L, name = "썸네일주최", thumbnail = "uploads/hosts/logo.png")
            every { hostRepository.save(any<Host>()) } returns savedHost

            val result = hostService.createHost("썸네일주최", mockFile)

            assertEquals("uploads/hosts/logo.png", result.thumbnail)
        }

        @Test
        @DisplayName("이미 존재하는 이름이면 EntityExistsException이 발생한다")
        fun throwsOnDuplicateName() {
            val existing = Host(id = 1L, name = "중복주최")
            every { hostRepository.findByName("중복주최") } returns existing

            assertThrows<EntityExistsException> {
                hostService.createHost("중복주최", null)
            }
        }
    }

    @Nested
    @DisplayName("findOrCreateHost")
    inner class FindOrCreateHostTests {

        @Test
        @DisplayName("이름이 존재하면 기존 호스트를 반환한다")
        fun returnsExisting() {
            val existing = Host(id = 1L, name = "기존주최")
            every { hostRepository.findByName("기존주최") } returns existing

            val result = hostService.findOrCreateHost(HostRequest("기존주최"))

            assertEquals(1L, result.id)
            verify(exactly = 0) { hostRepository.save(any()) }
        }

        @Test
        @DisplayName("이름이 존재하지 않으면 새로 생성한다")
        fun createsNew() {
            every { hostRepository.findByName("신규주최") } returns null
            val newHost = Host(id = 2L, name = "신규주최")
            every { hostRepository.save(any<Host>()) } returns newHost

            val result = hostService.findOrCreateHost(HostRequest("신규주최"))

            assertEquals("신규주최", result.name)
            verify(exactly = 1) { hostRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("deleteHost")
    inner class DeleteHostTests {

        @Test
        @DisplayName("연결된 행사가 없으면 호스트를 삭제한다")
        fun deleteSuccess() {
            val host = Host(id = 1L, name = "삭제대상", thumbnail = "uploads/logo.png")
            every { hostRepository.findById(1L) } returns Optional.of(host)
            every { eventRepository.existsByHostId(1L) } returns false
            every { hostRepository.delete(host) } just runs

            hostService.deleteHost(1L)

            verify(exactly = 1) { fileStorageService.deleteFile("uploads/logo.png") }
            verify(exactly = 1) { hostRepository.delete(host) }
        }

        @Test
        @DisplayName("연결된 행사가 있으면 IllegalStateException이 발생한다")
        fun throwsWhenEventsLinked() {
            val host = Host(id = 1L, name = "삭제대상", thumbnail = "uploads/logo.png")
            every { hostRepository.findById(1L) } returns Optional.of(host)
            every { eventRepository.existsByHostId(1L) } returns true

            assertThrows<IllegalStateException> {
                hostService.deleteHost(1L)
            }

            verify(exactly = 0) { hostRepository.delete(any()) }
            verify(exactly = 0) { fileStorageService.deleteFile(any()) }
        }

        @Test
        @DisplayName("호스트가 존재하지 않으면 EntityNotFoundException이 발생한다")
        fun throwsWhenNotFound() {
            every { hostRepository.findById(999L) } returns Optional.empty()

            assertThrows<EntityNotFoundException> {
                hostService.deleteHost(999L)
            }
        }
    }

    @Nested
    @DisplayName("deleteHosts - 일괄 삭제 (부분 성공)")
    inner class DeleteHostsTests {

        @Test
        @DisplayName("모든 호스트가 삭제 가능하면 전부 삭제하고 blockedHosts는 비어있다")
        fun allDeletable() {
            val host1 = Host(id = 1L, name = "주최1", thumbnail = "uploads/logo1.png")
            val host2 = Host(id = 2L, name = "주최2", thumbnail = null)
            every { hostRepository.findAllById(listOf(1L, 2L)) } returns listOf(host1, host2)
            every { eventRepository.findHostIdsWithEvents(listOf(1L, 2L)) } returns emptyList()
            every { hostRepository.deleteAll(any<Iterable<Host>>()) } just runs

            val result = hostService.deleteHosts(listOf(1L, 2L))

            assertEquals(2, result["deletedCount"])
            assertEquals(emptyList<Map<String, Any>>(), result["blockedHosts"])
            verify(exactly = 1) { fileStorageService.deleteFile("uploads/logo1.png") }
            verify(exactly = 1) { hostRepository.deleteAll(match<Iterable<Host>> { it.toList() == listOf(host1, host2) }) }
        }

        @Test
        @DisplayName("일부에 행사가 있으면 가능한 것만 삭제하고 차단된 것은 응답에 포함한다")
        fun partialDeletable() {
            val host1 = Host(id = 1L, name = "주최1", thumbnail = "uploads/logo1.png")
            val host2 = Host(id = 2L, name = "주최2", thumbnail = null)
            val host3 = Host(id = 3L, name = "주최3", thumbnail = "uploads/logo3.png")
            every { hostRepository.findAllById(listOf(1L, 2L, 3L)) } returns listOf(host1, host2, host3)
            every { eventRepository.findHostIdsWithEvents(listOf(1L, 2L, 3L)) } returns listOf(2L)
            every { hostRepository.deleteAll(any<Iterable<Host>>()) } just runs

            val result = hostService.deleteHosts(listOf(1L, 2L, 3L))

            assertEquals(2, result["deletedCount"])
            @Suppress("UNCHECKED_CAST")
            val blocked = result["blockedHosts"] as List<Map<String, Any>>
            assertEquals(1, blocked.size)
            assertEquals(2L, blocked[0]["id"])
            assertEquals("주최2", blocked[0]["name"])
            verify(exactly = 1) { fileStorageService.deleteFile("uploads/logo1.png") }
            verify(exactly = 1) { fileStorageService.deleteFile("uploads/logo3.png") }
            verify(exactly = 1) { hostRepository.deleteAll(match<Iterable<Host>> { it.toList() == listOf(host1, host3) }) }
        }

        @Test
        @DisplayName("모두 행사가 있으면 아무것도 삭제하지 않고 전부 차단된다")
        fun allBlocked() {
            val host1 = Host(id = 1L, name = "주최1")
            val host2 = Host(id = 2L, name = "주최2")
            every { hostRepository.findAllById(listOf(1L, 2L)) } returns listOf(host1, host2)
            every { eventRepository.findHostIdsWithEvents(listOf(1L, 2L)) } returns listOf(1L, 2L)
            every { hostRepository.deleteAll(any<Iterable<Host>>()) } just runs

            val result = hostService.deleteHosts(listOf(1L, 2L))

            assertEquals(0, result["deletedCount"])
            @Suppress("UNCHECKED_CAST")
            val blocked = result["blockedHosts"] as List<Map<String, Any>>
            assertEquals(2, blocked.size)
            verify(exactly = 0) { fileStorageService.deleteFile(any()) }
            verify(exactly = 1) { hostRepository.deleteAll(match<Iterable<Host>> { it.toList().isEmpty() }) }
        }

        @Test
        @DisplayName("존재하지 않는 ID는 무시된다")
        fun ignoresNonExistingIds() {
            every { hostRepository.findAllById(listOf(999L)) } returns emptyList()
            every { eventRepository.findHostIdsWithEvents(listOf(999L)) } returns emptyList()
            every { hostRepository.deleteAll(any<Iterable<Host>>()) } just runs

            val result = hostService.deleteHosts(listOf(999L))

            assertEquals(0, result["deletedCount"])
            assertEquals(emptyList<Map<String, Any>>(), result["blockedHosts"])
        }
    }

    @Nested
    @DisplayName("updateHost")
    inner class UpdateHostTests {

        @Test
        @DisplayName("썸네일 변경 없이 이름만 수정한다")
        fun updateNameOnly() {
            val host = Host(id = 1L, name = "기존이름", thumbnail = "old.png")
            every { hostRepository.findById(1L) } returns Optional.of(host)
            every { hostRepository.save(host) } returns host

            val result = hostService.updateHost(1L, HostUpdateRequest("새이름", false), null)

            assertEquals("새이름", result.name)
            assertEquals("old.png", result.thumbnail)
        }

        @Test
        @DisplayName("deleteThumbnail=true이면 기존 썸네일을 삭제하고 null로 설정한다")
        fun deletesExistingThumbnail() {
            val host = Host(id = 1L, name = "주최", thumbnail = "old.png")
            every { hostRepository.findById(1L) } returns Optional.of(host)
            every { hostRepository.save(host) } returns host

            val result = hostService.updateHost(1L, HostUpdateRequest("주최", true), null)

            assertNull(result.thumbnail)
            verify(exactly = 1) { fileStorageService.deleteFile("old.png") }
        }

        @Test
        @DisplayName("새 썸네일을 업로드하면 기존 것을 삭제하고 교체한다")
        fun replacesWithNewThumbnail() {
            val host = Host(id = 1L, name = "주최", thumbnail = "old.png")
            val newFile = mockk<MultipartFile>()
            every { hostRepository.findById(1L) } returns Optional.of(host)
            every { fileStorageService.uploadFile(newFile, "hosts") } returns "new.png"
            every { hostRepository.save(host) } returns host

            val result = hostService.updateHost(1L, HostUpdateRequest("주최", false), newFile)

            assertEquals("new.png", result.thumbnail)
            verify(exactly = 1) { fileStorageService.deleteFile("old.png") }
        }

        @Test
        @DisplayName("존재하지 않는 호스트를 수정하면 EntityNotFoundException이 발생한다")
        fun throwsWhenNotFound() {
            every { hostRepository.findById(999L) } returns Optional.empty()

            assertThrows<EntityNotFoundException> {
                hostService.updateHost(999L, HostUpdateRequest("이름", false), null)
            }
        }
    }
}
