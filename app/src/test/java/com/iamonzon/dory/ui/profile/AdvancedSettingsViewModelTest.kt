package com.iamonzon.dory.ui.profile

import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: AdvancedSettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDataStore = FakeDataStore()
        settingsRepository = SettingsRepository(fakeDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `desired retention is loaded from settings`() = runTest(testDispatcher) {
        settingsRepository.setDesiredRetention(0.85)

        viewModel = AdvancedSettingsViewModel(settingsRepository)
        advanceUntilIdle()

        assertEquals(0.85, viewModel.uiState.value.desiredRetention, 0.001)
    }

    @Test
    fun `setDesiredRetention persists to settings`() = runTest(testDispatcher) {
        viewModel = AdvancedSettingsViewModel(settingsRepository)
        advanceUntilIdle()

        viewModel.setDesiredRetention(0.95)
        advanceUntilIdle()

        assertEquals(0.95, settingsRepository.getDesiredRetention(), 0.001)
    }

    @Test
    fun `FSRS weights are displayed from defaults`() = runTest(testDispatcher) {
        viewModel = AdvancedSettingsViewModel(settingsRepository)
        advanceUntilIdle()

        val weights = viewModel.uiState.value.fsrsWeights
        assertEquals(17, weights.size)
        assertEquals(FsrsParameters.DEFAULT_WEIGHTS.toList(), weights)
    }
}
