package com.skyecodes.skyetools.downloader

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
class DownloaderController(val downloaderService: DownloaderService) {
    private val logger = LoggerFactory.getLogger(DownloaderController::class.java)

    @PostMapping("/downloader/download")
    @CrossOrigin(exposedHeaders = [HttpHeaders.CONTENT_DISPOSITION])
    fun download(@RequestBody req: DownloadRequest): ResponseEntity<*> {
        logger.debug("Downloading video from {}", req.url)
        val path = downloaderService.downloadFromUrl(req).orElseThrow { ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val file = FileSystemResource(path)
        val mediaType = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM)
        val disposition = ContentDisposition.attachment().filename(file.filename).build()
        val headers = HttpHeaders().apply {
            contentType = mediaType
            contentDisposition = disposition
        }
        logger.debug("Finished downloading video from {}", req.url)
        return ResponseEntity(file, headers, HttpStatus.OK)
    }
}