package com.elitedarkkaiser.redmagic

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable

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

    class Session internal constructor(
        internal val process: Process,
        internal val writer: BufferedWriter,
        internal val reader: BufferedReader,
        internal val token: String
    ) : Closeable {
        @Volatile
        internal var closed = false

        internal var commandId = 0L

        val isAlive: Boolean
            get() = !closed && process.isAlive

        fun exec(command: String): Boolean {
            return RootShell.runSessionCommand(this, command)
        }

        override fun close() {
            RootShell.closeSession(this)
        }
    }

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

    fun openSession(): Session? {
        return synchronized(commandLock) {
            try {
                val process = ProcessBuilder("su")
                    .redirectErrorStream(true)
                    .start()
                val session = Session(
                    process = process,
                    writer = process.outputStream.bufferedWriter(),
                    reader = process.inputStream.bufferedReader(),
                    token = java.lang.Long.toHexString(
                        android.os.SystemClock.elapsedRealtimeNanos()
                    )
                )

                if (!runSessionCommandLocked(
                        session,
                        "id -u | grep -qx 0"
                    )
                ) {
                    closeSessionLocked(session)
                    null
                } else {
                    session
                }
            } catch (error: Exception) {
                Log.e(
                    TAG,
                    "Unable to open persistent root session",
                    error
                )
                null
            }
        }
    }

    private fun runSessionCommand(
        session: Session,
        command: String
    ): Boolean {
        return synchronized(commandLock) {
            runSessionCommandLocked(session, command)
        }
    }

    private fun runSessionCommandLocked(
        session: Session,
        command: String
    ): Boolean {
        if (!session.isAlive) return false

        return try {
            session.commandId += 1
            val marker =
                "__REDMAGIC_ROOT_${session.token}_${session.commandId}__"

            session.writer.write(command)
            session.writer.newLine()
            session.writer.write(
                "redmagic_status=${'$'}?; " +
                    "printf '$marker:%s\\n' " +
                    "\"${'$'}redmagic_status\""
            )
            session.writer.newLine()
            session.writer.flush()

            var exitCode: Int? = null

            while (exitCode == null) {
                val line = session.reader.readLine()
                    ?: throw IllegalStateException(
                        "Persistent root shell closed unexpectedly"
                    )

                if (line.startsWith("$marker:")) {
                    exitCode = line
                        .substringAfter(':')
                        .trim()
                        .toIntOrNull()
                        ?: -1
                }
            }

            if (exitCode != 0) {
                Log.w(
                    TAG,
                    "Persistent root command failed with " +
                        "exit code $exitCode"
                )
            }

            exitCode == 0
        } catch (error: Exception) {
            Log.e(TAG, "Persistent root command failed", error)
            closeSessionLocked(session)
            false
        }
    }

    private fun closeSession(session: Session) {
        synchronized(commandLock) {
            closeSessionLocked(session)
        }
    }

    private fun closeSessionLocked(session: Session) {
        if (session.closed) return

        session.closed = true

        runCatching {
            session.writer.write("exit")
            session.writer.newLine()
            session.writer.flush()
        }

        runCatching { session.writer.close() }
        runCatching { session.reader.close() }

        if (session.process.isAlive) {
            session.process.destroy()
        }
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
