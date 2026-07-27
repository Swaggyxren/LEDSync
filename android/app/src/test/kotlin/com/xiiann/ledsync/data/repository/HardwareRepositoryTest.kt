package com.xiiann.ledsync.data.repository

import com.xiiann.ledsync.data.executor.IRootExecutor
import com.xiiann.ledsync.domain.model.LH8nConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    private lateinit var mockPreferences: PreferencesRepository
    private lateinit var repository: HardwareRepository

    @Before
    fun setUp() {
        mockExecutor = MockRootExecutor()
        mockPreferences = mockk(relaxed = true) {
            every { audioLedEnabled } returns flowOf(false)
            every { audioLedDynamic } returns flowOf(false)
            every { audioLedGain } returns flowOf(3)
        }
        repository = HardwareRepository(mockExecutor, mockPreferences)
        repository.setConfig(LH8nConfig())
    }

    @Test
    fun `ensureLedEnabled primes hardware on first call and caches state`() = runTest {
        val result1 = repository.ensureLedEnabled(force = false)
        assertTrue(result1)
        assertEquals(1, mockExecutor.executedCommands.size)
        assertTrue(mockExecutor.executedCommands[0].contains("hwen"))

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

        repository.sendRawHex("00 05 01 00 00 00")
        assertEquals(4, mockExecutor.executedCommands.size)
    }

    @Test
    fun `emergencyKillAndRevive power cycles hardware and re-initializes`() = runTest {
        val ok = repository.emergencyKillAndRevive(offTimeMs = 10L)
        assertTrue(ok)
        assertTrue(repository.isReady.value)
        assertEquals(2, mockExecutor.executedCommands.size)
        assertTrue(mockExecutor.executedCommands[0].contains("echo 0 >"))
    }
}
