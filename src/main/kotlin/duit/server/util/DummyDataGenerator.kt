package duit.server.util

import duit.server.domain.admin.entity.Admin
import duit.server.domain.admin.repository.AdminRepository
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.user.entity.ProviderType
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserRepository
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
    private val eventRepository: EventRepository,
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
            user.registerDeviceToken(user.deviceToken!!)
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
            deviceToken = generateRandomDeviceToken(),
        )
        user.registerDeviceToken(user.deviceToken!!)

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

    private fun disableEventIndexes() {
        logger.info("🔧 인덱스 비활성화 중... (11개)")

        // Foreign Key 체크 비활성화
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")

        // 11개 인덱스 삭제 (PK는 유지)
        val indexNames = listOf(
            "idx_status_id",
            "idx_status_created_at_id",
            "idx_status_start_at_asc_id",
            "idx_status_start_at_desc_id",
            "idx_status_recruitment_asc_id",
            "idx_status_recruitment_desc_id",
            "idx_status_group_created_at_id",
            "idx_status_group_start_at_asc_id",
            "idx_status_group_start_at_desc_id",
            "idx_status_group_recruitment_asc_id",
            "idx_status_group_recruitment_desc_id"
        )

        indexNames.forEach { indexName ->
            try {
                jdbcTemplate.execute("ALTER TABLE events DROP INDEX $indexName")
                logger.info("  ✓ $indexName 삭제 완료")
            } catch (e: Exception) {
                logger.warn("  ✗ $indexName 삭제 실패 (이미 없음): ${e.message}")
            }
        }

        logger.info("✅ 인덱스 비활성화 완료")
    }

    private fun enableEventIndexes() {
        logger.info("🔧 인덱스 재생성 중... (11개, 일괄 생성)")

        // 단일 ALTER TABLE로 모든 인덱스 일괄 생성 (테이블 스캔 1회)
        val bulkIndexQuery = """
            ALTER TABLE events
            ADD INDEX idx_status_id (status, id DESC),
            ADD INDEX idx_status_created_at_id (status, created_at DESC, id DESC),
            ADD INDEX idx_status_start_at_asc_id (status, start_at ASC, id DESC),
            ADD INDEX idx_status_start_at_desc_id (status, start_at DESC, id DESC),
            ADD INDEX idx_status_recruitment_asc_id (status, recruitment_end_at ASC, id DESC),
            ADD INDEX idx_status_recruitment_desc_id (status, recruitment_end_at DESC, id DESC),
            ADD INDEX idx_status_group_created_at_id (status_group, created_at DESC, id DESC),
            ADD INDEX idx_status_group_start_at_asc_id (status_group, start_at ASC, id DESC),
            ADD INDEX idx_status_group_start_at_desc_id (status_group, start_at DESC, id DESC),
            ADD INDEX idx_status_group_recruitment_asc_id (status_group, recruitment_end_at ASC, id DESC),
            ADD INDEX idx_status_group_recruitment_desc_id (status_group, recruitment_end_at DESC, id DESC),
            ALGORITHM=INPLACE, LOCK=NONE
        """.trimIndent()

        jdbcTemplate.execute(bulkIndexQuery)

        // Foreign Key 체크 재활성화
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")

        logger.info("✅ 인덱스 재생성 완료 (일괄 생성)")
    }

    private fun generateEventsInternal() {
        logger.info("🏗️ Event 데이터 생성 시작 (LOAD DATA INFILE 방식)... (${EVENT_COUNT}개)")

        val startTime = System.currentTimeMillis()

        try {
            // 1. 인덱스 비활성화
            disableEventIndexes()

            // 2. CSV 파일 생성
            logger.info("📝 CSV 파일 생성 중...")
            val csvPath = generateEventsCsv()
            logger.info("✅ CSV 파일 생성 완료: $csvPath")

            // 3. LOAD DATA INFILE 실행
            logger.info("🚀 MySQL LOAD DATA INFILE 실행 중...")
            loadEventsFromCsv(csvPath)

            // CSV 파일 삭제
            File(csvPath).delete()

            // 4. 인덱스 재생성
            enableEventIndexes()

            val endTime = System.currentTimeMillis()
            val totalDuration = (endTime - startTime) / 1000.0
            logger.info("✅ Event 생성 완료: ${EVENT_COUNT}개, 총 소요 시간: ${totalDuration}초")

        } catch (e: Exception) {
            logger.error("❌ Event 생성 실패", e)
            // 실패 시에도 인덱스 복구
            try {
                enableEventIndexes()
            } catch (indexError: Exception) {
                logger.error("❌ 인덱스 복구 실패", indexError)
            }
            throw e
        }
    }

    private fun generateEventsCsv(): String {
        val csvFile = File.createTempFile("events_", ".csv")
        csvFile.bufferedWriter().use { writer ->
            val now = LocalDateTime.now()

            for (i in 1..EVENT_COUNT) {
                // 지난 행사 50%, 예정 행사 50%
                val isPastEvent = Random.nextBoolean()
                val startAt = if (isPastEvent) {
                    generateRandomDateTime(now.minusMonths(6), Random.nextLong(0, 180))
                } else {
                    generateRandomDateTime(now, Random.nextLong(1, 180))
                }

                val endAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                    generateRandomDateTime(startAt, Random.nextLong(1, 8))
                } else null

                val recruitmentStartAt = if (Random.nextDouble() > NULL_PROBABILITY) {
                    generateRandomDateTime(startAt, -Random.nextLong(7, 30))
                } else null

                val recruitmentEndAt = recruitmentStartAt?.let {
                    if (Random.nextDouble() > NULL_PROBABILITY) {
                        generateRandomDateTime(it, Random.nextLong(1, 14))
                    } else null
                }

                val randomTitle = "${SAMPLE_TITLES.random()}_$i"
                val hostId = Random.nextLong(1, HOST_COUNT.toLong() + 1)
                val eventType = EVENT_TYPES.random().name
                val (status, statusGroup) = calculateStatusAndGroup(
                    now = now,
                    startAt = startAt,
                    endAt = endAt,
                    recruitmentStartAt = recruitmentStartAt,
                    recruitmentEndAt = recruitmentEndAt
                )

                // CSV 라인 작성 (NULL 처리: \N)
                writer.write(
                    "$randomTitle\t" +
                    "$startAt\t" +
                    "${endAt ?: "\\N"}\t" +
                    "${recruitmentStartAt ?: "\\N"}\t" +
                    "${recruitmentEndAt ?: "\\N"}\t" +
                    "https://duit.com/events/$i\t" +
                    "\\N\t" +  // thumbnail
                    "$eventType\t" +
                    "$hostId\t" +
                    "$status\t" +
                    "$statusGroup\n"
                )

                // 진행률 로깅 (10% 단위)
                if (i % 100000 == 0) {
                    logger.info("📊 CSV 생성 진행률: $i/${EVENT_COUNT} (${i * 100 / EVENT_COUNT}%)")
                }
            }
        }

        return csvFile.absolutePath
    }

    private fun loadEventsFromCsv(csvPath: String) {
        val sql = """
            LOAD DATA LOCAL INFILE '${csvPath.replace("\\", "/")}'
            INTO TABLE events
            FIELDS TERMINATED BY '\t'
            LINES TERMINATED BY '\n'
            (title, start_at, end_at, recruitment_start_at, recruitment_end_at,
             uri, thumbnail, is_approved, event_type, host_id, status, status_group)
            SET created_at = NOW(), updated_at = NOW()
        """.trimIndent()

        jdbcTemplate.execute(sql)
    }

    @Deprecated("Use calculateStatusAndGroup() with date parameters instead")
    private fun calculateStatus(): String {
        // Deprecated - 더 이상 사용 안 함
        return "PENDING"
    }

    private fun calculateStatusAndGroup(
        now: LocalDateTime,
        startAt: LocalDateTime,
        endAt: LocalDateTime?,
        recruitmentStartAt: LocalDateTime?,
        recruitmentEndAt: LocalDateTime?
    ): Pair<String, String> {
        // PENDING은 10% 확률로 랜덤 (승인되지 않은 행사)
        if (Random.nextDouble() < 0.10) {
            return Pair("PENDING", "PENDING")
        }

        // 행사 종료 여부 확인
        val actualEndAt = endAt ?: startAt.plusDays(1)  // endAt이 null이면 startAt + 1일
        if (now.isAfter(actualEndAt)) {
            return Pair("FINISHED", "FINISHED")
        }

        // 행사 진행 중 확인
        if (now.isAfter(startAt)) {
            return Pair("ACTIVE", "ACTIVE")
        }

        // 모집 종료 확인 (recruitmentEndAt이 있고, 현재 시각이 지난 경우)
        if (recruitmentEndAt != null && now.isAfter(recruitmentEndAt)) {
            return Pair("EVENT_WAITING", "ACTIVE")
        }

        // 모집 진행 중 확인 (recruitmentStartAt이 있고, 현재 시각이 지난 경우)
        if (recruitmentStartAt != null && now.isAfter(recruitmentStartAt)) {
            return Pair("RECRUITING", "ACTIVE")
        }

        // 모집 대기 (recruitmentStartAt이 있고, 아직 시작 안 함)
        if (recruitmentStartAt != null) {
            return Pair("RECRUITMENT_WAITING", "ACTIVE")
        }

        // 모집 정보 없이 행사만 대기 중
        return Pair("EVENT_WAITING", "ACTIVE")
    }

    /**
     * 랜덤한 날짜/시간 생성 (시, 분, 초 랜덤화)
     */
    private fun generateRandomDateTime(baseDate: LocalDateTime, daysOffset: Long): LocalDateTime {
        return baseDate
            .plusDays(daysOffset)
            .withHour(Random.nextInt(0, 24))        // 0~23시
            .withMinute(Random.nextInt(0, 60))      // 0~59분
            .withSecond(Random.nextInt(0, 60))      // 0~59초
            .withNano(0)                            // 나노초는 0으로 통일
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

    private fun generateRandomDeviceToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..152).map { chars.random() }.joinToString("")
    }
}