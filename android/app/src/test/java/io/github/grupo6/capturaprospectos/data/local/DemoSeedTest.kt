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
    @Test fun catalogHasIndependentSitesOnlineAndParentMentionsWithoutDemoLabels() {
        assertEquals("OFFICIAL_REVIEWED", DemoSeed.catalog.sourceKind)
        assertFalse(DemoSeed.CONSENT_TEXT.contains("ficticio", ignoreCase = true))
        assertTrue(DemoSeed.sites.size >= 4)
        assertTrue(DemoSeed.programs.any { it.section == "Duoc Online" })
        assertTrue(DemoSeed.programs.any { it.section != "Duoc Online" })
        DemoSeed.mentions.forEach { m ->
            assertTrue(DemoSeed.programs.any { it.id == m.programId })
            assertFalse(m.name.contains("DEMO", ignoreCase = true))
        }
        assertTrue(DemoSeed.programs.all { it.catalogVersion == DemoSeed.catalog.id && !it.name.contains("DEMO", ignoreCase = true) })
        assertTrue(DemoSeed.sites.all { it.catalogVersion == DemoSeed.catalog.id && !it.name.contains("DEMO", ignoreCase = true) })
        assertTrue(DemoSeed.events.all { it.latitude == null && it.longitude == null })
    }
}
