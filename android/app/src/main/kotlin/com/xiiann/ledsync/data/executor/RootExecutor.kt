package com.xiiann.ledsync.data.executor

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

interface IRootExecutor {
    suspend fun runSu(cmd: String): Boolean
    suspend fun runSuWithRetry(cmd: String, maxRetries: Int = 1, delayMs: Long = 150L): Boolean
    suspend fun isRooted(forceRecheck: Boolean = false): Boolean
    suspend fun runSuOutput(cmd: String): String
    fun closeShell()
}

@Singleton
class RootExecutor @Inject constructor() : IRootExecutor {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadDispatcher: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(1)

    private var cachedRootStatus: Boolean? = null

    private var process: Process? = null
    private var writer: DataOutputStream? = null
    private var reader: BufferedReader? = null

    private val SENTINEL = "__LED_SU_EXIT__"

    override suspend fun isRooted(forceRecheck: Boolean): Boolean = withContext(singleThreadDispatcher) {
        if (!forceRecheck) {
            cachedRootStatus?.let { return@withContext it }
        }
        val ok = try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-v"))
            val exitCode = p.waitFor()
            exitCode == 0
        } catch (t: Throwable) {
            Log.e("RootExecutor", "isRooted check failed: ${t.message}")
            false
        }
        cachedRootStatus = ok
        ok
    }

    private fun getOrStartShell(): Boolean {
        if (process != null && isProcessAlive(process)) {
            return true
        }
        closeShellInternal()
        return try {
            val p = Runtime.getRuntime().exec("su")
            process = p
            writer = DataOutputStream(p.outputStream)
            reader = BufferedReader(InputStreamReader(p.inputStream))
            true
        } catch (t: Throwable) {
            Log.e("RootExecutor", "Failed to spawn su shell", t)
            closeShellInternal()
            false
        }
    }

    private fun isProcessAlive(p: Process?): Boolean {
        if (p == null) return false
        return try {
            p.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    override suspend fun runSu(cmd: String): Boolean = withContext(singleThreadDispatcher) {
        return@withContext runSuInternal(cmd)
    }

    private fun runSuInternal(cmd: String): Boolean {
        if (!getOrStartShell()) return false
        val w = writer ?: return false
        val r = reader ?: return false

        return try {
            w.writeBytes("$cmd\n")
            w.writeBytes("echo \"$SENTINEL$?\"\n")
            w.flush()

            var line: String?
            var exitCode = -1
            while (true) {
                line = r.readLine() ?: break
                if (line.contains(SENTINEL)) {
                    val statusStr = line.substringAfter(SENTINEL).trim()
                    exitCode = statusStr.toIntOrNull() ?: -1
                    break
                }
            }
            val success = exitCode == 0
            if (!success) {
                Log.w("RootExecutor", "su cmd exited code $exitCode: $cmd")
            }
            success
        } catch (t: Throwable) {
            Log.e("RootExecutor", "Persistent su exec failed: $cmd", t)
            closeShellInternal()
            false
        }
    }

    override suspend fun runSuWithRetry(cmd: String, maxRetries: Int, delayMs: Long): Boolean = withContext(singleThreadDispatcher) {
        var attempts = 0
        while (attempts <= maxRetries) {
            val ok = runSuInternal(cmd)
            if (ok) return@withContext true
            attempts++
            if (attempts <= maxRetries) {
                delay(delayMs)
            }
        }
        return@withContext false
    }

    override suspend fun runSuOutput(cmd: String): String = withContext(singleThreadDispatcher) {
        return@withContext try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor()
            output.trim()
        } catch (t: Throwable) {
            Log.e("RootExecutor", "runSuOutput failed for: $cmd", t)
            ""
        }
    }

    override fun closeShell() {
        closeShellInternal()
    }

    private fun closeShellInternal() {
        try { writer?.writeBytes("exit\n"); writer?.flush() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        writer = null
        reader = null
        process = null
    }
}
