package duit.server.application.config

import duit.server.ServerApplication
import duit.server.domain.event.entity.QEvent
import duit.server.util.DummyDataGenerator
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory

/**
 * 풀 커넥션이 autocommit=false 라서(AGENTS.md, D-016) 트랜잭션 밖에서 JDBC 로 직접 쓴 내용은 예외 없이 롤백된다. 이 설정 전에는 커밋되던 쓰기다.
 * JPA 의 save·@Modifying 은 스스로 트랜잭션을 열거나 트랜잭션이 없으면 예외가 나서 이 설정의 영향을 받지 않으므로, main 에서 JDBC 를 직접 쓰는 클래스만 허용 목록으로 제한한다.
 * import 없이 쓴 경우(완전한 이름, 람다 인자로 추론된 Connection)도 잡도록 소스가 아니라 컴파일된 클래스를 검사한다.
 */
@DisplayName("main 코드의 JDBC 직접 사용 제한")
class DirectJdbcUsageGuardTest {

    @Test
    @DisplayName("JDBC 를 직접 쓰는 main 클래스는 허용 목록에 있는 것뿐이다")
    fun onlyAllowedClassesUseJdbcDirectly() {
        val violations = mainUsages.filterKeys { it !in ALLOWED }
        assertTrue(violations.isEmpty()) {
            "트랜잭션 밖의 JDBC 쓰기는 커밋되지 않는다 (hikari.auto-commit=false). JPA 로 바꾸거나, " +
                "JDBC 가 꼭 필요하면 쓰기를 @Transactional 안에 두고 ALLOWED 에 이유와 함께 추가할 것:\n" +
                violations.entries.joinToString("\n") { (type, refs) -> "  $type → $refs" }
        }
    }

    @Test
    @DisplayName("허용 목록의 클래스는 실제로 JDBC 를 직접 쓴다")
    fun allowedClassesStillUseJdbc() {
        assertEquals(ALLOWED, ALLOWED intersect mainUsages.keys, "JDBC 를 더 이상 직접 쓰지 않는 클래스는 ALLOWED 에서 뺄 것")
    }

    @Test
    @DisplayName("import 없이 람다 인자로만 쓴 Connection 도 잡는다")
    fun detectsConnectionWithoutImport() {
        val fixture = InferredConnectionWriter::class.java
        val dir = classesRoot(fixture).resolve(fixture.packageName.replace('.', '/'))
        val refs = Files.newDirectoryStream(dir, "${fixture.simpleName}*.class").use { files -> files.flatMap(::jdbcReferences).toSet() }
        assertTrue("java.sql.Connection" in refs) { "탐지 결과: $refs" }
    }

    companion object {
        // 허용은 클래스 단위다. 허용된 클래스에 새로 넣는 JDBC 코드는 이 테스트가 잡지 못하므로 리뷰에서 트랜잭션 안인지 확인할 것
        private val ALLOWED = setOf(
            // local 프로파일 전용 DummyDataController 에서만 호출된다. JDBC 호출은 모두 @Transactional 인 generateAllDummyData 경로 안
            DummyDataGenerator::class.java.name,
        )

        // SQL 을 실행할 수 있는 타입: Spring JDBC 의 실행 API(core·datasource·object), javax.sql(DataSource 등), java.sql 의 커넥션·문장 타입.
        // Spring JDBC 예외(CannotGetJdbcConnectionException 등)나 java.sql.Timestamp 같은 값 타입은 SQL 을 실행하지 않으므로 제외
        private val JDBC_REFERENCE = Regex(
            """org/springframework/jdbc/(?:core|datasource|object)/[\w/$]+|javax/sql/\w+|java/sql/(?:Connection|Statement|PreparedStatement|CallableStatement|DriverManager)(?![\w$])"""
        )

        // main 의 Kotlin 출력과 Java 출력(kapt 가 만든 QueryDSL Q 클래스. Java 소스를 추가하면 같은 곳에 컴파일된다)
        private val mainUsages by lazy {
            jdbcUsages(listOf(ServerApplication::class.java, QEvent::class.java).map(::classesRoot).distinct())
        }

        private fun classesRoot(type: Class<*>): Path =
            Paths.get(type.protectionDomain.codeSource.location.toURI()).also {
                check(it.isDirectory()) { "${type.name} 을 클래스 디렉터리가 아닌 곳에서 읽었다: $it" }
            }

        /** 최상위 클래스 이름 → 참조하는 JDBC 타입. 람다·중첩 클래스(`Outer$…`)는 최상위 클래스로 합친다 */
        private fun jdbcUsages(roots: List<Path>): Map<String, Set<String>> =
            roots.flatMap { root ->
                Files.walk(root).use { paths -> paths.filter { it.toString().endsWith(".class") }.toList() }
                    .map { file -> topLevelClassName(root.relativize(file)) to jdbcReferences(file) }
            }
                .filter { (_, refs) -> refs.isNotEmpty() }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, refs) -> refs.flatten().toSortedSet() }

        // 클래스 이름·디스크립터는 상수 풀에 ASCII 그대로 들어 있고, 항목 사이의 태그 바이트가 매치를 끊는다.
        // 문자열 상수에 같은 '/' 표기 경로가 있어도 잡히지만, 그 경우는 테스트 실패로 드러난다
        private fun jdbcReferences(classFile: Path): Set<String> =
            JDBC_REFERENCE.findAll(String(Files.readAllBytes(classFile), Charsets.ISO_8859_1))
                .map { it.value.replace('/', '.') }
                .toSet()

        private fun topLevelClassName(relative: Path): String =
            relative.invariantSeparatorsPathString.removeSuffix(".class").substringBefore('$').replace('/', '.')
    }
}

/** 탐지 확인용 (실행하지 않음): java.sql 을 import 하지 않고 람다 인자로 받은 Connection 으로 쓴다 */
@Suppress("unused")
private class InferredConnectionWriter(private val entityManager: EntityManager) {
    fun reset() = entityManager.unwrap(Session::class.java).doWork { connection ->
        connection.createStatement().use { it.executeUpdate("UPDATE views SET count = 0") }
    }
}
