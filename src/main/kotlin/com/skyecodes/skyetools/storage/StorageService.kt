package com.skyecodes.skyetools.storage

import com.google.common.collect.HashBiMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
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
        val lastHour = Instant.now().minus(Duration.ofHours(1))
        storedFiles.iterator().run {
            while (hasNext()) {
                val file = next()
                val fileTimestamp = Date(file.key.timestamp() / 10000L - 12219292800000L).toInstant()
                if (fileTimestamp < lastHour) {
                    file.value.deleteIfExists()
                    remove()
                    logger.info("File {} cleared ({})", file.key, file.value)
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun clearTempDirAndOtherFiles() {
        logger.info("Clearing temp dir and other files")
        File(tempPath).deleteRecursively()
        Files.walkFileTree(Path.of(storagePath), object : FileVisitor<Path> {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?) = FileVisitResult.CONTINUE

            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                if (!storedFiles.values.map { it.absolutePathString() }.contains(file.absolutePathString()))
                    file.deleteIfExists()
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.CONTINUE

            override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
        })
        logger.info("Temp dir and other files cleared")
    }
}