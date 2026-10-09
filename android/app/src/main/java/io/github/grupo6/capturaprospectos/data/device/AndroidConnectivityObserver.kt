package io.github.grupo6.capturaprospectos.data.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.github.grupo6.capturaprospectos.domain.device.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    override fun observe() = callbackFlow {
        if (manager == null) { trySend(ConnectivityStatus.UNKNOWN); close(); return@callbackFlow }
        val lock = Any()
        var current: Network? = null
        var receivedCallback = false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = synchronized(lock) {
                receivedCallback = true; current = network
                trySend(ConnectivityStatus.UNKNOWN); Unit
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = synchronized(lock) {
                receivedCallback = true
                if (current == network) trySend(connectivityStatus(true, true,
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)))
                Unit
            }
            override fun onLost(network: Network) = synchronized(lock) {
                receivedCallback = true
                if (current == network) { current = null; trySend(ConnectivityStatus.OFFLINE) }
                Unit
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
        } catch (_: SecurityException) {
            trySend(ConnectivityStatus.UNKNOWN); close(); return@callbackFlow
        }
        try {
            synchronized(lock) {
                if (!receivedCallback) {
                    current = manager.activeNetwork
                    val caps = current?.let(manager::getNetworkCapabilities)
                    trySend(connectivityStatus(current != null, caps != null,
                        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true))
                }
            }
        } catch (_: SecurityException) { trySend(ConnectivityStatus.UNKNOWN) }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
