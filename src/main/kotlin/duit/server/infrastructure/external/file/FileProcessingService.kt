//package duit.server.infrastructure.external.file
//
//import duit.server.application.dto.webhook.FileInfo
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.stereotype.Service
//import java.io.File
//import java.nio.file.Files
//import java.nio.file.Paths
//import java.util.*
//
///**
// * 파일 처리 결과
// */
//data class FileProcessingResult(
//    val success: Boolean,
//    val originalPath: String? = null,
//    val thumbnailPath: String? = null,
//    val message: String? = null
//)
//
///**
// * 파일 처리 서비스
// * 외부 파일 시스템과의 연동이므로 infrastructure 레이어에 배치
// */
//@Service
//class FileProcessingService {
//
//    private val logger = LoggerFactory.getLogger(FileProcessingService::class.java)
//
//    @Value("\${app.upload.path:uploads}")
//    private lateinit var uploadPath: String
//
//    /**
//     * 파일 타입에 따라 적절한 처리 수행
//     */
//    fun processFileByType(fileInfo: FileInfo): FileProcessingResult {
//        return try {
//            when {
//                isImageFile(fileInfo.fileName) -> processImageFile(fileInfo)
//                isDocumentFile(fileInfo.fileName) -> processDocumentFile(fileInfo)
//                else -> processGenericFile(fileInfo)
//            }
//        } catch (e: Exception) {
//            logger.error("❌ 파일 처리 실패: ${fileInfo.fileName}", e)
//            FileProcessingResult(
//                success = false,
//                message = "파일 처리 중 오류 발생: ${e.message}"
//            )
//        }
//    }
//
//    /**
//     * 이미지 파일 처리 (로고, 썸네일 등)
//     */
//    private fun processImageFile(fileInfo: FileInfo): FileProcessingResult {
//        logger.info("🖼️ 이미지 파일 처리: ${fileInfo.fileName}")
//
//        val savedPath = saveFile(fileInfo, "images")
//
//        // 필요시 썸네일 생성 로직 추가
//        // val thumbnailPath = createThumbnail(savedPath)
//
//        return FileProcessingResult(
//            success = true,
//            originalPath = savedPath,
//            message = "이미지 파일 저장 완료"
//        )
//    }
//
//    /**
//     * 문서 파일 처리
//     */
//    private fun processDocumentFile(fileInfo: FileInfo): FileProcessingResult {
//        logger.info("📄 문서 파일 처리: ${fileInfo.fileName}")
//
//        val savedPath = saveFile(fileInfo, "documents")
//
//        return FileProcessingResult(
//            success = true,
//            originalPath = savedPath,
//            message = "문서 파일 저장 완료"
//        )
//    }
//
//    /**
//     * 일반 파일 처리
//     */
//    private fun processGenericFile(fileInfo: FileInfo): FileProcessingResult {
//        logger.info("📎 일반 파일 처리: ${fileInfo.fileName}")
//
//        val savedPath = saveFile(fileInfo, "files")
//
//        return FileProcessingResult(
//            success = true,
//            originalPath = savedPath,
//            message = "파일 저장 완료"
//        )
//    }
//
//    /**
//     * 파일 저장
//     */
//    private fun saveFile(fileInfo: FileInfo, subDirectory: String): String {
//        val targetDir = Paths.get(uploadPath, subDirectory)
//        Files.createDirectories(targetDir)
//
//        // 고유한 파일명 생성
//        val uniqueFileName = generateUniqueFileName(fileInfo.fileName)
//        val targetPath = targetDir.resolve(uniqueFileName)
//
//        // Base64 디코딩 후 파일 저장
//        val decodedBytes = Base64.getDecoder().decode(fileInfo.content)
//        Files.write(targetPath, decodedBytes)
//
//        logger.info("💾 파일 저장 완료: $targetPath")
//        return targetPath.toString()
//    }
//
//    /**
//     * 고유한 파일명 생성
//     */
//    private fun generateUniqueFileName(originalFileName: String): String {
//        val timestamp = System.currentTimeMillis()
//        val extension = File(originalFileName).extension
//        val nameWithoutExtension = File(originalFileName).nameWithoutExtension
//
//        return "${nameWithoutExtension}_${timestamp}.$extension"
//    }
//
//    /**
//     * 이미지 파일 여부 확인
//     */
//    private fun isImageFile(fileName: String): Boolean {
//        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
//        val extension = File(fileName).extension.lowercase()
//        return extension in imageExtensions
//    }
//
//    /**
//     * 문서 파일 여부 확인
//     */
//    private fun isDocumentFile(fileName: String): Boolean {
//        val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
//        val extension = File(fileName).extension.lowercase()
//        return extension in documentExtensions
//    }
//}
