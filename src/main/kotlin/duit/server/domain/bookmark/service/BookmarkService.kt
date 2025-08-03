package duit.server.domain.bookmark.service

import duit.server.domain.bookmark.repository.BookmarkRepository
import org.springframework.stereotype.Service

@Service
class BookmarkService(private val bookmarkRepository: BookmarkRepository) {
}