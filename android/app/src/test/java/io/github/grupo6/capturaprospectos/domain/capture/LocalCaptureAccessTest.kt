package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class LocalCaptureAccessTest {
    private val owner = UUID.randomUUID()
    private val site = UUID.randomUUID()
    private val actor = LocalActor(owner, LocalRole.CAPTOR, setOf(site))
    private val event = CaptureEvent(UUID.randomUUID(), site, "Evento ficticio", Instant.EPOCH,
        Instant.EPOCH.plusSeconds(3600), ZoneId.of("America/Santiago"), setOf(owner), null)

    @Test fun creationRequiresCaptorSiteAndAssignment() {
        assertTrue(LocalCaptureAccess.canCreate(actor, event))
        assertFalse(LocalCaptureAccess.canCreate(actor.copy(role = LocalRole.SUPERVISOR), event))
        assertFalse(LocalCaptureAccess.canCreate(actor.copy(siteIds = emptySet()), event))
        assertFalse(LocalCaptureAccess.canCreate(actor, event.copy(assignedCaptorIds = emptySet())))
    }
    @Test fun invalidEventCannotAuthorizeCreation() {
        assertFalse(LocalCaptureAccess.canCreate(actor, event.copy(endsAt = event.startsAt)))
        assertFalse(LocalCaptureAccess.canCreate(actor, event.copy(name = " ")))
    }
    @Test fun captorReadsOnlyOwnRecordsWithinScope() {
        CaptureState.entries.forEach { assertTrue(LocalCaptureAccess.canRead(actor, owner, site, it)) }
        assertFalse(LocalCaptureAccess.canRead(actor, UUID.randomUUID(), site, CaptureState.READY))
        assertFalse(LocalCaptureAccess.canRead(actor, owner, UUID.randomUUID(), CaptureState.READY))
    }
    @Test fun supervisorCannotReadAnotherPersonsDraftOrForeignSite() {
        val supervisor = actor.copy(id = UUID.randomUUID(), role = LocalRole.SUPERVISOR)
        assertTrue(LocalCaptureAccess.canRead(supervisor, owner, site, CaptureState.READY))
        assertFalse(LocalCaptureAccess.canRead(supervisor, owner, site, CaptureState.DRAFT))
        assertFalse(LocalCaptureAccess.canRead(supervisor, owner, UUID.randomUUID(), CaptureState.READY))
    }

    @Test fun supervisorCanSuperviseEventsWithinAssignedSites() {
        val supervisor = actor.copy(id = UUID.randomUUID(), role = LocalRole.SUPERVISOR)
        assertTrue(LocalCaptureAccess.canSupervise(supervisor, event))
        assertFalse(LocalCaptureAccess.canSupervise(supervisor.copy(siteIds = emptySet()), event))
        assertFalse(LocalCaptureAccess.canSupervise(actor, event))
    }

    @Test fun accessEventDifferentiatesCaptorAndSupervisor() {
        val supervisor = actor.copy(id = UUID.randomUUID(), role = LocalRole.SUPERVISOR)
        assertTrue(LocalCaptureAccess.canAccessEvent(actor, event))
        assertTrue(LocalCaptureAccess.canAccessEvent(supervisor, event))

        val foreignEvent = event.copy(siteId = UUID.randomUUID())
        assertFalse(LocalCaptureAccess.canAccessEvent(actor, foreignEvent))
        assertFalse(LocalCaptureAccess.canAccessEvent(supervisor, foreignEvent))
    }
}
