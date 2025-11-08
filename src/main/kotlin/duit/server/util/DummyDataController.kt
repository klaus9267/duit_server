package duit.server.util

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "DummyData", description = "더미 데이터 생성 API")
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