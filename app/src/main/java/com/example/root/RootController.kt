package com.example.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ShellResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val isRoot: Boolean,
    val executionTimeMs: Long
)

class RootController {

    private val suBinaryPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    fun isRootBinaryPresent(): Boolean {
        for (path in suBinaryPaths) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val completed = process.waitFor(4, TimeUnit.SECONDS)
            if (completed && process.exitValue() == 0 && line != null && line.contains("uid=0")) {
                return@withContext true
            }
        } catch (_: Exception) {
        }
        false
    }

    fun isDangerousCommand(command: String): Boolean {
        val normalized = command.trim().lowercase()
        val dangerousTokens = listOf(
            "rm -rf /",
            "rm -r /",
            "dd if=",
            "mkfs",
            "format",
            "wipe",
            "reboot",
            "shutdown",
            "poweroff",
            "setenforce 0",
            "remount,rw",
            "flash_image",
            "> /dev/block"
        )
        return dangerousTokens.any { normalized.contains(it) }
    }

    suspend fun executeCommand(command: String, requireRoot: Boolean = false): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var process: Process? = null
        try {
            val cmdArray = if (requireRoot) {
                arrayOf("su", "-c", command)
            } else {
                arrayOf("sh", "-c", command)
            }

            process = Runtime.getRuntime().exec(cmdArray)

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stdoutBuilder.appendLine(line)
                        }
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stderrBuilder.appendLine(line)
                        }
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(10, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                return@withContext ShellResult(
                    exitCode = -1,
                    output = stdoutBuilder.toString().trim(),
                    error = "Command timed out after 10 seconds",
                    isRoot = requireRoot,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val exitCode = process.exitValue()
            ShellResult(
                exitCode = exitCode,
                output = stdoutBuilder.toString().trim(),
                error = stderrBuilder.toString().trim(),
                isRoot = requireRoot,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                output = "",
                error = e.localizedMessage ?: "Execution error",
                isRoot = requireRoot,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } finally {
            process?.destroy()
        }
    }
}
