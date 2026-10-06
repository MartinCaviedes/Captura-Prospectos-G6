package io.github.grupo6.capturaprospectos.domain.admission

import io.github.grupo6.capturaprospectos.domain.capture.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class PrepareAdmissionTest {
    private fun id(n: Int) = UUID(0, n.toLong())
    private val actor = id(100)
    private val now = Instant.parse("2026-10-06T15:00:00Z")
    private val zone = ZoneId.of("America/Santiago")
    private val consent = ConsentEvidence("demo-v1", now, actor, AcceptanceMode.DIRECT)
    private val programs = (1..4).map {
        ProgramOption(id(it), "Programa ficticio $it", "demo-v1", if (it == 2) "DEMO_ONLINE" else "DEMO_PHYSICAL",
            mentions = if (it == 3) mapOf(id(30) to "Mención ficticia") else emptyMap())
    }
    private val sites = (11..14).map { SiteOption(id(it), "Sede ficticia $it", "demo-v1") }
    private val valid = AdmissionInput(
        givenNames = "Persona Ficticia", familyNames = "Demostracion", rut = "12345678-5",
        email = "demo@example.invalid", phone = "+56900000000", birthDate = "2000-01-01",
        residenceCommune = "Comuna ficticia", currentActivity = CurrentActivity.SECONDARY_GRADUATE,
        interests = listOf(RankedInterest(id(1), 1)),
    )
    private fun prepare(
        input: AdmissionInput = valid, intent: SaveIntent = SaveIntent.CONFIRM_REGISTRATION,
        evidence: ConsentEvidence? = consent, catalog: List<ProgramOption> = programs,
        siteCatalog: List<SiteOption> = sites, location: CaptureLocation? = null,
        instant: Instant = now,
    ) = PrepareAdmission().prepare(input, intent, evidence, "demo-v1", actor, catalog, siteCatalog,
        location, null, instant, zone)

    private fun rejected(input: AdmissionInput, field: AdmissionField, code: ValidationCode? = null) {
        val result = prepare(input)
        assertTrue("Expected invalid result for $field", result is AdmissionPreparation.Invalid)
        assertTrue((result as AdmissionPreparation.Invalid).issues.any { it.field == field && (code == null || it.code == code) })
    }

    @Test fun optionalFieldsRemainAbsentAndLocationIsPending() {
        val p = prepare() as AdmissionPreparation.Prepared
        assertNull(p.input.formPersonalOption); assertNull(p.input.motivation); assertNull(p.input.preferredSchedule)
        assertNull(p.input.interestLevel); assertTrue(p.sites.isEmpty())
        assertEquals(CaptureState.NEEDS_CONTEXT, p.state)
        assertEquals(TransportState.NOT_QUEUED, p.transportState)
    }
    @Test fun requiresSeparateNamesWithoutDemandingTwoSurnames() {
        rejected(valid.copy(givenNames = ""), AdmissionField.GIVEN_NAMES, ValidationCode.REQUIRED)
        rejected(valid.copy(familyNames = ""), AdmissionField.FAMILY_NAMES, ValidationCode.REQUIRED)
        assertTrue(prepare(valid.copy(familyNames = "Perez")) is AdmissionPreparation.Prepared)
    }
    @Test fun birthIsRequiredRealAndNotFuture() {
        for (value in listOf("", "2001-02-29", "2027-01-01", "06/10/2000")) {
            rejected(valid.copy(birthDate = value), AdmissionField.BIRTH_DATE)
        }
        assertTrue(prepare(valid.copy(birthDate = "2000-02-29")) is AdmissionPreparation.Prepared)
    }
    @Test fun birthUsesEventLocalDateWithoutInventedAgeLimits() {
        val midnight = Instant.parse("2026-10-06T01:00:00Z") // 05/10 en Santiago.
        val evidence = consent.copy(acceptedAt = midnight)
        assertTrue(prepare(valid.copy(birthDate = "2026-10-06"), evidence = evidence, instant = midnight) is AdmissionPreparation.Invalid)
        assertTrue(prepare(valid.copy(birthDate = "2026-10-05"), evidence = evidence, instant = midnight) is AdmissionPreparation.Prepared)
        assertTrue(prepare(valid.copy(birthDate = "1900-01-01")) is AdmissionPreparation.Prepared)
    }
    @Test fun residenceAndActivityHaveNoDefaults() {
        rejected(valid.copy(residenceCommune = ""), AdmissionField.RESIDENCE_COMMUNE)
        rejected(valid.copy(currentActivity = null), AdmissionField.ACTIVITY)
        assertNull(AdmissionInput().currentActivity)
    }
    @Test fun fourthSecondaryRequiresEstablishmentOnlyOnConfirmation() {
        val input = valid.copy(currentActivity = CurrentActivity.FOURTH_SECONDARY)
        rejected(input, AdmissionField.ESTABLISHMENT)
        assertTrue(prepare(input.copy(establishment = "Colegio ficticio")) is AdmissionPreparation.Prepared)
        assertTrue(prepare(input, SaveIntent.SAVE_DRAFT) is AdmissionPreparation.Prepared)
    }
    @Test fun otherRequiresDescriptionButNotEstablishment() {
        rejected(valid.copy(currentActivity = CurrentActivity.OTHER), AdmissionField.ACTIVITY_OTHER)
        assertTrue(prepare(valid.copy(currentActivity = CurrentActivity.OTHER, currentActivityOther = "Trabajando")) is AdmissionPreparation.Prepared)
    }
    @Test fun dependentTextIsNotSilentlyDiscarded() {
        rejected(valid.copy(currentActivityOther = "Texto previo"), AdmissionField.ACTIVITY_OTHER)
        rejected(valid.copy(establishmentCommune = "Comuna ficticia"), AdmissionField.ESTABLISHMENT_COMMUNE)
    }
    @Test fun confirmationRequiresOneToThreePrograms() {
        rejected(valid.copy(interests = emptyList()), AdmissionField.INTERESTS)
        rejected(valid.copy(interests = (1..4).map { RankedInterest(id(it), it) }), AdmissionField.INTERESTS)
        assertTrue(prepare(valid.copy(interests = (1..3).map { RankedInterest(id(it), it) })) is AdmissionPreparation.Prepared)
    }
    @Test fun rejectsDuplicateProgramEvenWithDifferentPriority() {
        rejected(valid.copy(interests = listOf(RankedInterest(id(1), 1), RankedInterest(id(1), 2))), AdmissionField.INTERESTS, ValidationCode.DUPLICATE)
    }
    @Test fun prioritiesMustBeUniqueAndContiguous() {
        for (priorities in listOf(listOf(0, 1), listOf(1, 1), listOf(1, 3))) {
            rejected(valid.copy(interests = priorities.mapIndexed { i, p -> RankedInterest(id(i + 1), p) }), AdmissionField.INTERESTS, ValidationCode.FORMAT)
        }
    }
    @Test fun rankingIsPreservedIndependentOfInputOrder() {
        val p = prepare(valid.copy(interests = listOf(RankedInterest(id(1), 2), RankedInterest(id(2), 1)))) as AdmissionPreparation.Prepared
        assertEquals(listOf(id(2), id(1)), p.programs.map { it.id })
        assertEquals(listOf(1, 2), p.input.interests.map { it.priority })
    }
    @Test fun acceptsFourSitesAndDoesNotFilterByEventSite() {
        val p = prepare(valid.copy(preferredSiteIds = sites.map { it.id })) as AdmissionPreparation.Prepared
        assertEquals(4, p.sites.size)
        // No parámetro eventSiteId: la autorización de contexto pertenece al repositorio.
        assertTrue(prepare(valid.copy(preferredSiteIds = listOf(id(14)))) is AdmissionPreparation.Prepared)
    }
    @Test fun rejectsDuplicateUnknownDisabledAndAmbiguousSites() {
        rejected(valid.copy(preferredSiteIds = listOf(id(11), id(11))), AdmissionField.SITES, ValidationCode.DUPLICATE)
        rejected(valid.copy(preferredSiteIds = listOf(id(99))), AdmissionField.SITES, ValidationCode.UNAVAILABLE)
        assertTrue(prepare(valid.copy(preferredSiteIds = listOf(id(11))), siteCatalog = listOf(sites[0].copy(enabled = false))) is AdmissionPreparation.Invalid)
        assertTrue(prepare(valid.copy(preferredSiteIds = listOf(id(11))), siteCatalog = sites + sites[0]) is AdmissionPreparation.Invalid)
    }
    @Test fun onlineAndMixedProgramsDoNotRequireSiteOrSchedule() {
        assertTrue(prepare(valid.copy(interests = listOf(RankedInterest(id(2), 1)))) is AdmissionPreparation.Prepared)
        assertTrue(prepare(valid.copy(interests = listOf(RankedInterest(id(1), 1), RankedInterest(id(2), 2)))) is AdmissionPreparation.Prepared)
    }
    @Test fun mentionMustBelongToParentAndDoesNotAddAnotherCareer() {
        val p = prepare(valid.copy(interests = listOf(RankedInterest(id(3), 1, id(30))))) as AdmissionPreparation.Prepared
        assertEquals(1, p.programs.size); assertEquals("Mención ficticia", p.programs.single().mentionName)
        rejected(valid.copy(interests = listOf(RankedInterest(id(1), 1, id(30)))), AdmissionField.INTERESTS)
    }
    @Test fun rejectsUnknownDisabledOrAmbiguousPrograms() {
        rejected(valid.copy(interests = listOf(RankedInterest(id(99), 1))), AdmissionField.INTERESTS)
        assertTrue(prepare(catalog = listOf(programs[0].copy(enabled = false))) is AdmissionPreparation.Invalid)
        assertTrue(prepare(catalog = programs + programs[0]) is AdmissionPreparation.Invalid)
    }
    @Test fun noConsentCannotPrepareEvenDraft() {
        assertEquals(AdmissionPreparation.ConsentRequired, prepare(evidence = null))
        assertEquals(AdmissionPreparation.ConsentRequired, prepare(AdmissionInput(), SaveIntent.SAVE_DRAFT, null))
    }
    @Test fun consentMustMatchActorVersionAndTime() {
        for (e in listOf(consent.copy(version = "wrong"), consent.copy(recordedBy = id(99)), consent.copy(acceptedAt = now.plusSeconds(1)))) {
            val result = prepare(evidence = e) as AdmissionPreparation.Invalid
            assertTrue(result.issues.any { it.field == AdmissionField.CONSENT })
        }
    }
    @Test fun consentedIncompleteDraftIsNeverRegistration() {
        val p = prepare(AdmissionInput(), SaveIntent.SAVE_DRAFT) as AdmissionPreparation.Prepared
        assertEquals(CaptureState.DRAFT, p.state)
        assertTrue(p.programs.isEmpty())
    }
    @Test fun validLocationMakesReadyButStaleLocationIsRejected() {
        val location = CaptureLocation.Device(Coordinates(-33.0, -71.0), now, 20.0)
        assertEquals(CaptureState.READY, (prepare(location = location) as AdmissionPreparation.Prepared).state)
        assertTrue(prepare(location = location.copy(measuredAt = now.minusSeconds(301))) is AdmissionPreparation.Invalid)
    }
    @Test fun optionalChoiceNotStatedIsDistinctFromAbsenceAndMotivationIsSingle() {
        val p = prepare(valid.copy(formPersonalOption = PersonalOption.NOT_STATED, motivation = Motivation.VOCATION)) as AdmissionPreparation.Prepared
        assertEquals(PersonalOption.NOT_STATED, p.input.formPersonalOption)
        assertEquals(Motivation.VOCATION, p.input.motivation)
        assertEquals(4, Motivation.entries.size)
    }
    @Test fun normalizesContactsAndRejectsBadFormatsAndUnknownChannel() {
        val p = prepare(valid.copy(givenNames = "  Persona Ficticia  ", rut = "12.345.678-5", phone = "+56 9 0000 0000", establishment = "  ")) as AdmissionPreparation.Prepared
        assertEquals("Persona Ficticia", p.input.givenNames); assertEquals("12345678-5", p.input.rut)
        assertEquals("+56900000000", p.input.phone); assertNull(p.input.establishment)
        rejected(valid.copy(email = "a..b@example.invalid"), AdmissionField.EMAIL)
        rejected(valid.copy(phone = "123"), AdmissionField.PHONE)
        rejected(valid.copy(rut = "12345678-0"), AdmissionField.RUT)
        rejected(valid.copy(contactChannel = "invented"), AdmissionField.CHANNEL)
    }
    @Test fun optionalModalityUsesConfiguredCatalogWithoutConstrainingInterests() {
        rejected(valid.copy(modality = "invented"), AdmissionField.MODALITY)
        val validator = AdmissionValidator(modalities = setOf("DEMO_MODE"))
        assertTrue(validator.validate(valid.copy(modality = " DEMO_MODE "), SaveIntent.CONFIRM_REGISTRATION,
            now.atZone(zone).toLocalDate()).isEmpty())
        assertNull(valid.modality)
    }
    @Test fun snapshotDoesNotChangeWhenOriginalSelectionsAreMutated() {
        val selected = mutableListOf(RankedInterest(id(1), 1), RankedInterest(id(2), 2))
        val selectedSites = mutableListOf(id(11), id(12))
        val p = prepare(valid.copy(interests = selected, preferredSiteIds = selectedSites)) as AdmissionPreparation.Prepared
        selected.clear(); selectedSites.clear()
        assertEquals(2, p.input.interests.size); assertEquals(2, p.input.preferredSiteIds.size)
        (p.programs as MutableList).clear()
        assertEquals(2, p.programs.size)
    }
    @Test fun limitsApplyToDraftAndErrorsDoNotExposePersonalValues() {
        val result = prepare(valid.copy(givenNames = "x".repeat(121)), SaveIntent.SAVE_DRAFT) as AdmissionPreparation.Invalid
        assertTrue(result.issues.any { it.field == AdmissionField.GIVEN_NAMES })
        assertFalse(valid.toString().contains(valid.rut))
        assertFalse(prepare().toString().contains(valid.email))
        assertFalse(result.toString().contains(valid.givenNames))
    }
}
