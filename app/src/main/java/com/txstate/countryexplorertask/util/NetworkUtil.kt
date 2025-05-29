package com.sravan.countries.util

import android.content.Context
import android.net.*
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Sealed class representing different network states.
 */
sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Losing : NetworkStatus()
    object Lost : NetworkStatus()
    data class Error(val error: Throwable) : NetworkStatus()
}

object NetworkUtil {

    /**
     * Checks the current network connectivity status.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            networkCapabilities?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Observes detailed network status changes as a Flow.
     *
     * @param context Application context
     * @param connectivityManager Optional, for dependency injection/testing.
     */
    fun observeNetworkStatus(
        context: Context,
        connectivityManager: ConnectivityManager? = null
    ): Flow<NetworkStatus> = callbackFlow {
        val cm = connectivityManager
            ?: context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkStatus.Available).isSuccess
            }

            override fun onUnavailable() {
                trySend(NetworkStatus.Unavailable).isSuccess
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(NetworkStatus.Losing).isSuccess
            }

            override fun onLost(network: Network) {
                trySend(NetworkStatus.Lost).isSuccess
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // Optionally, you can react to changes in network capabilities
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                // Optionally, log or handle link property changes
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                // Optionally, handle blocked status changes
            }
        }

        // Emit initial state for current connection
        val isConnected = isNetworkAvailable(context)
        trySend(if (isConnected) NetworkStatus.Available else NetworkStatus.Unavailable).isSuccess

        // Register callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            trySend(NetworkStatus.Error(e)).isSuccess
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
                // Already unregistered
            }
        }
    }.conflate()
}
