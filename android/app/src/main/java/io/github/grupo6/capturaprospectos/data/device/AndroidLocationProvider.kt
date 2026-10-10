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
import io.github.grupo6.capturaprospectos.domain.device.LocationProvider
import io.github.grupo6.capturaprospectos.domain.device.LocationResult
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Implementación Android nativa de LocationProvider mediante LocationManager del framework.
 * Cumple con los requisitos de E1 (RF04):
 * - Verificación de permisos de tiempo de ejecución (ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION).
 * - Verificación de estado de proveedores satelitales/red.
 * - Adquisición acotada en tiempo (timeout).
 * - Cancelación reactiva sin fugas de listeners, sin caché arbitrario y sin fallback implícito.
 */
class AndroidLocationProvider(
    private val context: Context? = null,
    private val locationManager: LocationManager? = context?.applicationContext?.getSystemService(LocationManager::class.java),
    private val permissionChecker: (String) -> Boolean = { perm ->
        context?.let { ctx ->
            try {
                ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                false
            }
        } ?: false
    },
    private val isProviderEnabled: (LocationManager, String) -> Boolean = { manager, provider ->
        try {
            manager.isProviderEnabled(provider)
        } catch (_: Exception) {
            false
        }
    },
) : LocationProvider {

    constructor(context: Context) : this(
        context = context,
        locationManager = context.applicationContext.getSystemService(LocationManager::class.java),
    )

    override suspend fun request(timeout: Duration): LocationResult {
        require(!timeout.isNegative && !timeout.isZero) { "Timeout must be positive" }

        val hasFine = permissionChecker(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = permissionChecker(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasFine && !hasCoarse) {
            return LocationResult.PermissionDenied
        }

        val manager = locationManager ?: return LocationResult.Unavailable

        val isGpsEnabled = try {
            isProviderEnabled(manager, LocationManager.GPS_PROVIDER)
        } catch (_: SecurityException) {
            return LocationResult.PermissionDenied
        } catch (_: Exception) {
            false
        }

        val isNetworkEnabled = try {
            isProviderEnabled(manager, LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            return LocationResult.PermissionDenied
        } catch (_: Exception) {
            false
        }

        if (!isGpsEnabled && !isNetworkEnabled) {
            return LocationResult.ServicesDisabled
        }

        return try {
            val result = withTimeoutOrNull(timeout.toMillis()) {
                acquireSingleLocation(manager, isGpsEnabled, isNetworkEnabled)
            }
            result ?: LocationResult.TimedOut
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            LocationResult.PermissionDenied
        } catch (_: Exception) {
            LocationResult.Unavailable
        }
    }

    private suspend fun acquireSingleLocation(
        manager: LocationManager,
        gpsEnabled: Boolean,
        networkEnabled: Boolean,
    ): LocationResult = suspendCancellableCoroutine { continuation ->
        val providerName = when {
            gpsEnabled -> LocationManager.GPS_PROVIDER
            networkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> {
                continuation.resume(LocationResult.ServicesDisabled)
                return@suspendCancellableCoroutine
            }
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (continuation.isActive) {
                    try {
                        manager.removeUpdates(this)
                    } catch (_: Exception) {}

                    val coords = Coordinates(loc.latitude, loc.longitude)
                    if (coords.isValid() && loc.accuracy.isFinite() && loc.accuracy >= 0f) {
                        val deviceLoc = CaptureLocation.Device(
                            coordinates = coords,
                            measuredAt = Instant.ofEpochMilli(loc.time),
                            accuracyMeters = loc.accuracy.toDouble(),
                        )
                        continuation.resume(LocationResult.Candidate(deviceLoc))
                    } else {
                        continuation.resume(LocationResult.Unavailable)
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                if (continuation.isActive) {
                    try {
                        manager.removeUpdates(this)
                    } catch (_: Exception) {}
                    continuation.resume(LocationResult.ServicesDisabled)
                }
            }
        }

        continuation.invokeOnCancellation {
            try {
                manager.removeUpdates(listener)
            } catch (_: Exception) {}
        }

        try {
            manager.requestLocationUpdates(
                providerName,
                0L,
                0f,
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: SecurityException) {
            continuation.resume(LocationResult.PermissionDenied)
        } catch (_: Exception) {
            continuation.resume(LocationResult.Unavailable)
        }
    }
}
