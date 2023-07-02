package com.skyecodes.skyetools.downloader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.*


@RestController
class DownloaderController(val downloaderService: DownloaderService, val objectMapper: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(DownloaderController::class.java)

    @GetMapping("/downloader/process")
    fun process(data: String): Flux<ServerSentEvent<String>> {
        val req = objectMapper.readValue<DownloadRequest>(data)
        logger.debug("Processing video from {}", req.url)
        val outStream = downloaderService.processDownload(req)
        return Flux.fromStream(outStream).map {
            ServerSentEvent.builder<String>()
                .id(it.id.toString())
                .event(it.event)
                .data(it.asString(objectMapper))
                .build()
                .apply { logger.trace("Sending {} message: {}", event(), data()) }
        }
    }

    @GetMapping("/downloader/download")
    @CrossOrigin(exposedHeaders = [HttpHeaders.CONTENT_DISPOSITION])
    fun download(fileId: String): ResponseEntity<*> {
        logger.debug("Requested file {}", fileId)
        val path = downloaderService.getProcessedFile(UUID.fromString(fileId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        val file = FileSystemResource(path)
        val mediaType = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM)
        val disposition = ContentDisposition.attachment().filename(file.filename).build()
        val headers = HttpHeaders().apply {
            contentType = mediaType
            contentDisposition = disposition
        }
        logger.debug("Sending file {}", fileId)
        return ResponseEntity(file, headers, HttpStatus.OK)
    }
}