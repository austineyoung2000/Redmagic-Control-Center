package com.elitedarkkaiser.redmagic

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable

object RootShell {
    private const val TAG = "RedmagicRootShell"

    /*
     * All command-style root work shares one serialized shell.
     * Blocking input readers remain separate because they must
     * continuously consume independent device streams.
     */
    private val commandLock = Any()

    @Volatile
    private var sharedSession: Session? = null

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
            return RootShell.runSessionCommand(
                this,
                command
            )?.exitCode == 0
        }

        fun execForOutput(command: String): String? {
            val result =
                RootShell.runSessionCommand(
                    this,
                    command
                ) ?: return null

            if (result.exitCode != 0) {
                return null
            }

            return result.output.ifEmpty { null }
        }

        override fun close() {
            RootShell.closeSession(this)
        }
    }

    fun hasRoot(): Boolean {
        return execForOutput("id -u")
            ?.trim() == "0"
    }

    fun exec(command: String): Boolean {
        return runSharedCommand(command)
            ?.exitCode == 0
    }

    fun execForOutput(command: String): String? {
        val result =
            runSharedCommand(command)
                ?: return null

        if (result.exitCode != 0) {
            return null
        }

        return result.output.ifEmpty { null }
    }

    /*
     * Dedicated sessions remain available for operations that
     * explicitly need ownership of a shell. Normal application
     * commands should use exec()/execForOutput() instead.
     */
    fun openSession(): Session? {
        return synchronized(commandLock) {
            openSessionLocked()
        }
    }

    fun closeSharedSession() {
        synchronized(commandLock) {
            sharedSession?.let {
                closeSessionLocked(it)
            }
            sharedSession = null
        }
    }

    private fun runSharedCommand(
        command: String
    ): CommandResult? {
        return synchronized(commandLock) {
            val session = activeSharedSessionLocked()

            if (session != null) {
                val result =
                    runSessionCommandLocked(
                        session,
                        command
                    )

                if (result != null) {
                    return@synchronized result
                }
            }

            /*
             * Preserve compatibility if a root implementation
             * refuses an interactive shell.
             */
            runOneShotCommandLocked(command)
        }
    }

    private fun activeSharedSessionLocked(): Session? {
        val current = sharedSession

        if (current?.isAlive == true) {
            return current
        }

        current?.let {
            closeSessionLocked(it)
        }

        return openSessionLocked().also {
            sharedSession = it
        }
    }

    private fun openSessionLocked(): Session? {
        return try {
            val process = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()

            val session = Session(
                process = process,
                writer =
                    process.outputStream.bufferedWriter(),
                reader =
                    process.inputStream.bufferedReader(),
                token = java.lang.Long.toHexString(
                    android.os.SystemClock
                        .elapsedRealtimeNanos()
                )
            )

            val rootCheck =
                runSessionCommandLocked(
                    session,
                    "id -u"
                )

            if (
                rootCheck == null ||
                rootCheck.exitCode != 0 ||
                rootCheck.output.trim() != "0"
            ) {
                closeSessionLocked(session)
                null
            } else {
                Log.i(
                    TAG,
                    "Persistent root command shell opened"
                )
                session
            }
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Unable to open persistent root shell",
                error
            )
            null
        }
    }

    private fun runSessionCommand(
        session: Session,
        command: String
    ): CommandResult? {
        return synchronized(commandLock) {
            runSessionCommandLocked(
                session,
                command
            )
        }
    }

    private fun runSessionCommandLocked(
        session: Session,
        command: String
    ): CommandResult? {
        if (!session.isAlive) {
            return null
        }

        return try {
            session.commandId += 1

            val marker =
                "__REDMAGIC_ROOT_" +
                    session.token +
                    "_" +
                    session.commandId +
                    "__"

            session.writer.write(command)
            session.writer.newLine()
            session.writer.write(
                "redmagic_status=${'$'}?; " +
                    "printf '\\n$marker:%s\\n' " +
                    "\"${'$'}redmagic_status\""
            )
            session.writer.newLine()
            session.writer.flush()

            val output = StringBuilder()
            var exitCode: Int? = null

            while (exitCode == null) {
                val line =
                    session.reader.readLine()
                        ?: throw IllegalStateException(
                            "Persistent root shell closed"
                        )

                if (line.startsWith("$marker:")) {
                    exitCode =
                        line.substringAfter(':')
                            .trim()
                            .toIntOrNull()
                            ?: -1
                } else {
                    if (output.isNotEmpty()) {
                        output.append('\n')
                    }
                    output.append(line)
                }
            }

            if (exitCode != 0) {
                Log.w(
                    TAG,
                    "Root command failed with exit code " +
                        exitCode
                )
            }

            CommandResult(
                exitCode = exitCode,
                output = output.toString().trim()
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Persistent root command failed",
                error
            )

            closeSessionLocked(session)
            null
        }
    }

    private fun closeSession(
        session: Session
    ) {
        synchronized(commandLock) {
            closeSessionLocked(session)
        }
    }

    private fun closeSessionLocked(
        session: Session
    ) {
        if (session.closed) {
            if (sharedSession === session) {
                sharedSession = null
            }
            return
        }

        session.closed = true

        if (sharedSession === session) {
            sharedSession = null
        }

        runCatching {
            session.writer.write("exit")
            session.writer.newLine()
            session.writer.flush()
        }

        runCatching {
            session.writer.close()
        }

        runCatching {
            session.reader.close()
        }

        if (session.process.isAlive) {
            runCatching {
                session.process.destroy()
            }
        }
    }

    private fun runOneShotCommandLocked(
        command: String
    ): CommandResult? {
        return try {
            Log.w(
                TAG,
                "Using one-shot root fallback"
            )

            val process =
                ProcessBuilder(
                    "su",
                    "-c",
                    command
                )
                    .redirectErrorStream(true)
                    .start()

            val output =
                process.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }
                    .trim()

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                Log.w(
                    TAG,
                    "One-shot root command failed with " +
                        "exit code $exitCode"
                )
            }

            CommandResult(
                exitCode = exitCode,
                output = output
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "One-shot root execution failed",
                error
            )
            null
        }
    }
}
