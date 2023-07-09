package com.skyecodes.skyetools.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.skyecodes.skyetools.streamToFlux
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.util.*

@RestController
class ConverterController(val converterService: ConverterService, val objectMapper: ObjectMapper) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/converter/process")
    fun process(file: MultipartFile, ext: String): UUID {
        logger.debug("Processing file {} for conversion", file.resource.filename)
        return converterService.processConvert(file, ext)
    }

    @GetMapping("/converter/{processId}/progress")
    fun getProgress(@PathVariable processId: UUID): Flux<ServerSentEvent<String>> {
        logger.debug("Streaming progress of process {}", processId)
        return streamToFlux(converterService.streamConvertProgress(processId), objectMapper)
    }
}