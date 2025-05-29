package com.txstate.countryexplorertask

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.txstate.countryexplorertask.model.Country
import com.txstate.countryexplorertask.model.Currency
import com.txstate.countryexplorertask.model.Language
import com.txstate.countryexplorertask.network.CountryRepository
import com.txstate.countryexplorertask.viewmodel.CountryViewModel
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class CountryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CountryRepository
    private lateinit var viewModel: CountryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = CountryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchCountries emits countries and loading changes`() = runTest {
        val mockCountries = listOf(
            Country(
                name = "United States",
                capital = "Washington, D.C.",
                code = "US",
                flag = "https://flagcdn.com/us.svg",
                region = "Americas",
                language = Language("en", "English"),
                currency = Currency("USD", "United States dollar", "$")
            )
        )
        whenever(repository.getCountries()).thenReturn(mockCountries)

        viewModel.fetchCountries()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.loading.value)
        assertEquals(mockCountries, viewModel.countries.value)
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `fetchCountries sets error if repository throws exception`() = runTest {
        whenever(repository.getCountries()).thenThrow(RuntimeException("Network error"))

        viewModel.fetchCountries()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.loading.value)
        assertEquals(emptyList<Country>(), viewModel.countries.value)
        assertEquals("Failed to load data: Network error", viewModel.error.value)
    }

    @Test
    fun `fetchCountries sets error if no countries found`() = runTest {
        whenever(repository.getCountries()).thenReturn(emptyList())

        viewModel.fetchCountries()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.loading.value)
        assertEquals(emptyList<Country>(), viewModel.countries.value)
        assertEquals("No countries found.", viewModel.error.value)
    }
}