package com.txstate.countryexplorertask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txstate.countryexplorertask.model.Country
import com.txstate.countryexplorertask.network.CountryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CountryViewModel(private val repository: CountryRepository) : ViewModel() {
    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchCountries() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val list = repository.getCountries()
                _countries.value = list
                if (list.isEmpty()) {
                    _error.value = "No countries found."
                }
            } catch (e: Exception) {
                _countries.value = emptyList()
                _error.value = "Failed to load data: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }
}
