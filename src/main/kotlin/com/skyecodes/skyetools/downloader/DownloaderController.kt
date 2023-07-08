package com.skyecodes.skyetools.downloader

import com.fasterxml.jackson.databind.ObjectMapper
import com.skyecodes.skyetools.streamToFlux
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.*

@RestController
class DownloaderController(val downloaderService: DownloaderService, val objectMapper: ObjectMapper) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/downloader/process")
    fun process(@RequestBody data: DownloadRequest): UUID {
        logger.debug("Processing URL {} for download", data.url)
        return downloaderService.processDownload(data)
    }

    @GetMapping("/downloader/{processId}/progress")
    fun getProgress(@PathVariable processId: UUID): Flux<ServerSentEvent<String>> {
        logger.debug("Streaming progress of process {}", processId)
        return streamToFlux(downloaderService.streamDownloadProgress(processId), objectMapper)
    }
}