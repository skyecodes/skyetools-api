package com.skyecodes.skyetools.downloader

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class DownloaderService {
    private val logger = LoggerFactory.getLogger(DownloaderService::class.java)

    @Value("\${skyetools.downloader.ytdlpPath}")
    private lateinit var ytdlpPath: String

    @Value("\${skyetools.downloader.outputPath}")
    private lateinit var outputPath: String

    fun downloadFromUrl(req: DownloadRequest): Optional<Path> {
        val params = mutableListOf(ytdlpPath, "--print", "after_move:filepath", "--force-overwrites", "-P", outputPath, "--restrict-filenames")
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
        logger.debug("Executing command: {}", params.joinToString(" "))
        val process = ProcessBuilder(params).start()
        val errorLines = process.errorReader().lines().toList()
        if (errorLines.isNotEmpty()) handleError(req, errorLines)
        return process.inputReader().lines().findFirst().map(Path::of)
    }

    private fun handleError(req: DownloadRequest, errorLines: List<String>) {
        logger.error("Error while processing {}", req.url)
        errorLines.forEach { logger.debug(it) }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun clearCache() {
        logger.info("Clearing downloader cache...")
        File(outputPath).deleteRecursively()
        logger.info("Downloader cache cleared")
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun updateBinary() {
        logger.info("Updating yt-dlp...")
        ProcessBuilder(ytdlpPath, "-U").redirectErrorStream(true).start().inputReader().lines().forEach { logger.debug(it) }
        logger.info("Updated yt-dlp")
    }
}
