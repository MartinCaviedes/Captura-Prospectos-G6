package io.github.grupo6.capturaprospectos.data.device

import android.content.Context
import android.location.Geocoder
import android.os.Build
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.device.*
import java.io.IOException
import java.time.Clock
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal data class StreetParts(val street:String?,val number:String?) {
    override fun toString()="StreetParts(address=redacted)"
}
internal interface NativeAddressLookup {
    fun isPresent():Boolean
    /** false: otra operación sigue pendiente; no se ha empezado otra consulta. */
    fun request(coordinates:Coordinates,result:(StreetParts?)->Unit,error:(GeocodingFailure)->Unit):Boolean
}

internal class AndroidAddressGeocoder(private val source:NativeAddressLookup,
    private val clock:Clock=Clock.systemUTC()):ReverseGeocoder {
    constructor(context:Context):this(FrameworkAddressLookup(context.applicationContext))
    override suspend fun lookup(position:CaptureLocation.Device):GeocodingResult {
        if(!GeocodingPolicy.isFresh(position,clock.instant())) return GeocodingResult.Failed(GeocodingFailure.STALE_POSITION)
        if(!source.isPresent()) return GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE)
        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { continuation ->
                val finished=AtomicBoolean(false)
                fun finish(value:GeocodingResult) {
                    if(finished.compareAndSet(false,true) && continuation.isActive) continuation.resume(value)
                }
                continuation.invokeOnCancellation {finished.set(true)}
                val started=source.request(position.coordinates,result={ parts ->
                    fun clean(value:String?,max:Int)=value?.trim()?.takeIf {it.length in 1..max && it.none(Char::isISOControl)}
                    val street=clean(parts?.street,160)
                    if(street==null) finish(GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE))
                    else finish(GeocodingResult.Found(listOfNotNull(street,clean(parts?.number,30)).joinToString(" ")))
                },error={finish(GeocodingResult.Failed(it))})
                if(!started) finish(GeocodingResult.Failed(GeocodingFailure.LIMITED))
            }
        } ?: GeocodingResult.Failed(GeocodingFailure.UNAVAILABLE)
    }
}

private class FrameworkAddressLookup(context:Context):NativeAddressLookup {
    private val geocoder=Geocoder(context,Locale.forLanguageTag("es-CL"))
    override fun isPresent()=Geocoder.isPresent()
    override fun request(coordinates:Coordinates,result:(StreetParts?)->Unit,error:(GeocodingFailure)->Unit):Boolean {
        if(!pending.compareAndSet(false,true)) return false
        val delivered=AtomicBoolean(false)
        fun finish(parts:StreetParts?,failure:GeocodingFailure?=null) {
            if(delivered.compareAndSet(false,true)) {
                pending.set(false)
                if(failure==null) result(parts) else error(failure)
            }
        }
        executor.execute {
            try {
                if(Build.VERSION.SDK_INT>=33) {
                    geocoder.getFromLocation(coordinates.latitude,coordinates.longitude,1,object:Geocoder.GeocodeListener {
                        override fun onGeocode(addresses:MutableList<android.location.Address>) {
                            finish(addresses.firstOrNull()?.let {StreetParts(it.thoroughfare,it.subThoroughfare)})
                        }
                        override fun onError(errorMessage:String?) {finish(null,GeocodingFailure.UNAVAILABLE)}
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses=geocoder.getFromLocation(coordinates.latitude,coordinates.longitude,1)
                    finish(addresses?.firstOrNull()?.let {StreetParts(it.thoroughfare,it.subThoroughfare)})
                }
            } catch(_:IOException) {finish(null,GeocodingFailure.NETWORK)}
            catch(_:IllegalArgumentException) {finish(null,GeocodingFailure.INVALID_RESPONSE)}
            catch(_:SecurityException) {finish(null,GeocodingFailure.UNAVAILABLE)}
            catch(_:IllegalStateException) {finish(null,GeocodingFailure.UNAVAILABLE)}
        }
        return true
    }
    companion object {
        // La llamada antigua no es cancelable: no encolar más trabajo si todavía no terminó.
        private val pending=AtomicBoolean(false)
        private val executor=Executors.newSingleThreadExecutor {task -> Thread(task,"g6-address-geocoder").apply {isDaemon=true}}
    }
}
