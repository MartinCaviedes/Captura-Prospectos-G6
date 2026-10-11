package io.github.grupo6.capturaprospectos.data.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import io.github.grupo6.capturaprospectos.domain.capture.CaptureLocation
import io.github.grupo6.capturaprospectos.domain.capture.Coordinates
import io.github.grupo6.capturaprospectos.domain.capture.LocationPolicy
import io.github.grupo6.capturaprospectos.domain.device.LocationProvider
import io.github.grupo6.capturaprospectos.domain.device.LocationResult
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

enum class NativeLocationSource { GPS, NETWORK }

/** Adaptador de suscripciones nativas; permite comprobar adquisición y cierre con fuentes ficticias. */
interface NativeLocationUpdates {
    fun isEnabled(source: NativeLocationSource): Boolean
    fun subscribe(source: NativeLocationSource, onPosition: (CaptureLocation.Device) -> Unit,
        onDisabled: () -> Unit): AutoCloseable
}

/** Adquiere desde los proveedores permitidos, sin ubicación cacheada ni confirmación implícita. */
class AndroidLocationProvider(
    private val context: Context? = null,
    private val locationManager: LocationManager? = context?.applicationContext?.getSystemService(LocationManager::class.java),
    private val permissionChecker: (String) -> Boolean = { permission ->
        context?.let { ContextCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_GRANTED } ?: false
    },
    private val updates: NativeLocationUpdates? = null,
    private val now: () -> Instant = { Instant.now() },
) : LocationProvider {
    constructor(context: Context) : this(context = context,
        locationManager = context.applicationContext.getSystemService(LocationManager::class.java))

    override suspend fun request(timeout: Duration): LocationResult {
        require(!timeout.isNegative && !timeout.isZero) { "Timeout must be positive" }
        val fine = permissionChecker(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = permissionChecker(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) return LocationResult.PermissionDenied
        val source = updates ?: locationManager?.let(::FrameworkLocationUpdates) ?: return LocationResult.Unavailable
        return try {
            val allowed = listOfNotNull(NativeLocationSource.GPS.takeIf { fine }, NativeLocationSource.NETWORK)
                .filter(source::isEnabled)
            if (allowed.isEmpty()) return LocationResult.ServicesDisabled
            val lastRejected = AtomicReference<CaptureLocation.Device?>(null)
            withTimeoutOrNull(timeout.toMillis()) {
                acquire(source, allowed, lastRejected)
            } ?: lastRejected.get()?.let(LocationResult::Candidate) ?: LocationResult.TimedOut
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: SecurityException) { LocationResult.PermissionDenied }
        catch (_: IllegalArgumentException) { LocationResult.Unavailable }
        catch (_: IllegalStateException) { LocationResult.Unavailable }
    }

    /** El primer resultado válido gana; una muestra imprecisa no bloquea al otro proveedor. */
    private suspend fun acquire(source: NativeLocationUpdates, allowed: List<NativeLocationSource>,
        rejected: AtomicReference<CaptureLocation.Device?>): LocationResult = suspendCancellableCoroutine { continuation ->
        val lock = Any()
        val subscriptions = mutableListOf<AutoCloseable>()
        val live = allowed.toMutableSet()
        var finished = false
        fun closeAll() {
            val closing = synchronized(lock) { subscriptions.toList().also { subscriptions.clear() } }
            closing.forEach { try { it.close() } catch (_: SecurityException) { } catch (_: IllegalStateException) { } }
        }
        fun finish(result: LocationResult) {
            val won = synchronized(lock) { if (finished) false else { finished = true; true } }
            if (won) { closeAll(); continuation.resume(result) }
        }
        continuation.invokeOnCancellation { synchronized(lock) { finished = true }; closeAll() }
        for (provider in allowed) {
            if (synchronized(lock) { finished }) break
            try {
                val subscription = source.subscribe(provider, onPosition = { position ->
                    if (LocationPolicy().isValid(position, now(), UUID(0,0), null)) finish(LocationResult.Candidate(position))
                    else if (!synchronized(lock) { finished }) rejected.set(position)
                }, onDisabled = {
                    val none = synchronized(lock) { live.remove(provider); live.isEmpty() }
                    if (none) finish(LocationResult.ServicesDisabled)
                })
                val closeNow = synchronized(lock) {
                    if (finished || !continuation.isActive) true else { subscriptions += subscription; false }
                }
                if (closeNow) subscription.close()
            } catch (_: SecurityException) { finish(LocationResult.PermissionDenied) }
            catch (_: IllegalArgumentException) {
                val none = synchronized(lock) { live.remove(provider); live.isEmpty() }
                if (none) finish(LocationResult.Unavailable)
            } catch (_: IllegalStateException) { finish(LocationResult.Unavailable) }
        }
    }
}

/** Convierte únicamente muestras con precisión declarada; cada suscripción retira su listener. */
private class FrameworkLocationUpdates(private val manager: LocationManager): NativeLocationUpdates {
    private fun provider(source: NativeLocationSource) = when (source) {
        NativeLocationSource.GPS -> LocationManager.GPS_PROVIDER
        NativeLocationSource.NETWORK -> LocationManager.NETWORK_PROVIDER
    }
    override fun isEnabled(source: NativeLocationSource) = manager.isProviderEnabled(provider(source))
    override fun subscribe(source: NativeLocationSource, onPosition: (CaptureLocation.Device) -> Unit,
        onDisabled: () -> Unit): AutoCloseable {
        val listener = object: LocationListener {
            override fun onLocationChanged(location: Location) {
                val coordinates = Coordinates(location.latitude, location.longitude)
                if (location.hasAccuracy() && coordinates.isValid() && location.accuracy.isFinite() && location.accuracy >= 0f)
                    onPosition(CaptureLocation.Device(coordinates, Instant.ofEpochMilli(location.time), location.accuracy.toDouble()))
            }
            override fun onProviderDisabled(provider: String) = onDisabled()
            override fun onProviderEnabled(provider: String) { }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
        }
        try {
            manager.requestLocationUpdates(provider(source), 0L, 0f, listener, Looper.getMainLooper())
        } catch (error: SecurityException) {
            try { manager.removeUpdates(listener) } catch (_: SecurityException) { }
            throw error
        }
        return AutoCloseable { manager.removeUpdates(listener) }
    }
}
