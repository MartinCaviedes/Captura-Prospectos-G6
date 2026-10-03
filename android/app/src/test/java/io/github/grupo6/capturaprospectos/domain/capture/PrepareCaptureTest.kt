package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class PrepareCaptureTest {
    private val actor = UUID.fromString("30000000-0000-4000-8000-000000000001")
    private val site = UUID.fromString("10000000-0000-4000-8000-000000000001")
    private val now = Instant.parse("2026-10-19T15:00:00Z")
    private val consent = ConsentEvidence("demo-v1", now.minusSeconds(10), actor, AcceptanceMode.DIRECT)
    private val coordinates = Coordinates(-33.0, -71.5)
    private val gps = CaptureLocation.Device(coordinates, now.minusSeconds(2), 20.0)
    private val offering = Offering(
        UUID.fromString("60000000-0000-4000-8000-000000000001"),
        UUID.fromString("50000000-0000-4000-8000-000000000001"), site, "demo-2026-v1", "Programa ficticio A", true,
    )
    private val input = CaptureInput("Persona Ficticia Alfa", "12.345.678-5", "alfa@example.invalid", "+56 9 0000 0000", establishment = "Establecimiento ficticio")
    private val service = PrepareCapture(ContactValidator(setOf("SECONDARY")))

    private fun prepare(
        input: CaptureInput = this.input,
        intent: SaveIntent = SaveIntent.CONFIRM_REGISTRATION,
        evidence: ConsentEvidence? = consent,
        location: CaptureLocation? = gps,
        ids: List<UUID> = listOf(offering.id),
        catalog: List<Offering> = listOf(offering),
    ) = service.prepare(input, intent, evidence, "demo-v1", actor, site, ids, catalog, location, coordinates, now)

    private fun hasIssue(result: PreparationResult, field: CaptureField) =
        result is PreparationResult.Invalid && result.issues.any { it.field == field }

    @Test fun consentRequiredForBothDraftAndRegistration() {
        SaveIntent.entries.forEach { assertEquals(PreparationResult.ConsentRequired, prepare(intent = it, evidence = null)) }
    }

    @Test fun rejectsWrongConsentVersionActorAndFutureTime() {
        listOf(consent.copy(version = "other"), consent.copy(recordedBy = UUID.randomUUID()), consent.copy(acceptedAt = now.plusSeconds(1))).forEach {
            assertTrue(hasIssue(prepare(evidence = it), CaptureField.CONSENT))
        }
    }

    @Test fun verbalConsentIsRecordedWithoutChangingItsMode() {
        val result = prepare(evidence = consent.copy(mode = AcceptanceMode.VERBAL)) as PreparationResult.Prepared
        assertEquals(AcceptanceMode.VERBAL, result.consent.mode)
    }

    @Test fun completeCaptureIsReadyAndNeverSynced() {
        val result = prepare() as PreparationResult.Prepared
        assertEquals(CaptureState.READY, result.state)
        assertEquals(TransportState.NOT_QUEUED, result.transportState)
        assertEquals("12345678-5", result.input.rut)
    }

    @Test fun missingLocationNeedsContextWithoutInventingCoordinates() {
        val result = prepare(location = null) as PreparationResult.Prepared
        assertEquals(CaptureState.NEEDS_CONTEXT, result.state)
        assertNull(result.location)
    }

    @Test fun explicitDraftRemainsDraftEvenWithCompleteFields() {
        assertEquals(CaptureState.DRAFT, (prepare(intent = SaveIntent.SAVE_DRAFT) as PreparationResult.Prepared).state)
    }

    @Test fun consentedIncompleteDraftCanBePrepared() {
        val result = prepare(input = CaptureInput(), intent = SaveIntent.SAVE_DRAFT, ids = emptyList(), location = null)
        assertEquals(CaptureState.DRAFT, (result as PreparationResult.Prepared).state)
    }

    @Test fun incompleteRegistrationAndMissingInterestAreRejected() {
        assertTrue(prepare(input = CaptureInput()) is PreparationResult.Invalid)
        assertTrue(hasIssue(prepare(ids = emptyList()), CaptureField.INTERESTS))
    }

    @Test fun duplicateOfferingsAndSameProgramAreRejected() {
        assertTrue(hasIssue(prepare(ids = listOf(offering.id, offering.id)), CaptureField.INTERESTS))
        val other = offering.copy(id = UUID.randomUUID())
        assertTrue(hasIssue(prepare(ids = listOf(offering.id, other.id), catalog = listOf(offering, other)), CaptureField.INTERESTS))
    }

    @Test fun missingDisabledForeignSiteAndAmbiguousCatalogEntriesAreRejected() {
        listOf(emptyList(), listOf(offering.copy(enabled = false)), listOf(offering.copy(siteId = UUID.randomUUID())), listOf(offering, offering)).forEach {
            assertTrue(hasIssue(prepare(catalog = it), CaptureField.INTERESTS))
        }
    }

    @Test fun gpsThresholdBoundariesAreInclusive() {
        assertTrue(prepare(location = gps.copy(measuredAt = now.minusSeconds(300), accuracyMeters = 100.0)) is PreparationResult.Prepared)
        assertTrue(hasIssue(prepare(location = gps.copy(measuredAt = now.minusSeconds(301))), CaptureField.LOCATION))
        assertTrue(hasIssue(prepare(location = gps.copy(accuracyMeters = 100.1)), CaptureField.LOCATION))
    }

    @Test fun gpsRejectsFutureTimeNanAndInvalidCoordinates() {
        listOf(gps.copy(measuredAt = now.plusSeconds(1)), gps.copy(accuracyMeters = Double.NaN), gps.copy(accuracyMeters = -1.0), gps.copy(coordinates = Coordinates(91.0, 0.0)), gps.copy(coordinates = Coordinates(0.0, Double.POSITIVE_INFINITY))).forEach {
            assertTrue(hasIssue(prepare(location = it), CaptureField.LOCATION))
        }
    }

    @Test fun eventFallbackRequiresExactDeclaredLocationAndActorConfirmation() {
        val event = CaptureLocation.EventConfirmed(coordinates, now, actor)
        assertTrue(prepare(location = event) is PreparationResult.Prepared)
        assertTrue(hasIssue(prepare(location = event.copy(confirmedBy = UUID.randomUUID())), CaptureField.LOCATION))
        assertTrue(hasIssue(prepare(location = event.copy(coordinates = Coordinates(0.0, 0.0))), CaptureField.LOCATION))
        assertTrue(hasIssue(prepare(location = event.copy(confirmedAt = now.plusSeconds(1))), CaptureField.LOCATION))
    }

    @Test fun preparationDoesNotMutateInputAndDoesNotExposeItInDescription() {
        val result = prepare() as PreparationResult.Prepared
        assertEquals("12.345.678-5", input.rut)
        assertFalse(result.toString().contains(input.fullName))
        assertFalse(coordinates.toString().contains("-33"))
    }
}
