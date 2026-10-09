package io.github.grupo6.capturaprospectos.domain.device

import org.junit.Test
import org.junit.Assert.assertEquals

class ConnectivityTest {
    @Test fun noNetworkIsOfflineEvenWithStaleCapabilities() {
        assertEquals(ConnectivityStatus.OFFLINE, connectivityStatus(false, true, true))
    }
    @Test fun missingCapabilitiesDoesNotClaimInternet() {
        assertEquals(ConnectivityStatus.UNKNOWN, connectivityStatus(true, false, true))
    }
    @Test fun captiveOrUnvalidatedNetworkIsSeparateFromOffline() {
        assertEquals(ConnectivityStatus.CONNECTED_UNVALIDATED, connectivityStatus(true, true, false))
    }
    @Test fun validatedNetworkIsOnlyNetworkEvidence() {
        assertEquals(ConnectivityStatus.VALIDATED, connectivityStatus(true, true, true))
    }
}
