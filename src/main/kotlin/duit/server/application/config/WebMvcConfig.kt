package duit.server.application.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {

    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(multipartJsonConverter())
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:$uploadDir/")
            .setCachePeriod(3600) // 1시간 캐싱
            .resourceChain(true)
    }

    private fun multipartJsonConverter(): MappingJackson2HttpMessageConverter {
        val converter = MappingJackson2HttpMessageConverter()
        converter.supportedMediaTypes = listOf(
            MediaType.APPLICATION_OCTET_STREAM
        )
        return converter
    }
}
