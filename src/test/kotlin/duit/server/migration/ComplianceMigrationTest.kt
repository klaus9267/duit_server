package duit.server.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

/**
 * V5~V8 을 실제 MySQL 에서 실행해 컬럼 타입·NULL 허용·기본값과 데이터 변경을 확인한다.
 * 운영은 ddl-auto=validate 라 H2 스키마 테스트(MigrationColumnMappingTest)만으로는 SQL 오류를 잡을 수 없다.
 */
@Testcontainers(disabledWithoutDocker = true)
class ComplianceMigrationTest {

    private data class ColumnSpec(val type: String, val nullable: Boolean, val default: String?)

    @Test
    fun `V5~V8 마이그레이션은 실제 MySQL에서 엔티티 매핑과 같은 컬럼을 만든다`() {
        // 운영 스키마 중 V4~V8 이 다루는 컬럼만 최소로 만든다 (Boolean 은 Hibernate 가 BIT(1) 로 만든 컬럼)
        execute(
            """
            CREATE TABLE users (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                marketing BIT(1) NULL
            )
            """,
            "INSERT INTO users (id, marketing) VALUES (1, b'1'), (2, b'0'), (3, NULL)",
            "CREATE TABLE companies (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY)",
            "INSERT INTO companies (id) VALUES (1)",
            """
            CREATE TABLE job_postings (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                is_active BOOLEAN NOT NULL,
                created_at DATETIME(6) NOT NULL,
                receipt_close_dt VARCHAR(255),
                sal_tp_nm VARCHAR(255)
            )
            """,
            """
            INSERT INTO job_postings (id, is_active, created_at, receipt_close_dt, sal_tp_nm)
            VALUES (1, TRUE, '2026-07-01 10:00:00', '20991231', '시급 9,000원')
            """,
        )

        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("3")
            .load()
            .migrate()

        assertThat(column("users", "marketing_agreed_at")).isEqualTo(ColumnSpec("datetime(6)", true, null))
        assertThat(column("users", "terms_version")).isEqualTo(ColumnSpec("varchar(20)", true, null))
        assertThat(column("users", "terms_agreed_at")).isEqualTo(ColumnSpec("datetime(6)", true, null))
        assertThat(column("companies", "wage_defaulter")).isEqualTo(ColumnSpec("tinyint(1)", true, null))
        assertThat(column("companies", "wage_defaulter_checked_at")).isEqualTo(ColumnSpec("datetime(6)", true, null))
        assertThat(column("job_postings", "below_minimum_wage")).isEqualTo(ColumnSpec("tinyint(1)", false, "0"))

        // V5: 기존 동의는 모두 무효이므로 NULL 까지 포함해 전원 미동의
        assertThat(queryLongs("SELECT COUNT(*) FROM users WHERE marketing IS NULL OR marketing <> FALSE"))
            .containsExactly(0L)
        assertThat(queryLongs("SELECT COUNT(*) FROM users WHERE marketing_agreed_at IS NOT NULL")).containsExactly(0L)
        // V7: 기존 회사는 미확인(NULL), V8: 기존 공고는 다음 수집 때 판정하므로 FALSE 로 시작
        assertThat(queryLongs("SELECT COUNT(*) FROM companies WHERE wage_defaulter IS NOT NULL")).containsExactly(0L)
        assertThat(queryLongs("SELECT below_minimum_wage FROM job_postings")).containsExactly(0L)
    }

    private fun column(table: String, column: String): ColumnSpec =
        connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, table)
                statement.setString(2, column)
                statement.executeQuery().use { resultSet ->
                    check(resultSet.next()) { "$table.$column 컬럼이 없습니다" }
                    ColumnSpec(
                        type = resultSet.getString("COLUMN_TYPE").lowercase(),
                        nullable = resultSet.getString("IS_NULLABLE") == "YES",
                        default = resultSet.getString("COLUMN_DEFAULT"),
                    )
                }
            }
        }

    private fun queryLongs(sql: String): List<Long> =
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    generateSequence { if (resultSet.next()) resultSet.getLong(1) else null }.toList()
                }
            }
        }

    private fun execute(vararg sqls: String) {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                sqls.forEach { statement.execute(it.trimIndent()) }
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
