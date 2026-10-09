package io.github.grupo6.capturaprospectos.domain.device

import kotlinx.coroutines.flow.Flow

/** Red del dispositivo, nunca ACK ni disponibilidad del backend. */
enum class ConnectivityStatus { UNKNOWN, OFFLINE, CONNECTED_UNVALIDATED, VALIDATED }
interface ConnectivityObserver { fun observe(): Flow<ConnectivityStatus> }

fun connectivityStatus(hasNetwork: Boolean, capabilitiesKnown: Boolean, validated: Boolean): ConnectivityStatus = when {
    !hasNetwork -> ConnectivityStatus.OFFLINE
    !capabilitiesKnown -> ConnectivityStatus.UNKNOWN
    validated -> ConnectivityStatus.VALIDATED
    else -> ConnectivityStatus.CONNECTED_UNVALIDATED
}
