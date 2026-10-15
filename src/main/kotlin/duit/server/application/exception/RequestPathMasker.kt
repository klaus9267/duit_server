package duit.server.application.exception

/**
 * 로그와 Discord(국외 서버)로 나가는 요청 경로에서 FCM 디바이스 토큰 같은 민감한 경로 변수를 가린다.
 */
object RequestPathMasker {

    private val deviceTokenSegment = Regex("(/api/v1/users/device/)[^/?#]+")

    fun mask(path: String): String = deviceTokenSegment.replace(path, "$1***")
}
