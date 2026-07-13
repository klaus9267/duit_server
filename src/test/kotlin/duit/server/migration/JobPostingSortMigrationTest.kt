package duit.server.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@Testcontainers(disabledWithoutDocker = true)
class JobPostingSortMigrationTest {

    @Test
    fun `V3 마이그레이션은 실제 MySQL에서 정렬 메타데이터를 안전하게 백필한다`() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE job_postings (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        is_active BOOLEAN NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        receipt_close_dt VARCHAR(255),
                        sal_tp_nm VARCHAR(255)
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    INSERT INTO job_postings (id, is_active, created_at, receipt_close_dt, sal_tp_nm) VALUES
                        (1, TRUE, '2026-07-01 10:00:00', '20260731', '연봉36,000,000원 이상'),
                        (2, TRUE, '2026-06-01 10:00:00', '20260430', '월급 250만원 이상'),
                        (3, TRUE, '2026-05-01 10:00:00', '채용시까지', '시급20,000원 이상'),
                        (4, TRUE, '2026-04-01 10:00:00', '상시', '연봉 999,999,999,999,999만원')
                    """.trimIndent()
                )
            }
        }

        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("2")
            .load()
            .migrate()

        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT id, is_active, posted_at, expires_at, salary_min FROM job_postings ORDER BY id"
                ).use { resultSet ->
                    resultSet.next()
                    assertThat(resultSet.getLong("salary_min")).isEqualTo(36_000_000L)
                    assertThat(resultSet.getTimestamp("posted_at").toLocalDateTime())
                        .isEqualTo(java.time.LocalDateTime.of(2026, 7, 1, 10, 0))

                    resultSet.next()
                    assertThat(resultSet.getBoolean("is_active")).isFalse()
                    assertThat(resultSet.getLong("salary_min")).isEqualTo(30_000_000L)

                    resultSet.next()
                    assertThat(resultSet.getObject("expires_at")).isNull()
                    assertThat(resultSet.getLong("salary_min")).isEqualTo(50_160_000L)

                    resultSet.next()
                    assertThat(resultSet.getObject("salary_min")).isNull()
                }
            }
        }
    }

    private fun connection() = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")
    }
}
