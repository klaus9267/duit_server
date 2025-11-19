package duit.server.util

import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.repository.HostRepository
import duit.server.domain.user.entity.User
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.repository.UserRepository
import duit.server.domain.view.repository.ViewRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.random.Random

@Component
class DummyDataGenerator(
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
    private val hostRepository: HostRepository,
    private val eventRepository: EventRepository,
    private val viewRepository: ViewRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jdbcTemplate: JdbcTemplate
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val BATCH_SIZE = 1000
        private const val HOST_COUNT = 10_000
        private const val EVENT_COUNT = 1_000_000
        private const val NULL_PROBABILITY = 0.3 // 30% null
        private val EVENT_TYPES = EventType.values()
        private val SAMPLE_TITLES = listOf(
            "Spring Boot 세미나", "JPA 워크숍", "Kotlin 컨퍼런스", "Docker 튜토리얼",
            "React 개발자 모임", "Vue.js 스터디", "Node.js 해커톤", "Python 강의",
            "AI/ML 세미나", "데이터 사이언스 워크숍", "블록체인 컨퍼런스", "모바일 앱 개발"
        )
    }

    private data class EventData(
        val title: String,
        val startAt: LocalDateTime,
        val endAt: LocalDateTime?,
        val recruitmentStartAt: LocalDateTime?,
        val recruitmentEndAt: LocalDateTime?,
        val uri: String,
        val thumbnail: String?,
        val isApproved: Boolean,
        val eventType: EventType,
        val hostId: Long
    )
    
    @Transactional
    fun generateAllDummyData() {
        logger.info("🚀 더미 데이터 생성을 시작합니다...")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 1. User & Admin 생성
            generateUserAndAdminInternal()
            
            // 2. Host 생성 (1만개)
            generateHostsInternal()

            // 3. Event 생성 (100만개)
            generateEventsInternal()

            // 4. View 생성 (100만개)
            generateViewsInternal()

            generateBookmarksForAllUsers()
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000.0
            
            logger.info("✅ 모든 더미 데이터 생성 완료! 총 소요 시간: ${duration}초")
            
        } catch (e: Exception) {
            logger.error("❌ 더미 데이터 생성 중 오류 발생", e)
            throw e
        }
    }
    
    @Transactional
    fun generateUserAndAdmin() {
        generateUserAndAdminInternal()
    }
    
    @Transactional
    fun generateDummyUsers(userCount: Int = 100) {
        logger.info("👤 더미 User 데이터 생성 시작... (${userCount}개)")

        val startTime = System.currentTimeMillis()

        val users = mutableListOf<User>()

        for (i in 1..userCount) {
            val user = User(
                email = "user${i}@duit.com",
                nickname = "유저${i}",
                providerType = ProviderType.values().random(),
                providerId = "user_${i}_${System.currentTimeMillis()}",
                autoAddBookmarkToCalendar = Random.nextBoolean(),
                deviceToken = generateRandomDeviceToken()
            )
            users.add(user)
        }

        userRepository.saveAll(users)

        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000.0

        logger.info("✅ 더미 User ${userCount}개 생성 완료! 소요 시간: ${duration}초")
    }

    private fun generateUserAndAdminInternal() {
        logger.info("👤 User & Admin 데이터 생성 시작...")

        // User 생성
        val user = User(
            email = "admin@duit.com",
            nickname = "관리자",
            providerType = ProviderType.GOOGLE,
            providerId = "admin_google_id_123",
            autoAddBookmarkToCalendar = true,
            deviceToken = generateRandomDeviceToken()
        )

        val savedUser = userRepository.save(user)
        logger.info("✅ User 생성 완료: ${savedUser.email}")

        // Admin 생성
        val admin = Admin(
            user = savedUser,
            adminId = "admin",
            password = passwordEncoder.encode("admin123")
        )

        adminRepository.save(admin)
        logger.info("✅ Admin 생성 완료: ${admin.adminId}")
    }
    
    @Transactional
    fun generateHosts() {
        generateHostsInternal()
    }
    
    private fun generateHostsInternal() {
        logger.info("🏢 Host 데이터 생성 시작... (${HOST_COUNT}개)")

        val batchCount = HOST_COUNT / BATCH_SIZE

        for (batch in 0 until batchCount) {
            val hostNames = mutableListOf<String>()

            for (i in 1..BATCH_SIZE) {
                val hostNumber = batch * BATCH_SIZE + i
                hostNames.add("Host_${hostNumber}")
            }

            batchInsertHosts(hostNames)

            if ((batch + 1) % 2 == 0) {
                logger.info("📊 Host 생성 진행률: ${((batch + 1) * BATCH_SIZE)}/${HOST_COUNT} (${((batch + 1) * 100 / batchCount)}%)")
            }
        }

        logger.info("✅ Host 생성 완료: ${HOST_COUNT}개")
    }

    private fun batchInsertHosts(hostNames: List<String>) {
        val sql = "INSERT INTO hosts (name, thumbnail) VALUES (?, ?)"

        jdbcTemplate.batchUpdate(sql, hostNames, BATCH_SIZE) { ps, hostName ->
            ps.setString(1, hostName)
            ps.setString(2, null) // thumbnail은 모두 null
        }
    }
    
    @Transactional
    fun generateEvents() {
        generateEventsInternal()
    }
    
    private fun generateEventsInternal() {
        logger.info("🎉 Event 데이터 생성 시작... (${EVENT_COUNT}개)")

        val batchCount = EVENT_COUNT / BATCH_SIZE
        val now = LocalDateTime.now()

        for (batch in 0 until batchCount) {
            val events = mutableListOf<EventData>()

            for (i in 1..BATCH_SIZE) {
                val eventNumber = batch * BATCH_SIZE + i

                // 지난 행사 50%, 예정 행사 50%
                val isPastEvent = Random.nextBoolean()
                val startAt = if (isPastEvent) {
                    // 과거 6개월 내
                    now.minusMonths(6).plusDays(Random.nextLong(0, 180))
                } else {
                    // 미래 6개월 내
                    now.plusDays(Random.nextLong(1, 180))
                }

                // endAt: 70% 확률로 startAt + 1~7일
                val endAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                    startAt.plusDays(Random.nextLong(1, 8))
                } else null

                // recruitmentStartAt: 30% 확률로 설정
                val recruitmentStartAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                    startAt.minusDays(Random.nextLong(7, 30))
                } else null

                // recruitmentEndAt: recruitmentStartAt이 있을 때만 설정
                val recruitmentEndAt = recruitmentStartAt?.let {
                    if (Random.nextDouble() > NULL_PROBABILITY) {
                        it.plusDays(Random.nextLong(1, 14))
                    } else null
                }

                val randomTitle = SAMPLE_TITLES.random()
                val hostId = Random.nextLong(1, HOST_COUNT.toLong() + 1)

                val event = EventData(
                    title = "${randomTitle}_${eventNumber}",
                    startAt = startAt,
                    endAt = endAt,
                    recruitmentStartAt = recruitmentStartAt,
                    recruitmentEndAt = recruitmentEndAt,
                    uri = "https://duit.com/events/${eventNumber}",
                    thumbnail = null, // 썸네일은 모두 null
                    isApproved = Random.nextDouble() > 0.3, // 70% 승인
                    eventType = EVENT_TYPES.random(),
                    hostId = hostId
                )

                events.add(event)
            }

            batchInsertEvents(events)

            if ((batch + 1) % 100 == 0) {
                val progress = ((batch + 1) * BATCH_SIZE)
                val percentage = ((batch + 1) * 100 / batchCount)
                logger.info("📊 Event 생성 진행률: ${progress}/${EVENT_COUNT} (${percentage}%)")
            }
        }

        logger.info("✅ Event 생성 완료: ${EVENT_COUNT}개")
    }

    private fun batchInsertEvents(events: List<EventData>) {
        val sql = """
            INSERT INTO events (title, start_at, end_at, recruitment_start_at, recruitment_end_at,
                               uri, thumbnail, is_approved, event_type, host_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """.trimIndent()

        jdbcTemplate.batchUpdate(sql, events, BATCH_SIZE) { ps, event ->
            ps.setString(1, event.title)
            ps.setTimestamp(2, Timestamp.valueOf(event.startAt))
            ps.setTimestamp(3, event.endAt?.let { Timestamp.valueOf(it) })
            ps.setTimestamp(4, event.recruitmentStartAt?.let { Timestamp.valueOf(it) })
            ps.setTimestamp(5, event.recruitmentEndAt?.let { Timestamp.valueOf(it) })
            ps.setString(6, event.uri)
            ps.setString(7, event.thumbnail)
            ps.setBoolean(8, event.isApproved)
            ps.setString(9, event.eventType.name)
            ps.setLong(10, event.hostId)
        }
    }
    
    @Transactional
    fun generateViews() {
        generateViewsInternal()
    }
    
    private fun generateViewsInternal() {
        logger.info("👀 View 데이터 생성 시작... (${EVENT_COUNT}개)")

        val batchCount = EVENT_COUNT / BATCH_SIZE

        for (batch in 0 until batchCount) {
            val viewDataList = mutableListOf<Pair<Int, Long>>() // (count, eventId)

            for (i in 1..BATCH_SIZE) {
                val eventId = (batch * BATCH_SIZE + i).toLong()
                val count = Random.nextInt(0, 1001) // 0~1000 조회수

                viewDataList.add(count to eventId)
            }

            batchInsertViews(viewDataList)

            if ((batch + 1) % 100 == 0) {
                val progress = ((batch + 1) * BATCH_SIZE)
                val percentage = ((batch + 1) * 100 / batchCount)
                logger.info("📊 View 생성 진행률: ${progress}/${EVENT_COUNT} (${percentage}%)")
            }
        }

        logger.info("✅ View 생성 완료: ${EVENT_COUNT}개")
    }

    private fun batchInsertViews(viewDataList: List<Pair<Int, Long>>) {
        val sql = "INSERT INTO views (count, event_id) VALUES (?, ?)"

        jdbcTemplate.batchUpdate(sql, viewDataList, BATCH_SIZE) { ps, viewData ->
            ps.setInt(1, viewData.first) // count
            ps.setLong(2, viewData.second) // eventId
        }
    }
    
    @Transactional
    fun generateBookmarksForAllUsers() {
        logger.info("🔖 전체 유저 북마크 데이터 생성 시작...")

        val startTime = System.currentTimeMillis()

        // 전체 User 조회
        val allUsers = userRepository.findAll()
        logger.info("📊 총 User 수: ${allUsers.size}")

        if (allUsers.isEmpty()) {
            logger.warn("⚠️ User가 없습니다. 먼저 User를 생성해주세요.")
            return
        }

        var totalBookmarks = 0

        allUsers.forEach { user ->
            val userId = user.id!!

            // User ID 1번은 1000개 고정, 나머지는 0~1000 랜덤
            val bookmarkCount = if (userId == 1L) {
                1000
            } else {
                Random.nextInt(0, 1001) // 0~1000
            }

            if (bookmarkCount > 0) {
                // 랜덤으로 Event ID 선택 (중복 없이)
                val randomEventIds = generateRandomEventIds(bookmarkCount)

                // JDBC Batch Insert
                batchInsertBookmarks(userId, randomEventIds)

                totalBookmarks += bookmarkCount
                logger.info("✅ User #${userId} 북마크 ${bookmarkCount}개 생성 완료")
            } else {
                logger.info("⏭️ User #${userId} 북마크 0개 (스킵)")
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000.0

        logger.info("✅ 전체 유저 북마크 생성 완료! 총 ${totalBookmarks}개, 소요 시간: ${duration}초")
    }

    private fun generateRandomEventIds(count: Int): List<Long> {
        // 전체 Event 개수 확인
        val totalEvents = eventRepository.count()

        if (totalEvents < count) {
            logger.warn("⚠️ Event 개수(${totalEvents})가 요청한 북마크 개수(${count})보다 적습니다.")
            return (1L..totalEvents).toList()
        }

        // 랜덤으로 Event ID 선택 (중복 없이)
        val allEventIds = (1L..totalEvents).toList()
        return allEventIds.shuffled().take(count)
    }

    private fun batchInsertBookmarks(userId: Long, eventIds: List<Long>) {
        val sql = """
            INSERT INTO bookmarks (user_id, event_id, is_added_to_calendar, created_at, updated_at)
            VALUES (?, ?, ?, NOW(), NOW())
        """.trimIndent()

        val batchCount = (eventIds.size + BATCH_SIZE - 1) / BATCH_SIZE

        for (batch in 0 until batchCount) {
            val start = batch * BATCH_SIZE
            val end = minOf(start + BATCH_SIZE, eventIds.size)
            val batchEventIds = eventIds.subList(start, end)

            jdbcTemplate.batchUpdate(sql, batchEventIds, BATCH_SIZE) { ps, eventId ->
                ps.setLong(1, userId)
                ps.setLong(2, eventId)
                ps.setBoolean(3, Random.nextBoolean()) // isAddedToCalendar 랜덤
            }

            if ((batch + 1) % 10 == 0 || batch == batchCount - 1) {
                val progress = end
                val percentage = (progress * 100 / eventIds.size)
                logger.info("📊 북마크 생성 진행률: ${progress}/${eventIds.size} (${percentage}%)")
            }
        }
    }

    fun getDataCount(): Map<String, Long> {
        return mapOf(
            "users" to userRepository.count(),
            "admins" to adminRepository.count(),
            "hosts" to hostRepository.count(),
            "events" to eventRepository.count(),
            "views" to viewRepository.count()
        )
    }
    
    private fun generateRandomDeviceToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..152).map { chars.random() }.joinToString("")
    }
}