package com.skyecodes.skyetools

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.Executors

@Configuration
@EnableWebMvc
@EnableAsync
class WebConfig : WebMvcConfigurer {
    @Value("\${skyetools.frontUrl}")
    private lateinit var frontUrl: String

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(*frontUrl.split(" ", ";").toTypedArray())
            .allowedMethods("*")
    }

    override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
        configurer.setTaskExecutor(getTaskExecutor())
    }

    @Bean
    protected fun getTaskExecutor(): ConcurrentTaskExecutor {
        return ConcurrentTaskExecutor(Executors.newFixedThreadPool(8))
    }
}