package com.x695c.tuner.data

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Root privilege checker and manager for X695C Vendor Tuner.
 * Handles root access detection and shell command execution.
 */
object RootChecker {

    // Root access status
    private var rootAvailable: Boolean? = null
    private var suBinaryPath: String? = null

    /**
     * Check if root access is available on the device.
     * Caches the result for subsequent calls.
     */
    fun isRootAvailable(): Boolean {
        if (rootAvailable != null) {
            return rootAvailable!!
        }

        rootAvailable = checkRootAccess()
        ActivityLogger.log("RootChecker", "ROOT_CHECK", "Root access: ${if (rootAvailable == true) "AVAILABLE" else "NOT AVAILABLE"}")
        return rootAvailable!!
    }

    /**
     * Request root access and return the result.
     * This will show a popup on rooted devices asking for permission.
     */
    fun requestRootAccess(): Boolean {
        if (rootAvailable == true) {
            return true
        }

        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)

            // Simple command to test root access
            outputStream.writeBytes("id\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val exitCode = process.waitFor()
            rootAvailable = exitCode == 0

            if (rootAvailable == true) {
                ActivityLogger.log("RootChecker", "ROOT_GRANTED", "Root access granted by user")
            } else {
                ActivityLogger.log("RootChecker", "ROOT_DENIED", "Root access denied or not available")
            }

            rootAvailable!!
        } catch (e: Exception) {
            ActivityLogger.logError("RootChecker", "Failed to request root: ${e.message}")
            rootAvailable = false
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
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))

            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val output = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

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
     */
    private fun checkRootAccess(): Boolean {
        // Check for su binary in common locations
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/vendor/bin/su",
            "/su/bin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/magisk/.core/bin/su"
        )

        for (path in suPaths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.canExecute()) {
                    suBinaryPath = path
                    return true
                }
            } catch (e: Exception) {
                // Ignore permission errors
            }
        }

        // Try to execute su directly
        return try {
            val process = Runtime.getRuntime().exec("su")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get the path to the su binary if found.
     */
    fun getSuBinaryPath(): String? = suBinaryPath

    /**
     * Reset the cached root status (for re-checking).
     */
    fun resetRootStatus() {
        rootAvailable = null
        suBinaryPath = null
    }

    /**
     * Check if the app has been granted root access already.
     * Returns null if not checked yet.
     */
    fun getRootStatus(): Boolean? = rootAvailable
}
