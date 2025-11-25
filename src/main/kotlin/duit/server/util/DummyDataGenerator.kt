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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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
        logger.info("🏢 Host 데이터 생성 시작 (LOAD DATA INFILE 방식)... (${HOST_COUNT}개)")

        val startTime = System.currentTimeMillis()
        val tempFile = File.createTempFile("hosts_", ".csv")

        try {
            // 1. CSV 파일 생성
            logger.info("📝 CSV 파일 생성 중...")
            BufferedWriter(FileWriter(tempFile)).use { writer ->
                for (i in 1..HOST_COUNT) {
                    writer.write("Host_${i},\\N\n")

                    if (i % 2000 == 0) {
                        logger.info("📊 CSV 생성 진행률: ${i}/${HOST_COUNT} (${i * 100 / HOST_COUNT}%)")
                    }
                }
            }

            // 2. LOAD DATA INFILE 실행
            logger.info("🚀 MySQL LOAD DATA INFILE 실행 중...")
            val sql = """
                LOAD DATA LOCAL INFILE '${tempFile.absolutePath.replace("\\", "/")}'
                INTO TABLE hosts
                FIELDS TERMINATED BY ','
                LINES TERMINATED BY '\n'
                (name, @thumbnail)
                SET thumbnail = NULLIF(@thumbnail, '\\N')
            """.trimIndent()

            jdbcTemplate.execute(sql)

            val endTime = System.currentTimeMillis()
            logger.info("✅ Host 생성 완료: ${HOST_COUNT}개, 소요 시간: ${(endTime - startTime) / 1000.0}초")

        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
    
    @Transactional
    fun generateEvents() {
        generateEventsInternal()
    }

    private fun generateEventsInternal() {
        logger.info("🎉 Event 데이터 생성 시작 (LOAD DATA INFILE 방식)... (${EVENT_COUNT}개)")

        val startTime = System.currentTimeMillis()
        val tempFile = File.createTempFile("events_", ".csv")

        try {
            // 1. CSV 파일 생성
            logger.info("📝 CSV 파일 생성 중...")
            val csvStartTime = System.currentTimeMillis()

            BufferedWriter(FileWriter(tempFile), 8192 * 4).use { writer ->
                val now = LocalDateTime.now()
                val sb = StringBuilder()

                for (i in 1..EVENT_COUNT) {
                    // 지난 행사 50%, 예정 행사 50%
                    val isPastEvent = Random.nextBoolean()
                    val startAt = if (isPastEvent) {
                        now.minusMonths(6).plusDays(Random.nextLong(0, 180))
                    } else {
                        now.plusDays(Random.nextLong(1, 180))
                    }

                    val endAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                        startAt.plusDays(Random.nextLong(1, 8))
                    } else null

                    val recruitmentStartAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                        startAt.minusDays(Random.nextLong(7, 30))
                    } else null

                    val recruitmentEndAt = recruitmentStartAt?.let {
                        if (Random.nextDouble() > NULL_PROBABILITY) {
                            it.plusDays(Random.nextLong(1, 14))
                        } else null
                    }

                    val randomTitle = SAMPLE_TITLES.random().replace(",", "")
                    val hostId = Random.nextLong(1, HOST_COUNT.toLong() + 1)
                    val isApproved = if (Random.nextDouble() > 0.3) 1 else 0
                    val eventType = EVENT_TYPES.random().name
                    val status = calculateStatus()

                    // StringBuilder 사용 (빠름)
                    sb.append(randomTitle).append('_').append(i).append(',')
                    sb.append(startAt.format(DATE_TIME_FORMATTER)).append(',')
                    sb.append(endAt?.format(DATE_TIME_FORMATTER) ?: "\\N").append(',')
                    sb.append(recruitmentStartAt?.format(DATE_TIME_FORMATTER) ?: "\\N").append(',')
                    sb.append(recruitmentEndAt?.format(DATE_TIME_FORMATTER) ?: "\\N").append(',')
                    sb.append("https://duit.com/events/").append(i).append(',')
                    sb.append("\\N,") // thumbnail
                    sb.append(isApproved).append(',')
                    sb.append(eventType).append(',')
                    sb.append(hostId).append(',')
                    sb.append(status).append('\n')

                    // 10000개마다 flush (메모리 효율)
                    if (i % 10000 == 0) {
                        writer.write(sb.toString())
                        sb.clear()

                        if (i % 50000 == 0) {
                            logger.info("📊 CSV 생성 진행률: ${i}/${EVENT_COUNT} (${i * 100 / EVENT_COUNT}%)")
                        }
                    }
                }

                // 남은 데이터 flush
                if (sb.isNotEmpty()) {
                    writer.write(sb.toString())
                }
            }

            val csvEndTime = System.currentTimeMillis()
            logger.info("✅ CSV 파일 생성 완료: ${(csvEndTime - csvStartTime) / 1000.0}초")

            // 2. LOAD DATA INFILE 실행
            logger.info("🚀 MySQL LOAD DATA INFILE 실행 중...")
            val loadStartTime = System.currentTimeMillis()

            val sql = """
                LOAD DATA LOCAL INFILE '${tempFile.absolutePath.replace("\\", "/")}'
                INTO TABLE events
                FIELDS TERMINATED BY ','
                LINES TERMINATED BY '\n'
                (title, start_at, @end_at, @recruitment_start_at, @recruitment_end_at,
                 uri, @thumbnail, is_approved, event_type, host_id, status)
                SET
                    end_at = NULLIF(@end_at, '\\N'),
                    recruitment_start_at = NULLIF(@recruitment_start_at, '\\N'),
                    recruitment_end_at = NULLIF(@recruitment_end_at, '\\N'),
                    thumbnail = NULLIF(@thumbnail, '\\N'),
                    created_at = NOW(),
                    updated_at = NOW()
            """.trimIndent()

            jdbcTemplate.execute(sql)

            val loadEndTime = System.currentTimeMillis()
            logger.info("✅ LOAD DATA INFILE 완료: ${(loadEndTime - loadStartTime) / 1000.0}초")

            val endTime = System.currentTimeMillis()
            val totalDuration = (endTime - startTime) / 1000.0
            logger.info("✅ Event 생성 완료: ${EVENT_COUNT}개, 총 소요 시간: ${totalDuration}초")

        } catch (e: Exception) {
            logger.error("❌ Event 생성 실패", e)
            throw e
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun batchInsertEvents(events: List<EventData>) {
        val sql = """
            INSERT INTO events (title, start_at, end_at, recruitment_start_at, recruitment_end_at,
                               uri, thumbnail, is_approved, event_type, host_id, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
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
            ps.setString(11, calculateStatus())
        }
    }

    private fun calculateStatus(): String {
        // status 비율: ACTIVE 40%, PENDING 10%, RECRUITING 10%, FINISHED 40%
        val random = Random.nextDouble()

        return when {
            random < 0.40 -> "ACTIVE"           // 0.00 ~ 0.40 (40%)
            random < 0.50 -> "PENDING"          // 0.40 ~ 0.50 (10%)
            random < 0.60 -> "RECRUITING"       // 0.50 ~ 0.60 (10%)
            else -> "FINISHED"                  // 0.60 ~ 1.00 (40%)
        }
    }
    
    @Transactional
    fun generateViews() {
        generateViewsInternal()
    }
    
    private fun generateViewsInternal() {
        logger.info("👀 View 데이터 생성 시작 (LOAD DATA INFILE 방식)... (${EVENT_COUNT}개)")

        val startTime = System.currentTimeMillis()
        val tempFile = File.createTempFile("views_", ".csv")

        try {
            // 1. CSV 파일 생성
            logger.info("📝 CSV 파일 생성 중...")
            BufferedWriter(FileWriter(tempFile), 8192 * 4).use { writer ->
                val sb = StringBuilder()

                for (i in 1..EVENT_COUNT) {
                    val count = Random.nextInt(0, 1001) // 0~1000 조회수
                    sb.append(count).append(',').append(i).append('\n')

                    if (i % 10000 == 0) {
                        writer.write(sb.toString())
                        sb.clear()

                        if (i % 100000 == 0) {
                            logger.info("📊 CSV 생성 진행률: ${i}/${EVENT_COUNT} (${i * 100 / EVENT_COUNT}%)")
                        }
                    }
                }

                if (sb.isNotEmpty()) {
                    writer.write(sb.toString())
                }
            }

            // 2. LOAD DATA INFILE 실행
            logger.info("🚀 MySQL LOAD DATA INFILE 실행 중...")
            val sql = """
                LOAD DATA LOCAL INFILE '${tempFile.absolutePath.replace("\\", "/")}'
                INTO TABLE views
                FIELDS TERMINATED BY ','
                LINES TERMINATED BY '\n'
                (count, event_id)
            """.trimIndent()

            jdbcTemplate.execute(sql)

            val endTime = System.currentTimeMillis()
            logger.info("✅ View 생성 완료: ${EVENT_COUNT}개, 소요 시간: ${(endTime - startTime) / 1000.0}초")

        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }
    
    @Transactional
    fun generateBookmarksForAllUsers() {
        logger.info("🔖 전체 유저 북마크 데이터 생성 시작 (LOAD DATA INFILE 방식)...")

        val startTime = System.currentTimeMillis()

        // 전체 User 조회
        val allUsers = userRepository.findAll()
        logger.info("📊 총 User 수: ${allUsers.size}")

        if (allUsers.isEmpty()) {
            logger.warn("⚠️ User가 없습니다. 먼저 User를 생성해주세요.")
            return
        }

        val tempFile = File.createTempFile("bookmarks_", ".csv")

        try {
            // 1. CSV 파일 생성
            logger.info("📝 CSV 파일 생성 중...")
            val csvStartTime = System.currentTimeMillis()
            var totalBookmarks = 0

            BufferedWriter(FileWriter(tempFile), 8192 * 4).use { writer ->
                val sb = StringBuilder()
                val totalEvents = eventRepository.count()

                allUsers.forEach { user ->
                    val userId = user.id!!

                    // User ID 1번은 1000개 고정, 나머지는 0~1000 랜덤
                    val bookmarkCount = if (userId == 1L) {
                        1000
                    } else {
                        Random.nextInt(0, 1001)
                    }

                    if (bookmarkCount > 0) {
                        // 랜덤 Event ID 생성 (중복 없이)
                        val allEventIds = (1L..totalEvents).toList()
                        val randomEventIds = allEventIds.shuffled().take(bookmarkCount)

                        randomEventIds.forEach { eventId ->
                            val isAddedToCalendar = if (Random.nextBoolean()) 1 else 0
                            sb.append(userId).append(',')
                            sb.append(eventId).append(',')
                            sb.append(isAddedToCalendar).append('\n')

                            totalBookmarks++

                            if (totalBookmarks % 10000 == 0) {
                                writer.write(sb.toString())
                                sb.clear()

                                if (totalBookmarks % 50000 == 0) {
                                    logger.info("📊 CSV 생성 진행률: ${totalBookmarks}개")
                                }
                            }
                        }

                        logger.info("✅ User #${userId} 북마크 ${bookmarkCount}개 CSV 생성")
                    }
                }

                if (sb.isNotEmpty()) {
                    writer.write(sb.toString())
                }
            }

            val csvEndTime = System.currentTimeMillis()
            logger.info("✅ CSV 파일 생성 완료: 총 ${totalBookmarks}개, ${(csvEndTime - csvStartTime) / 1000.0}초")

            // 2. LOAD DATA INFILE 실행
            logger.info("🚀 MySQL LOAD DATA INFILE 실행 중...")
            val loadStartTime = System.currentTimeMillis()

            val sql = """
                LOAD DATA LOCAL INFILE '${tempFile.absolutePath.replace("\\", "/")}'
                INTO TABLE bookmarks
                FIELDS TERMINATED BY ','
                LINES TERMINATED BY '\n'
                (user_id, event_id, is_added_to_calendar)
                SET created_at = NOW(), updated_at = NOW()
            """.trimIndent()

            jdbcTemplate.execute(sql)

            val loadEndTime = System.currentTimeMillis()
            logger.info("✅ LOAD DATA INFILE 완료: ${(loadEndTime - loadStartTime) / 1000.0}초")

            val endTime = System.currentTimeMillis()
            val totalDuration = (endTime - startTime) / 1000.0
            logger.info("✅ 전체 유저 북마크 생성 완료! 총 ${totalBookmarks}개, 총 소요 시간: ${totalDuration}초")

        } finally {
            if (tempFile.exists()) tempFile.delete()
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