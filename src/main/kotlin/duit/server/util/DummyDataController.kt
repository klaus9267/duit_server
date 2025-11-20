package duit.server.util

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

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
}