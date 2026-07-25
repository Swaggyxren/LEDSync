package com.xiiann.ledsync.data.repository

import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.domain.model.LH8nConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareRepositoryTest {

    private class MockRootExecutor : IRootExecutor {
        val executedCommands = mutableListOf<String>()
        var rootStatus = true

        override suspend fun runSu(cmd: String): Boolean {
            executedCommands.add(cmd)
            return true
        }

        override suspend fun runSuWithRetry(cmd: String, maxRetries: Int, delayMs: Long): Boolean {
            return runSu(cmd)
        }

        override suspend fun isRooted(forceRecheck: Boolean): Boolean = rootStatus

        override suspend fun runSuOutput(cmd: String): String = ""

        override fun closeShell() {}
    }

    private lateinit var mockExecutor: MockRootExecutor
    private lateinit var repository: HardwareRepository

    @Before
    fun setUp() {
        mockExecutor = MockRootExecutor()
        repository = HardwareRepository(mockExecutor)
        repository.setConfig(LH8nConfig())
    }

    @Test
    fun `ensureLedEnabled primes hardware on first call and caches state`() = runTest {
        val result1 = repository.ensureLedEnabled(force = false)
        assertTrue(result1)
        assertEquals(1, mockExecutor.executedCommands.size)
        assertTrue(mockExecutor.executedCommands[0].contains("hwen"))

        // Second call without force should use cache and perform 0 additional su calls
        val result2 = repository.ensureLedEnabled(force = false)
        assertTrue(result2)
        assertEquals(1, mockExecutor.executedCommands.size)
    }

    @Test
    fun `ensureLedEnabled re-primes hardware when force is true`() = runTest {
        repository.ensureLedEnabled(force = false)
        assertEquals(1, mockExecutor.executedCommands.size)

        repository.ensureLedEnabled(force = true)
        assertEquals(2, mockExecutor.executedCommands.size)
    }

    @Test
    fun `turnOffAll sends turn off hex and invalidates active cache`() = runTest {
        repository.ensureLedEnabled(force = false)
        assertEquals(1, mockExecutor.executedCommands.size)

        repository.turnOffAll()
        assertEquals(2, mockExecutor.executedCommands.size)
        assertTrue(mockExecutor.executedCommands[1].contains("00 01 00 00 00 00"))

        // After turnOffAll, next write must re-prime
        repository.sendRawHex("00 05 01 00 00 00")
        // Should have: 1 (initial prime) + 1 (turnOff) + 1 (re-prime) + 1 (sendRawHex) = 4 commands
        assertEquals(4, mockExecutor.executedCommands.size)
    }

    @Test
    fun `emergencyKillAndRevive power cycles hardware and re-initializes`() = runTest {
        val ok = repository.emergencyKillAndRevive(offTimeMs = 10L)
        assertTrue(ok)
        assertTrue(repository.isReady.value)
        // Commands executed: 1 (kill command) + 1 (re-prime command)
        assertEquals(2, mockExecutor.executedCommands.size)
        assertTrue(mockExecutor.executedCommands[0].contains("echo 0 >"))
    }
}
