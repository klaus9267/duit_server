package duit.server.util

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Profile("local")  // local 프로파일에서만 활성화
@Tag(name = "DummyData", description = "더미 데이터 생성 API (local 프로파일 전용)")
@RestController
@RequestMapping("/api/v1/dummy")
class DummyDataController(
    private val dummyDataGenerator: DummyDataGenerator
) {
    
    @Operation(summary = "전체 더미 데이터 생성", description = "Host, Event, View 더미 데이터를 모두 생성합니다. (매우 오래 걸림)")
    @PostMapping("/generate-all")
    @ResponseStatus(HttpStatus.OK)
    fun generateAllDummyData(): String {
        dummyDataGenerator.generateAllDummyData()
        return "✅ 모든 더미 데이터 생성이 완료되었습니다!"
    }

    @Operation(
        summary = "더미 User 생성",
        description = "테스트용 더미 User를 생성합니다."
    )
    @PostMapping("/generate-users")
    @ResponseStatus(HttpStatus.OK)
    fun generateDummyUsers(
        @RequestParam(defaultValue = "100") count: Int
    ): String {
        dummyDataGenerator.generateDummyUsers(count)
        return "✅ 더미 User ${count}개 생성이 완료되었습니다!"
    }

    @Operation(
        summary = "전체 유저 북마크 생성",
        description = """
            전체 유저에게 랜덤 Event 북마크를 생성합니다.
            - User #1: 1000개 고정
            - 나머지 User: 0~1000개 랜덤
            - Event 중복 없음
        """
    )
    @PostMapping("/generate-bookmarks")
    @ResponseStatus(HttpStatus.OK)
    fun generateBookmarksForAllUsers(): String {
        dummyDataGenerator.generateBookmarksForAllUsers()
        return "✅ 전체 유저 북마크 생성이 완료되었습니다!"
    }
}