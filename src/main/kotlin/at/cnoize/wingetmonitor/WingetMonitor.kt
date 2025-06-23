package at.cnoize.wingetmonitor

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Winget Update Monitor
 * 
 * A utility to check for available Windows program updates using winget.
 */
fun main() {
    val logger = Logger()
    val timestamp = LocalDateTime.now().atZone(ZoneOffset.UTC)
    val timestampStr = timestamp.format(DateTimeFormatter.ISO_INSTANT)

    logger.log("Starting Winget Update Monitor at $timestampStr")

    try {
        val wingetOutput = runWingetCommand(logger)
        val updates = parseWingetOutput(wingetOutput)
        val jsonOutput = createJsonOutput(timestampStr, updates)
        writeOutputFile(jsonOutput)

        logger.log("Found ${updates.size} updates")
        logger.log("Finished Winget Update Monitor")

        System.exit(0)
    } catch (e: WingetNotInstalledException) {
        logger.log("Error: Winget is not installed or failed to run: ${e.message}")

        val emptyOutput = createJsonOutput(timestampStr, emptyList())
        writeOutputFile(emptyOutput)

        System.exit(0)
    } catch (e: Exception) {
        logger.log("Internal error: ${e.message}")
        e.printStackTrace()

        System.exit(1)
    }
}

/**
 * Runs the winget upgrade command and returns its output.
 * 
 * @param logger The logger instance to log messages
 * @throws WingetNotInstalledException if winget is not installed or fails to run
 */
fun runWingetCommand(logger: Logger): String {
    try {
        val wingetCommand = "winget upgrade --accept-source-agreements --accept-package-agreements"
        logger.log("Executing command: $wingetCommand")

        val process = ProcessBuilder(
            "powershell.exe", "-Command", 
            "&{$wingetCommand}"
        ).redirectErrorStream(true).start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw WingetNotInstalledException("Winget command failed with exit code $exitCode")
        }

        return output.toString()
    } catch (e: Exception) {
        throw WingetNotInstalledException("Failed to run winget: ${e.message}")
    }
}

/**
 * Parses the winget output to extract updatable programs.
 */
fun parseWingetOutput(output: String): List<Update> {
    val updates = mutableListOf<Update>()
    val lines = output.lines()

    if (output.contains("No available upgrades") || output.contains("No updates available")) {
        return emptyList()
    }

    val headerIndex = lines.indexOfFirst { 
        it.contains("Name") && it.contains("Version") && it.contains("Available") 
    }

    if (headerIndex == -1) {
        return emptyList()
    }

    val separatorIndex = headerIndex + 1
    if (separatorIndex >= lines.size || !lines[separatorIndex].contains("---")) {
        return emptyList()
    }

    val headerLine = lines[headerIndex]
    val nameIndex = headerLine.indexOf("Name")
    val idIndex = headerLine.indexOf("Id")
    val versionIndex = headerLine.indexOf("Version")
    val availableIndex = headerLine.indexOf("Available")
    val sourceIndex = headerLine.indexOf("Source")

    for (i in (separatorIndex + 1) until lines.size) {
        val line = lines[i]

        if (line.isBlank() || line.startsWith("No") || line.contains("upgrades available")) {
            continue
        }

        if (line.startsWith("--") || line.startsWith("The") || line.startsWith("Found")) {
            break
        }

        try {
            if (line.length < availableIndex) {
                continue
            }

            val name = if (nameIndex >= 0 && idIndex > nameIndex) {
                line.substring(nameIndex, idIndex).trim()
            } else {
                continue
            }

            val version = if (availableIndex > versionIndex && versionIndex >= 0) {
                val versionEnd = if (sourceIndex > availableIndex) {
                    sourceIndex
                } else {
                    line.length
                }

                if (availableIndex < line.length) {
                    line.substring(availableIndex, minOf(versionEnd, line.length)).trim()
                } else {
                    continue
                }
            } else {
                continue
            }

            val source = if (sourceIndex >= 0 && sourceIndex < line.length) {
                line.substring(sourceIndex).trim()
            } else {
                "winget"
            }

            updates.add(Update(name, version, source))
        } catch (e: Exception) {
            continue
        }
    }

    return updates
}

/**
 * Creates a JSON string from the updates list.
 */
fun createJsonOutput(timestamp: String, updates: List<Update>): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("  \"timestamp\": \"$timestamp\",\n")
    sb.append("  \"updateCount\": ${updates.size},\n")
    sb.append("  \"updates\": [\n")

    updates.forEachIndexed { index, update ->
        sb.append("    {\n")
        sb.append("      \"name\": \"${escapeJson(update.name)}\",\n")
        sb.append("      \"version\": \"${escapeJson(update.version)}\",\n")
        sb.append("      \"source\": \"${escapeJson(update.source)}\"\n")
        sb.append("    }")
        if (index < updates.size - 1) {
            sb.append(",")
        }
        sb.append("\n")
    }

    sb.append("  ]\n")
    sb.append("}")

    return sb.toString()
}

/**
 * Escapes special characters in JSON strings.
 */
fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
             .replace("\"", "\\\"")
             .replace("\b", "\\b")
             .replace("\n", "\\n")
             .replace("\r", "\\r")
             .replace("\t", "\\t")
}

/**
 * Writes the JSON output to the output file.
 */
fun writeOutputFile(content: String) {
    val userHome = System.getProperty("user.home")
    val outputFile = Paths.get(userHome, ".winget-monitor")

    Files.writeString(
        outputFile,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    )
}

/**
 * Data class representing an update.
 */
data class Update(
    val name: String,
    val version: String,
    val source: String
)

/**
 * Exception thrown when winget is not installed or fails to run.
 */
class WingetNotInstalledException(message: String) : Exception(message)

/**
 * Logger class for handling log messages.
 */
class Logger {
    private val logFile = Paths.get(System.getProperty("user.home"), ".winget-monitor.log")

    /**
     * Logs a message to the log file.
     */
    fun log(message: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val logMessage = "[$timestamp] $message\n"

        System.out.print(logMessage)

        try {
            if (!Files.exists(logFile)) {
                Files.createFile(logFile)
            }

            Files.write(
                logFile,
                logMessage.toByteArray(),
                StandardOpenOption.APPEND
            )
        } catch (e: Exception) {
            System.err.println("Failed to write to log file: ${e.message}")
            System.err.println(logMessage)
        }
    }
}
