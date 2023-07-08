package com.skyecodes.skyetools

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.*
import java.util.stream.Stream

private val logger: Logger = LoggerFactory.getLogger(SSEMessage::class.java)

sealed interface SSEMessage {
    val id: Int
    val event: String
    fun asString(objectMapper: ObjectMapper): String = objectMapper.writeValueAsString(this)
    fun toEvent(objectMapper: ObjectMapper) = ServerSentEvent.builder<String>()
        .id(id.toString())
        .event(event)
        .data(asString(objectMapper))
        .build()
}

data class SSEProgressMessage(
    @JsonIgnore override val id: Int,
    val isCompleted: Boolean,
    val progress: Double,
    val fileId: UUID?
) : SSEMessage {
    @JsonIgnore
    override val event: String = "progress"
}

data class SSEErrorMessage(override val id: Int, val errorMessage: String) : SSEMessage {
    override val event: String = "error"
    override fun asString(objectMapper: ObjectMapper) = errorMessage
}

data object SSEPassMessage : SSEMessage {
    override val id: Int = 0
    override val event: String = "pass"
    override fun asString(objectMapper: ObjectMapper): String = ""
}

fun streamToFlux(optStream: Optional<Stream<SSEMessage>>, objectMapper: ObjectMapper): Flux<ServerSentEvent<String>> {
    return Flux.fromStream(optStream.orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) })
        .map { it!!.toEvent(objectMapper).apply { logger.trace("Sending {} message: {}", event(), data()) } }
}
