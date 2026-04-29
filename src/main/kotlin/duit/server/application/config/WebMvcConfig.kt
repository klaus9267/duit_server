package duit.server.application.config

import duit.server.application.security.AdminAuthorizationInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val adminAuthorizationInterceptor: AdminAuthorizationInterceptor,
) : WebMvcConfigurer {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(multipartJsonConverter())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 인터셉터는 @RequireAdmin 이 붙은 핸들러에서만 동작 (preHandle 내부에서 분기)
        registry.addInterceptor(adminAuthorizationInterceptor)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:$uploadDir/")
            .setCachePeriod(3600) // 1시간 캐싱
    }

    private fun multipartJsonConverter(): MappingJackson2HttpMessageConverter {
        val converter = MappingJackson2HttpMessageConverter()
        converter.supportedMediaTypes = listOf(
            MediaType.APPLICATION_OCTET_STREAM
        )
        return converter
    }
}
