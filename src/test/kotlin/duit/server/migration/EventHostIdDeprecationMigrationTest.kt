package duit.server.migration

import duit.server.support.RequiresDocker
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

@RequiresDocker
@Testcontainers
class EventHostIdDeprecationMigrationTest {

    @BeforeEach
    fun dropTables() {
        execute(
            "DROP TABLE IF EXISTS event_hosts",
            "DROP TABLE IF EXISTS events",
            "DROP TABLE IF EXISTS hosts",
            "DROP TABLE IF EXISTS flyway_schema_history",
        )
    }

    @Test
    fun `V6 는 event_hosts 가 없는 행사를 백필하고 host_id FK 를 지워 행사에서 빠진 주최를 삭제할 수 있게 한다`() {
        // 운영처럼 Flyway 도입 전 Hibernate 가 자동 이름으로 만든 FK
        execute(
            "CREATE TABLE hosts (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255))",
            """
            CREATE TABLE events (
                id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255),
                host_id BIGINT,
                CONSTRAINT FKdqkiykj89a9gdfh6cepo057o6 FOREIGN KEY (host_id) REFERENCES hosts (id)
            )
            """.trimIndent(),
            "INSERT INTO hosts (id, name) VALUES (1, '주최A'), (2, '주최B')",
            "INSERT INTO events (id, title, host_id) VALUES (1, '행사1', 1)",
        )
        migrate(target = "5")
        // V5 적용 후 전환 구간에 다중 주최 이전 서버가 event_hosts 없이 만든 행사
        execute("INSERT INTO events (id, title, host_id) VALUES (2, '행사2', 2)")

        migrate()

        assertThat(rows("SELECT event_id, host_id, sort_order FROM event_hosts ORDER BY event_id"))
            .containsExactly(listOf<Any>(1L, 1L, 0), listOf<Any>(2L, 2L, 0))
        assertThat(rows(HOST_ID_FOREIGN_KEYS)).isEmpty()
        assertThat(rows(HOST_ID_NULLABLE)).containsExactly(listOf("YES"))

        // 행사 1의 주최를 2로 바꿔도 이 버전은 host_id 를 갱신하지 않는다. 남은 host_id 참조가 주최 1 삭제를 막지 않아야 한다
        execute(
            "UPDATE event_hosts SET host_id = 2 WHERE event_id = 1",
            "DELETE FROM hosts WHERE id = 1",
        )
        assertThat(rows("SELECT id FROM hosts")).containsExactly(listOf(2L))
    }

    @Test
    fun `V6 는 host_id FK 가 없는 DB 에도 적용되고 host_id 를 NULL 허용으로 바꾼다`() {
        execute(
            "CREATE TABLE hosts (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255))",
            "CREATE TABLE events (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255), host_id BIGINT NOT NULL)",
        )

        migrate()

        assertThat(rows(HOST_ID_NULLABLE)).containsExactly(listOf("YES"))
        execute("INSERT INTO events (title) VALUES ('주최 없는 행사')")
    }

    private fun migrate(target: String? = null) {
        Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("4")
            .apply { target?.let { target(it) } }
            .load()
            .migrate()
    }

    private fun execute(vararg sqls: String) {
        connection().use { connection ->
            connection.createStatement().use { statement -> sqls.forEach(statement::execute) }
        }
    }

    private fun rows(sql: String): List<List<Any?>> =
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    val columnCount = resultSet.metaData.columnCount
                    generateSequence { if (resultSet.next()) (1..columnCount).map(resultSet::getObject) else null }
                        .toList()
                }
            }
        }

    private fun connection() = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        private const val HOST_ID_FOREIGN_KEYS = """
            SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'events' AND COLUMN_NAME = 'host_id'
              AND REFERENCED_TABLE_NAME IS NOT NULL
        """

        private const val HOST_ID_NULLABLE = """
            SELECT IS_NULLABLE FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'events' AND COLUMN_NAME = 'host_id'
        """
    }
}
