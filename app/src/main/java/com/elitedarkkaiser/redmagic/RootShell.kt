package com.elitedarkkaiser.redmagic

import android.util.Log

object RootShell {

    private const val TAG = "RedmagicRootShell"

    /*
     * Root commands can originate from multiple services. Serializing them
     * prevents overlapping sysfs writes and keeps their results deterministic.
     */
    private val commandLock = Any()

    private data class CommandResult(
        val exitCode: Int,
        val output: String
    )

    fun hasRoot(): Boolean {
        val output = execForOutput("id")
        return output?.contains("uid=0") == true
    }

    fun exec(command: String): Boolean {
        return runCommand(command)?.exitCode == 0
    }

    fun execForOutput(command: String): String? {
        val result = runCommand(command) ?: return null
        if (result.exitCode != 0) return null
        return result.output.ifEmpty { null }
    }

    private fun runCommand(command: String): CommandResult? {
        return synchronized(commandLock) {
            try {
                val process = ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start()

                /*
                 * Drain output before waitFor(). A command producing enough
                 * output could otherwise block while waiting for the pipe.
                 */
                val output = process.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                    .trim()

                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    Log.w(TAG, "Root command failed with exit code $exitCode")
                }

                CommandResult(
                    exitCode = exitCode,
                    output = output
                )
            } catch (error: Exception) {
                Log.e(TAG, "Root command execution failed", error)
                null
            }
        }
    }
}
