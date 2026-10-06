package io.github.grupo6.capturaprospectos.data.local

import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class DemoSeedTest {
    @Test fun allAssignmentsRespectCaptorRoleAndSiteScope() {
        assertEquals(DemoSeed.profiles.size, DemoSeed.profiles.map { it.id }.toSet().size)
        DemoSeed.profiles.forEach { UUID.fromString(it.id); LocalRole.valueOf(it.role) }
        DemoSeed.events.forEach { assertTrue(Instant.parse(it.startsAt) < Instant.parse(it.endsAt)); ZoneId.of(it.timeZone) }
        DemoSeed.assignments.forEach { assignment ->
            val profile = DemoSeed.profiles.single { it.id == assignment.profileId }
            val event = DemoSeed.events.single { it.id == assignment.eventId }
            assertEquals("CAPTOR", profile.role)
            assertTrue(DemoSeed.scopes.any { it.profileId == profile.id && it.siteId == event.siteId })
        }
        DemoSeed.scopes.forEach { scope -> assertTrue(DemoSeed.sites.any { it.id == scope.siteId }); assertTrue(DemoSeed.profiles.any { it.id == scope.profileId }) }
    }
    @Test fun syntheticCatalogHasIndependentSitesOnlineAndParentMentions() {
        assertEquals("SYNTHETIC", DemoSeed.catalog.sourceKind)
        assertTrue(DemoSeed.CONSENT_TEXT.contains("ficticio"))
        assertTrue(DemoSeed.sites.size > 3)
        assertTrue(DemoSeed.programs.any { it.section == "DEMO_ONLINE" })
        assertTrue(DemoSeed.programs.any { it.section == "DEMO_PHYSICAL" })
        DemoSeed.mentions.forEach { m -> assertTrue(DemoSeed.programs.any { it.id == m.programId }); assertTrue(m.name.contains("ficticia")) }
        assertTrue(DemoSeed.programs.all { it.catalogVersion == DemoSeed.catalog.id && it.name.contains("ficticio") })
        assertTrue(DemoSeed.sites.all { it.catalogVersion == DemoSeed.catalog.id && it.name.contains("ficticia") })
        assertTrue(DemoSeed.events.all { it.latitude == null && it.longitude == null })
    }
}
