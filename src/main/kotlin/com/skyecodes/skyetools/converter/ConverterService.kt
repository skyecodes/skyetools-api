package com.skyecodes.skyetools.converter

import com.skyecodes.skyetools.SSEErrorMessage
import com.skyecodes.skyetools.SSEMessage
import com.skyecodes.skyetools.SSEPassMessage
import com.skyecodes.skyetools.SSEProgressMessage
import com.skyecodes.skyetools.process.ProcessService
import com.skyecodes.skyetools.storage.StorageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream

@Service
class ConverterService(val storageService: StorageService, val processService: ProcessService) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${skyetools.converter.ffmpegPath}")
    private lateinit var ffmpegPath: String
    private val outputPath by lazy { File(storageService.storagePath, "converter").apply { mkdirs() } }

    fun processConvert(file: MultipartFile, ext: String): UUID {
        val inputFile = File(storageService.tempPath, UUID.randomUUID().toString()).absoluteFile.also {
            it.parentFile.mkdirs()
            file.transferTo(it)
        }
        val outputFileName = File(storageService.tempPath, file.resource.filename!!).nameWithoutExtension + ".$ext"
        val params = listOf(ffmpegPath, "-y", "-nostdin", "-i", inputFile.path, outputFileName)
        logger.debug("Executing command: {}", params.joinToString(" "))
        val process = ProcessBuilder(params).directory(outputPath).redirectErrorStream(true).start()
        process.onExit().thenApply {
            logger.debug("Deleting temp file {}", inputFile.name)
            inputFile.delete()
        }
        return processService.register(process)
    }

    fun streamConvertProgress(processId: UUID): Optional<Stream<SSEMessage>> {
        var duration = .0
        var id = 0
        var outputFile: Path? = null
        return processService.get(processId).map {
            it.inputReader().lines().map { line ->
                logger.trace(line)
                var result: SSEMessage = SSEPassMessage
                if (line.startsWith("  Duration: ")) {
                    duration = parseTime(line.substring(12).split(",")[0])
                } else if (line.startsWith("Output #0")) {
                    outputFile = Path.of(outputPath.path, line.split("'")[1])
                } else if (line.contains("time=")) {
                    val time = parseTime(line.substring(line.indexOf("time=") + 5).split(" ")[0])
                    result = SSEProgressMessage(id++, false, (time / duration * 100).coerceAtLeast(.0), null)
                } else if (line.startsWith("video:")) {
                    result =
                        SSEProgressMessage(id++, true, 100.0, storageService.storeAndGetId(outputFile!!, processId))
                } else if (line.lowercase().contains("error")) {
                    result = SSEErrorMessage(id++, line)
                } else if (!it.isAlive) {
                    result = SSEErrorMessage(id++, "Process exited without processing")
                }
                result
            }
        }
    }

    private fun parseTime(time: String) = time.split(":")
        .let { it[0].toInt() * 60 * 60 + it[1].toInt() * 60 + it[2].toDouble() }
}
