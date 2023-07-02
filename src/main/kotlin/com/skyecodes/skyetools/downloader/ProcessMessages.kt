package com.skyecodes.skyetools.downloader

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

sealed interface ProcessMessage {
    val id: Int
    val event: String
    fun asString(objectMapper: ObjectMapper): String = objectMapper.writeValueAsString(this)
}

data class ProcessProgressMessage(
    @JsonIgnore override val id: Int,
    val isCompleted: Boolean,
    val progress: Double,
    val fileId: UUID?
) : ProcessMessage {
    @JsonIgnore
    override val event: String = "progress"
}

data class ProcessErrorMessage(override val id: Int, val errorMessage: String) : ProcessMessage {
    override val event: String = "error"
    override fun asString(objectMapper: ObjectMapper) = errorMessage
}