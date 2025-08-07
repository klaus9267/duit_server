package duit.server.domain.view.service

import duit.server.domain.view.exception.ViewNotFoundException
import duit.server.domain.view.repository.ViewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ViewService(
    private val viewRepository: ViewRepository
) {

    @Transactional
    fun increaseCount(eventId: Long) {
        val view = viewRepository.findByEventId(eventId)
            ?: throw ViewNotFoundException(eventId)
        view.increaseCount()
    }
}