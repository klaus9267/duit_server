package duit.server.domain.bookmark.service

import duit.server.application.common.ThrottledLogger
import duit.server.domain.bookmark.repository.BookmarkRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 사용자별 북마크한 행사 ID 캐시 (performance/decisions.md D-020).
 *
 * 행사 목록은 Redis 캐시가 맞아도 로그인 사용자의 북마크 표시를 덧입히려고 매번 DB 를 조회해,
 * 커넥션 획득 + SELECT + COMMIT 이 요청마다 나갔다. 사용자 대부분(83%)은 북마크가 0개라 그 조회는 대개 빈 결과였다.
 *
 * 정합성: 북마크를 바꾼 트랜잭션이 커밋된 뒤 키를 지운다. TTL(CacheConfig)은 무효화를 놓친 경우의 상한이다.
 * 장애: Redis 오류는 요청 실패로 번지지 않게 DB 조회로 대체한다(fail-open). 이 캐시 전에는 행사 상세가 Redis 를 쓰지 않았다.
 */
@Component
class BookmarkedEventIdsCache(
    private val bookmarkRepository: BookmarkRepository,
    cacheManager: CacheManager,
) {
    private val cache: Cache = requireNotNull(cacheManager.getCache(CACHE_NAME)) { "$CACHE_NAME 캐시 설정이 없다" }
    private val throttled = ThrottledLogger(LoggerFactory.getLogger(javaClass))

    /** 북마크가 없는 사용자도 빈 목록으로 캐시한다 — 그래야 다음 요청이 DB 를 보지 않는다 */
    fun get(userId: Long): Set<Long> {
        @Suppress("UNCHECKED_CAST")
        val cached = runCatching { cache.get(userId)?.get() as List<Long>? }.onFailure(::warnThrottled).getOrNull()
        if (cached != null) return cached.toSet()

        val loaded = bookmarkRepository.findEventIdsByUserId(userId)
        runCatching { cache.put(userId, loaded) }.onFailure(::warnThrottled)
        return loaded.toSet()
    }

    /**
     * 커밋 전에 지우면 그 사이 다른 요청이 커밋 전 DB(옛 북마크)를 읽어 다시 채울 수 있으므로 커밋 뒤에 지운다.
     * 롤백되면 DB 가 그대로라 지우지 않는다.
     */
    fun evictAfterCommit(userId: Long) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictQuietly(userId)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = evictQuietly(userId)
        })
    }

    /** 커밋은 이미 끝났으므로 무효화 실패로 요청을 실패시키지 않는다. 남은 옛 값은 TTL 안에 사라진다 */
    private fun evictQuietly(userId: Long) {
        runCatching { cache.evict(userId) }.onFailure(::warnThrottled)
    }

    private fun warnThrottled(e: Throwable) = throttled.warn("북마크 ID 캐시를 쓸 수 없어 DB 조회로 대체", e)

    companion object {
        const val CACHE_NAME = "bookmarked-event-ids"
    }
}
