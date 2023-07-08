package com.skyecodes.skyetools

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SkyeToolsApplication

fun main(args: Array<String>) {
    runApplication<SkyeToolsApplication>(*args)
}
