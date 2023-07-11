package com.skyecodes.skyetools.downloader

import com.skyecodes.skyetools.SSEErrorMessage
import com.skyecodes.skyetools.SSEMessage
import com.skyecodes.skyetools.SSEProgressMessage
import com.skyecodes.skyetools.process.ProcessService
import com.skyecodes.skyetools.storage.StorageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.io.path.exists

@Service
class DownloaderService(val storageService: StorageService, val processService: ProcessService) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${skyetools.downloader.ytdlpPath}")
    private lateinit var ytdlpPath: String

    private val outputPath by lazy { File(storageService.storagePath, "downloader") }

    fun processDownload(url: String, audio: Boolean): UUID {
        val params = getCommandParametersFromRequest(url, audio)
        logger.debug("Executing command: {}", params.joinToString(" "))
        val process = ProcessBuilder(params).directory(outputPath.apply { mkdirs() }).redirectErrorStream(true).start()
        return processService.register(process)
    }

    fun streamDownloadProgress(processId: UUID): Optional<Stream<SSEMessage>> {
        var idx = 0
        var isCompleted = false
        var lastProgress = .0
        var progress: Double
        var fileId: UUID? = null
        return processService.get(processId).map {
            it.inputReader().lines().map { line ->
                logger.trace(line)
                if (line.startsWith("ERROR: ")) {
                    line.substring(7).let { error ->
                        logger.debug("Error while processing {}: {}", processId, error)
                        SSEErrorMessage(idx++, error)
                    }
                } else {
                    if (!isCompleted && line.startsWith("[download]")) {
                        progress = line.split("%")[0].substring(10).trim().toDouble().coerceAtMost(100.0)
                        if (!line.contains("ETA")) {
                            isCompleted = true
                        }
                        logger.trace("Processing of {} progress: {}%", processId, progress)
                    } else {
                        isCompleted = true
                        progress = 100.0
                        if (!line.startsWith("[download]")) {
                            try {
                                val path = Path.of(line)
                                if (path.exists()) {
                                    fileId = storageService.storeAndGetId(path, processId)
                                    logger.debug("Processing of {} completed, fileId={}", processId, fileId)
                                }
                            } catch (e: InvalidPathException) {
                                logger.warn("Ignored output line: {}", line)
                            }
                        }
                    }
                    progress = if (progress < 1) progress else progress.coerceAtLeast(lastProgress)
                    SSEProgressMessage(idx++, isCompleted, progress, fileId).apply { lastProgress = progress }
                }
            }
        }
    }

    private fun getCommandParametersFromRequest(url: String, audio: Boolean): List<String> {
        val params = mutableListOf(
            ytdlpPath,
            "--print",
            "after_move:filepath",
            "--progress",
            "--newline",
            "--restrict-filenames",
            "-k"
        )
        if (audio) params += "-x"
        params += url
        return params
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun updateBinary() {
        logger.info("Updating yt-dlp...")
        ProcessBuilder(ytdlpPath, "-U").redirectErrorStream(true).start().inputReader().lines().forEach { logger.debug(it) }
        logger.info("Updated yt-dlp")
    }
}
