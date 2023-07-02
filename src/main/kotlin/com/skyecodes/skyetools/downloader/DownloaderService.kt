package com.skyecodes.skyetools.downloader

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.io.path.exists

@Service
class DownloaderService {
    private val logger = LoggerFactory.getLogger(DownloaderService::class.java)
    private val processedFiles = ConcurrentHashMap<UUID, Path>()

    @Value("\${skyetools.downloader.ytdlpPath}")
    private lateinit var ytdlpPath: String

    @Value("\${skyetools.downloader.outputPath}")
    private lateinit var outputPath: String

    fun processDownload(req: DownloadRequest): Stream<ProcessMessage> {
        val params = getCommandParametersFromRequest(req)
        logger.debug("Executing command: {}", params.joinToString(" "))
        val process = ProcessBuilder(params).redirectErrorStream(true).start()
        var idx = 0
        var isCompleted = false
        var progress: Double
        var fileId: UUID? = null
        return process.inputReader().lines().map { line ->
            logger.trace(line)
            if (line.startsWith("ERROR: ")) {
                return@map line.substring(7).let { error ->
                    logger.debug("Error while processing {}: {}", req.url, error)
                    ProcessErrorMessage(idx++, error)
                }
            }
            if (!isCompleted && line.startsWith("[download]")) {
                progress = line.split("%")[0].substring(10).trim().toDouble()
                if (!line.contains("ETA")) {
                    isCompleted = true
                }
                logger.trace("Processing of {} progress: {}%", req.url, progress)
            } else {
                isCompleted = true
                progress = 100.0
                if (!line.startsWith("[download]")) {
                    try {
                        val path = Path.of(line)
                        if (path.exists()) {
                            fileId = UUID.randomUUID()
                            registerProcessedFile(fileId!!, path)
                            logger.debug("Processing of {} completed, fileId={}", req.url, fileId)
                        }
                    } catch (e: InvalidPathException) {
                        logger.warn("Ignored output line: {}", line)
                    }
                }
            }
            ProcessProgressMessage(idx++, isCompleted, progress, fileId)
        }
    }

    private fun getCommandParametersFromRequest(req: DownloadRequest): List<String> {
        val params = mutableListOf(
            ytdlpPath,
            "--print",
            "after_move:filepath",
            "--progress",
            "--newline",
            "--force-overwrites",
            "-P",
            outputPath,
            "--restrict-filenames"
        )
        if (!(req.type == DownloadRequest.Type.ALL && req.quality == DownloadRequest.Quality.BEST && req.size == DownloadRequest.Size.NO_LIMIT)) {
            if (req.type == DownloadRequest.Type.AUDIO) {
                params += "-x"
            }
            var format = req.quality.str + req.type.str
            if (req.type != DownloadRequest.Type.ALL && !(req.quality == DownloadRequest.Quality.BEST && req.type == DownloadRequest.Type.AUDIO)) {
                format += "*"
            }
            if (req.size != DownloadRequest.Size.NO_LIMIT) {
                format += "[filesize<${req.size.str}]"
            }
            params += "-f"
            params += "\"$format\""
        }
        if (req.preferFreeFormats) {
            params += "--prefer-free-formats"
        }
        params += req.url
        return params
    }

    fun registerProcessedFile(fileId: UUID, path: Path) {
        processedFiles[fileId] = path
    }

    fun getProcessedFile(fileId: UUID) = Optional.ofNullable(processedFiles[fileId])

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun clearCache() {
        logger.info("Clearing downloader cache...")
        File(outputPath).deleteRecursively()
        processedFiles.clear()
        logger.info("Downloader cache cleared")
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun updateBinary() {
        logger.info("Updating yt-dlp...")
        ProcessBuilder(ytdlpPath, "-U").redirectErrorStream(true).start().inputReader().lines().forEach { logger.debug(it) }
        logger.info("Updated yt-dlp")
    }
}
