package duit.server.infrastructure.external.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    @Value("\${file.base-url}")
    private lateinit var baseUrl: String

    fun uploadFile(file: MultipartFile, folder: String): String = runCatching {
        // 1. 파일 검증
        validateFile(file)

        // 2. 폴더명 검증 (Path Traversal 방지)
        if (folder.contains("..") || folder.contains("/") || folder.contains("\\")) {
            throw IllegalArgumentException("잘못된 폴더명입니다")
        }

        val folderPath = Paths.get(uploadDir, folder)
        Files.createDirectories(folderPath)

        val fileName = generateUniqueFileName(file.originalFilename ?: "file")
        val targetPath = folderPath.resolve(fileName)

        // 3. 최종 경로 검증 (uploadDir 내부인지 확인)
        if (!targetPath.normalize().startsWith(Paths.get(uploadDir).normalize())) {
            throw IllegalArgumentException("잘못된 파일 경로입니다")
        }

        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        "$baseUrl/uploads/$folder/$fileName"
    }.getOrElse { throw RuntimeException("파일 업로드 실패: ${it.message}", it) }

    private fun validateFile(file: MultipartFile) {
        // 파일 크기 검증 (10MB 제한)
        if (file.size > 10 * 1024 * 1024) {
            throw IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다")
        }

        // 파일 확장자 검증 (이미지만 허용)
        val allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "webp")
        val extension = file.originalFilename
            ?.substringAfterLast(".", "")
            ?.lowercase() ?: ""

        if (extension !in allowedExtensions) {
            throw IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)")
        }

        // Content-Type 검증
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) {
            throw IllegalArgumentException("이미지 파일만 업로드 가능합니다")
        }
    }

    fun deleteFile(fileUrl: String) = runCatching {
        val relativePath = fileUrl.substringAfter("/uploads/")
        val filePath = Paths.get(uploadDir, relativePath)
        Files.deleteIfExists(filePath)
    }.getOrElse { throw RuntimeException("파일 삭제 실패: ${it.message}", it) }

    private fun generateUniqueFileName(originalFilename: String): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = originalFilename.substringAfterLast(".", "")
        val nameWithoutExtension = originalFilename.substringBeforeLast(".")

        return if (extension.isNotEmpty()) {
            "${timestamp}_${uuid}_${nameWithoutExtension}.$extension"
        } else {
            "${timestamp}_${uuid}_${originalFilename}"
        }
    }
}
