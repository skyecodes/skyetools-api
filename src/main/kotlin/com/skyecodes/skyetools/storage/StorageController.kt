package com.skyecodes.skyetools.storage

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.PathResource
import org.springframework.http.*
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
class StorageController(val storageService: StorageService) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/storage/{fileId}")
    @CrossOrigin(exposedHeaders = [HttpHeaders.CONTENT_DISPOSITION])
    fun getStoredFile(@PathVariable fileId: String): ResponseEntity<PathResource> {
        logger.debug("Requested file {}", fileId)
        val path = storageService.getPathFromId(UUID.fromString(fileId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        val resource = PathResource(path)
        val mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM)
        val disposition = ContentDisposition.attachment().filename(resource.filename).build()
        val headers = HttpHeaders().apply {
            contentType = mediaType
            contentDisposition = disposition
        }
        logger.debug("Sending file {}", fileId)
        return ResponseEntity(resource, headers, HttpStatus.OK)
    }
}