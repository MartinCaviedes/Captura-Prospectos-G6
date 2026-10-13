package io.github.grupo6.capturaprospectos.data.local

import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.capture.LocalActor
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.capture.LocalCaptureAccess
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class StressDataset400Test {

    data class SyntheticProspect(
        val id: UUID,
        val ownerId: UUID,
        val siteId: UUID,
        val eventId: UUID,
        val state: CaptureState,
        val fullName: String,
        val capturedAt: Instant,
    )

    private val eventId = UUID.randomUUID()
    private val siteId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()

    @Test
    fun fixtureHas400RowsAndExpectedStateDistribution() {
        val records = (0 until 400).map { i ->
            val state = when (i % 20) {
                0, 1 -> CaptureState.CANCELLED
                2, 3, 4 -> CaptureState.NEEDS_CONTEXT
                5, 6 -> CaptureState.DRAFT
                else -> CaptureState.READY
            }
            SyntheticProspect(
                id = UUID.randomUUID(),
                ownerId = ownerId,
                siteId = siteId,
                eventId = eventId,
                state = state,
                fullName = "Prospecto Sintetico $i",
                capturedAt = Instant.now().minusSeconds(i.toLong() * 60),
            )
        }

        assertEquals(400, records.size)

        // Verificación de distribución de estados
        val readyCount = records.count { it.state == CaptureState.READY }
        val draftCount = records.count { it.state == CaptureState.DRAFT }
        val needsContextCount = records.count { it.state == CaptureState.NEEDS_CONTEXT }
        val cancelledCount = records.count { it.state == CaptureState.CANCELLED }

        assertEquals(260, readyCount)
        assertEquals(40, draftCount)
        assertEquals(60, needsContextCount)
        assertEquals(40, cancelledCount)
        assertEquals(400, readyCount + draftCount + needsContextCount + cancelledCount)

        // Agrupación del fixture: no prueba el cursor Room ni el rendimiento Android.
        val pageSize = 20
        val pages = records.chunked(pageSize)
        assertEquals(20, pages.size)
        assertEquals(400, pages.sumOf { it.size })

        // Filtro por estado READY
        val readyRecords = records.filter { it.state == CaptureState.READY }
        assertEquals(260, readyRecords.size)

        // Filtro de búsqueda textual
        val searchMatch = records.filter { it.fullName.contains("100") }
        assertEquals(1, searchMatch.size)
        assertEquals("Prospecto Sintetico 100", searchMatch.first().fullName)
    }

    @Test
    fun stressTrayFilterPreservesRoleIsolation() {
        val otherOwnerId = UUID.randomUUID()
        val mixedRecords = (0 until 400).map { i ->
            SyntheticProspect(
                id = UUID.randomUUID(),
                ownerId = if (i % 2 == 0) ownerId else otherOwnerId,
                siteId = siteId,
                eventId = eventId,
                state = if (i % 4 == 0 || i % 8 == 1) CaptureState.DRAFT else CaptureState.READY,
                fullName = "Prospecto $i",
                capturedAt = Instant.now().minusSeconds(i.toLong()),
            )
        }

        // Filtro captador: sólo sus propios registros
        val captor = LocalActor(ownerId, LocalRole.CAPTOR, setOf(siteId))
        val captorView = mixedRecords.filter { LocalCaptureAccess.canRead(captor,it.ownerId,it.siteId,it.state) }
        assertEquals(200, captorView.size)

        // Filtro supervisor: ve todos los de la sede EXCLUYENDO borradores ajenos
        val supervisor = captor.copy(role = LocalRole.SUPERVISOR)
        val supervisorView = mixedRecords.filter { LocalCaptureAccess.canRead(supervisor,it.ownerId,it.siteId,it.state) }
        // i % 8 == 1 aporta 50 borradores ajenos; i % 4 == 0 aporta 100 propios.
        // Quedan 200 (propios) + 150 (ajenos confirmados) = 350
        assertEquals(350, supervisorView.size)
        assertEquals(50, mixedRecords.count { it.ownerId == otherOwnerId && it.state == CaptureState.DRAFT })
        assertTrue(mixedRecords.none { LocalCaptureAccess.canRead(supervisor.copy(siteIds = emptySet()),it.ownerId,it.siteId,it.state) })
    }
}
