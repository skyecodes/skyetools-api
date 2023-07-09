package com.skyecodes.skyetools.process

import com.github.f4b6a3.uuid.UuidCreator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class ProcessService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val processMap = mutableMapOf<UUID, Process>()

    fun register(process: Process): UUID = UuidCreator.getTimeBased().also { processMap[it] = process }

    fun get(processId: UUID): Optional<Process> = Optional.ofNullable(processMap[processId])

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun clearProcess() {
        val lastHour = Instant.now().minus(Duration.ofHours(1))
        processMap.iterator().run {
            while (hasNext()) {
                val process = next()
                val processTimestamp = Date(process.key.timestamp() / 10000L - 12219292800000L).toInstant()
                if (!process.value.isAlive && processTimestamp < lastHour) {
                    remove()
                    logger.info("Process {} cleared", process.key)
                }
            }
        }
    }
}