package io.github.grupo6.capturaprospectos.domain.records

import java.time.Instant
import org.junit.Test
import org.junit.Assert.*

class RecordContractsTest {
    @Test fun queryBoundsAndHalfOpenDates() {
        val start = Instant.EPOCH
        assertTrue(RecordQuery(RecordFilter(fromInclusive = start, untilExclusive = start.plusSeconds(1))).isValid())
        assertFalse(RecordQuery(RecordFilter(fromInclusive = start, untilExclusive = start)).isValid())
        assertFalse(RecordQuery(limit = 0).isValid()); assertFalse(RecordQuery(limit = 101).isValid())
        assertFalse(RecordQuery(RecordFilter(query = "a".repeat(121))).isValid())
    }
    @Test fun searchTermsDoNotLeakViaDiagnostics() {
        assertFalse(RecordQuery(RecordFilter(query = "Persona ficticia")).toString().contains("Persona ficticia"))
        assertFalse(RecordFilter(query = "12345678-5").toString().contains("12345678-5"))
    }
}
