package com.txstate.countryexplorertask

import CountryAdapter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sravan.countries.util.NetworkStatus
import com.sravan.countries.util.NetworkUtil
import com.txstate.countryexplorertask.network.CountryRepository
import com.txstate.countryexplorertask.viewmodel.CountryViewModel
import com.txstate.countryexplorertask.viewmodel.CountryViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var adapter: CountryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var refreshButton: Button

    private val viewModel: CountryViewModel by viewModels {
        CountryViewModelFactory(CountryRepository())
    }

    // Track network status
    private var isNetworkAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupRecyclerView()
        observeViewModel()
        observeNetwork()
        setupListeners()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.countryRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        refreshButton = findViewById(R.id.refreshButton)
        adapter = CountryAdapter()
    }

    private fun setupRecyclerView() {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeViewModel() {
        // List update
        lifecycleScope.launch {
            viewModel.countries.collectLatest { list ->
                adapter.submitList(list)
            }
        }
        // Error messages
        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMsg ->
                if (!errorMsg.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
        // Loading indicator
        lifecycleScope.launch {
            viewModel.loading.collectLatest { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            NetworkUtil.observeNetworkStatus(context = this@MainActivity)
                .collect { status ->
                    when (status) {
                        is NetworkStatus.Available -> {
                            isNetworkAvailable = true
                            showContent()
                            // Fetch data only if list is empty (prevents double fetch)
                            if (adapter.itemCount == 0) {
                                fetchCountriesIfNetwork()
                            }
                        }
                        is NetworkStatus.Unavailable, is NetworkStatus.Lost -> {
                            if (adapter.currentList.isEmpty()) {
                                isNetworkAvailable = false
                                showNetworkError()
                            }

                        }
                        is NetworkStatus.Losing -> {
                            // Optional: show warning if needed
                        }
                        is NetworkStatus.Error -> {
                            // Optional: handle generic errors
                        }
                    }
                }
        }
    }

    private fun setupListeners() {
        refreshButton.setOnClickListener {
            fetchCountriesIfNetwork()
        }
    }

    private fun fetchCountriesIfNetwork() {
        if (isNetworkAvailable) {
            viewModel.fetchCountries()
        } else {
            showNetworkError()
        }
    }

    private fun showNetworkError() {
        errorTextView.visibility = View.VISIBLE
        refreshButton.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showContent() {
        errorTextView.visibility = View.GONE
        refreshButton.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
