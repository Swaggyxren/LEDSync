package com.xiiann.ledsync.data.executor

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface IRootExecutor {
    suspend fun runSu(cmd: String): Boolean
    suspend fun isRooted(): Boolean
    suspend fun runSuOutput(cmd: String): String
}

@Singleton
class RootExecutor @Inject constructor() : IRootExecutor {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadDispatcher: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(1)

    private var cachedRootStatus: Boolean? = null

    override suspend fun isRooted(): Boolean = withContext(singleThreadDispatcher) {
        cachedRootStatus?.let { return@withContext it }
        val ok = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-v"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (t: Throwable) {
            Log.e("RootExecutor", "isRooted check failed: ${t.message}")
            false
        }
        cachedRootStatus = ok
        ok
    }

    override suspend fun runSu(cmd: String): Boolean = withContext(singleThreadDispatcher) {
        return@withContext try {
            val p = Runtime.getRuntime().exec("su")
            DataOutputStream(p.outputStream).use { os ->
                os.writeBytes("$cmd\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            p.waitFor()
            val success = p.exitValue() == 0
            if (!success) {
                Log.w("RootExecutor", "su command exited with code ${p.exitValue()}: $cmd")
            }
            success
        } catch (t: Throwable) {
            Log.e("RootExecutor", "su exec failed for: $cmd", t)
            false
        }
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
}
