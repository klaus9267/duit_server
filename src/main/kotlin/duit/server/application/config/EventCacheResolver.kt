package duit.server.application.config

import duit.server.domain.common.dto.pagination.PaginationField
import duit.server.domain.event.dto.EventCursorPaginationParam
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.interceptor.CacheOperationInvocationContext
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.stereotype.Component

/**
 * 정렬 필드에 따라 TTL이 다른 캐시를 선택하는 CacheResolver
 * - START_DATE / RECRUITMENT_DEADLINE → events-3m (3분 TTL)
 * - 그 외 (CREATED_AT, ID) → events-5m (5분 TTL)
 */
@Component("eventCacheResolver")
class EventCacheResolver(
    private val cacheManager: CacheManager
) : CacheResolver {

    override fun resolveCaches(context: CacheOperationInvocationContext<*>): Collection<Cache> {
        val param = context.args.firstOrNull() as? EventCursorPaginationParam
        val cacheName = when (param?.field) {
            PaginationField.START_DATE, PaginationField.RECRUITMENT_DEADLINE -> "events-3m"
            else -> "events-5m"
        }
        return listOfNotNull(cacheManager.getCache(cacheName))
    }
}
