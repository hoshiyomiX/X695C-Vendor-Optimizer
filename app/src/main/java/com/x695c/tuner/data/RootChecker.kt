package com.x695c.tuner.data

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Root privilege checker and manager for X695C Vendor Tuner.
 * Handles root access detection and shell command execution.
 *
 * Thread-safe: uses AtomicBoolean/AtomicReference for cached state.
 * Process stream consumption prevents deadlocks on all root operations.
 */
object RootChecker {

    // Thread-safe state with atomic references
    private val rootAvailable = AtomicBoolean(false)
    private val suBinaryPath = AtomicReference<String?>(null)
    private val rootCheckAttempted = AtomicBoolean(false)

    /**
     * Check if root access is available on the device.
     * Caches result after first check. Call resetRootStatus() to re-check.
     */
    fun isRootAvailable(): Boolean {
        if (rootCheckAttempted.get()) {
            return rootAvailable.get()
        }
        val result = checkRootAccess()
        rootAvailable.set(result)
        rootCheckAttempted.set(true)
        ActivityLogger.log("RootChecker", "ROOT_CHECK", "Root access: ${if (result) "AVAILABLE" else "NOT AVAILABLE"}")
        return result
    }

    /**
     * Request root access and return the result.
     * Always performs a fresh check regardless of cache.
     * Also scans for su binary path for state tracking (FLOW-H006).
     */
    fun requestRootAccess(): Boolean {
        // FLOW-H006: Scan for su binary path before attempting grant
        scanForSuBinary()
        return try {
            val processBuilder = ProcessBuilder("su")
            val process = processBuilder.start()
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            // Drain streams on background threads to prevent deadlock
            val stdoutThread = Thread {
                try { BufferedReader(InputStreamReader(process.inputStream)).readText() } catch (_: Exception) {}
            }
            val stderrThread = Thread {
                try { BufferedReader(InputStreamReader(process.errorStream)).readText() } catch (_: Exception) {}
            }
            stdoutThread.start()
            stderrThread.start()
            stdoutThread.join(15000)
            stderrThread.join(15000)

            val exitCode = process.waitFor()
            val granted = exitCode == 0
            rootAvailable.set(granted)
            rootCheckAttempted.set(true)

            if (granted) {
                ActivityLogger.log("RootChecker", "ROOT_GRANTED", "Root access granted by user")
            } else {
                ActivityLogger.log("RootChecker", "ROOT_DENIED", "Root access denied or not available")
            }
            granted
        } catch (e: Exception) {
            ActivityLogger.logError("RootChecker", "Failed to request root: ${e.message}")
            rootAvailable.set(false)
            rootCheckAttempted.set(true)
            false
        }
    }

    /**
     * Execute a command with root privileges.
     * Returns the command output or null if failed.
     */
    fun executeWithRoot(command: String): String? {
        if (!isRootAvailable()) {
            ActivityLogger.logError("RootChecker", "Root not available for command execution")
            return null
        }
        return try {
            val processBuilder = ProcessBuilder("su")
            val process = processBuilder.start()
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            // Drain stderr on background thread
            val stderrThread = Thread {
                try { BufferedReader(InputStreamReader(process.errorStream)).readText() } catch (_: Exception) {}
            }
            stderrThread.start()

            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            reader.close()
            stderrThread.join(15000)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                ActivityLogger.log("RootChecker", "EXECUTE_SUCCESS", "Command executed successfully")
                output.toString()
            } else {
                ActivityLogger.logError("RootChecker", "Command failed with exit code: $exitCode")
                null
            }
        } catch (e: Exception) {
            ActivityLogger.logError("RootChecker", "Failed to execute command: ${e.message}")
            null
        }
    }

    /**
     * Check for common root binaries and indicators.
     * Does NOT return true just because su binary exists;
     * always verifies actual execution to confirm user granted permission.
     * Drains process streams to prevent deadlock.
     */
    private fun checkRootAccess(): Boolean {
        // FLOW-H006: Scan for su binary first (extracted to reusable method)
        scanForSuBinary()

        // Actually execute su and verify it works (user has granted permission)
        return try {
            val process = ProcessBuilder("su").start()
            val stderrThread = Thread {
                try { BufferedReader(InputStreamReader(process.errorStream)).readText() } catch (_: Exception) {}
            }
            val stdoutThread = Thread {
                try { BufferedReader(InputStreamReader(process.inputStream)).readText() } catch (_: Exception) {}
            }
            stdoutThread.start()
            stderrThread.start()
            process.outputStream.close()
            stdoutThread.join(10000)
            stderrThread.join(10000)
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Scan filesystem for su binary. Sets suBinaryPath if found.
     * FLOW-H006: Extracted from checkRootAccess() for reuse in requestRootAccess().
     */
    private fun scanForSuBinary() {
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su",
            "/vendor/bin/su", "/su/bin/su", "/data/local/xbin/su",
            "/data/local/bin/su", "/magisk/.core/bin/su"
        )
        for (path in suPaths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.canExecute()) {
                    suBinaryPath.set(path)
                    break  // Found su binary, stop scanning
                }
            } catch (_: Exception) {}
        }
    }

    fun getSuBinaryPath(): String? = suBinaryPath.get()

    /**
     * Reset cached root status to allow re-checking.
     * Should be called when user grants root after initial denial.
     */
    fun resetRootStatus() {
        rootAvailable.set(false)
        suBinaryPath.set(null)
        rootCheckAttempted.set(false)
        ActivityLogger.log("RootChecker", "RESET", "Root status cache cleared")
    }

    /** Whether the su binary was detected on the filesystem (may not have user grant yet). */
    fun isSuBinaryDetected(): Boolean = rootCheckAttempted.get() && suBinaryPath.get() != null
}
