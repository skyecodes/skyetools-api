package com.skyecodes.skyetools.storage

import com.github.f4b6a3.uuid.UuidCreator
import com.google.common.collect.HashBiMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists

@Service
class StorageService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val storedFiles = HashBiMap.create<UUID, Path>()

    @Value("\${skyetools.storage.temp}")
    lateinit var tempPath: String

    @Value("\${skyetools.storage.path}")
    lateinit var storagePath: String

    fun storeAndGetId(path: Path, id: UUID): UUID {
        if (storedFiles.containsValue(path)) {
            return storedFiles.inverse()[path]!!
        }
        storedFiles[id] = path
        return id
    }

    fun getPathFromId(fileId: UUID) = Optional.ofNullable(storedFiles[fileId])

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun clearCache() {
        val curTimestamp = UuidCreator.getTimeBased().timestamp()
        val lastHour = curTimestamp - 100 * 1000 * 60 * 60
        storedFiles.iterator().run {
            while (hasNext()) {
                val file = next()
                if (file.key.timestamp() < lastHour) {
                    file.value.deleteIfExists()
                    remove()
                    logger.info("File {} cleared ({})", file.key, file.value)
                }
            }
        }
        File(tempPath).deleteRecursively()
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun clearTempDir() {
        File(tempPath).deleteRecursively()
    }
}