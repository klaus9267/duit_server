package duit.server.infrastructure.external.googleform

import duit.server.application.controller.dto.googleform.FileInfo
import duit.server.application.controller.dto.googleform.GoogleFormResult
import duit.server.application.controller.dto.host.HostRequest
import duit.server.domain.host.entity.Host
import duit.server.domain.host.service.HostService
import duit.server.domain.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GoogleFormProcessor(
    private val userService: UserService,
    private val hostService: HostService,
) {
    private val logger = LoggerFactory.getLogger(GoogleFormProcessor::class.java)

    /**
     * Google Forms 응답 처리
     */
    fun handleGoogleFormResult(resullt: GoogleFormResult) {
        val formData = resullt.formData

        val eventTitle = formData["행사 제목"]
        val eventStartDate = formData["행사 시작 날짜"]
        val eventEndDate = formData["행사 종료 날짜"]
        val eventUrl = formData["행사 정보 상세 정보 페이지 주소"]
        val recruitStartDate = formData["모집 시작 날짜"]
        val recruitEndDate = formData["모집 종료 날짜"]
        val hostName = formData["주최 기관명"]

        val eventThumbnail = formData["행사 썸네일"]
        val hostThumbnail = formData["주최 기관 로고"]

        val host = createHost(hostName!!, null)

        // event 연결
    }

    fun createHost(name: String, logoFile: FileInfo? = null): Host {
        val hostRequest = HostRequest(
            name = name,
            thumbnail = null
        )

        //todo 파일 처리 필요

        return hostService.createHost(hostRequest)
    }
}