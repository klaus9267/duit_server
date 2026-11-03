package duit.server.application.common

import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicLong

/**
 * 같은 장애가 요청마다 반복될 때(예: Redis 다운 중 모든 캐시 호출 실패) 경고를 간격당 한 번만 남긴다.
 * 고부하에서 요청마다 로그를 쓰면 그 자체가 CPU·디스크 부하가 된다.
 */
class ThrottledLogger(
    private val logger: Logger,
    private val intervalMillis: Long = 60_000L,
) {
    private val lastWarnAt = AtomicLong()

    fun warn(message: String, e: Throwable) {
        val now = System.currentTimeMillis()
        val last = lastWarnAt.get()
        if (now - last >= intervalMillis && lastWarnAt.compareAndSet(last, now)) {
            logger.warn("{} ({}초 동안 같은 경고는 생략): {}", message, intervalMillis / 1000, e.toString())
        }
    }
}
