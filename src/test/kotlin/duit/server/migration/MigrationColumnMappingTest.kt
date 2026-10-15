package duit.server.migration

import duit.server.support.IntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Flyway 마이그레이션이 추가하는 컬럼명이 엔티티 매핑(Hibernate 가 만든 스키마)과 같은지 확인한다.
 * 운영은 ddl-auto=validate 라 이름이 어긋나면 기동이 실패한다.
 */
@DisplayName("마이그레이션 컬럼 매핑 테스트")
class MigrationColumnMappingTest : IntegrationTestSupport() {

    @Test
    fun `V5 마케팅 동의 컬럼은 엔티티 매핑과 일치한다`() {
        assertColumns("users", "marketing", "marketing_agreed_at")
    }

    @Test
    fun `V6 약관 동의 컬럼은 엔티티 매핑과 일치한다`() {
        assertColumns("users", "terms_version", "terms_agreed_at")
    }

    private fun assertColumns(table: String, vararg columns: String) {
        @Suppress("UNCHECKED_CAST")
        val actual = (entityManager.createNativeQuery(
            "SELECT LOWER(COLUMN_NAME) FROM INFORMATION_SCHEMA.COLUMNS WHERE LOWER(TABLE_NAME) = :table"
        ).setParameter("table", table).resultList as List<String>).toSet()

        columns.forEach { column ->
            assertTrue(column in actual, "$table.$column 컬럼이 엔티티 매핑에 없습니다. 실제 컬럼: $actual")
        }
    }
}
