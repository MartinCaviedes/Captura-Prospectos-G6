package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.DateTimeException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomAdmissionRepository(
    private val db: CaptureDatabase, private val context: AdmissionContextSource,
    private val prepare: PrepareAdmission = PrepareAdmission(),
    private val clock: Clock = Clock.systemUTC(), private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AdmissionRepository {
    override suspend fun create(request: NewAdmission): CreateAdmissionResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction CreateAdmissionResult.AccessDenied
                val catalog = context.environment(request.eventId) ?: return@withTransaction CreateAdmissionResult.AccessDenied
                val env = catalog.context
                if (env.actor != actor || env.event.id != request.eventId || !LocalCaptureAccess.canCreate(actor, env.event))
                    return@withTransaction CreateAdmissionResult.AccessDenied
                val now = clock.instant()
                val prepared = prepare.prepare(request.input, request.intent, request.consent, env.consentVersion,
                    actor.id, catalog.programs, catalog.sites, request.location, env.event.location, now, env.event.timeZone)
                when (prepared) {
                    AdmissionPreparation.ConsentRequired -> return@withTransaction CreateAdmissionResult.ConsentRequired
                    is AdmissionPreparation.Invalid -> return@withTransaction CreateAdmissionResult.Invalid(prepared.issues)
                    is AdmissionPreparation.Prepared -> {
                        val dao = db.captures()
                        if (prepared.state != CaptureState.DRAFT) {
                            val duplicate = dao.duplicate(env.event.id.toString(), prepared.input.rut)
                            if (duplicate != null) return@withTransaction if (duplicate.ownerId == actor.id.toString())
                                CreateAdmissionResult.DuplicateOwn(UUID.fromString(duplicate.id)) else CreateAdmissionResult.DuplicateRestricted
                        }
                        val oldText = dao.consent(env.consentVersion)
                        if (env.consentText.isBlank() || (oldText != null && oldText.text != env.consentText))
                            return@withTransaction CreateAdmissionResult.Invalid(listOf(AdmissionIssue(AdmissionField.CONSENT, ValidationCode.INVALID_EVIDENCE)))
                        if (oldText == null) dao.insertConsent(ConsentTextRow(env.consentVersion, env.consentText))
                        val id = UUID.randomUUID()
                        dao.insertCapture(prepared.toAdmissionRow(id, env, now))
                        dao.insertInterests(prepared.programs.map { InterestRow(id.toString(), it.id.toString(), null,
                            it.catalogVersion, it.name, null, it.priority, it.section, it.mentionId?.toString(), it.mentionName) })
                        dao.insertSites(prepared.sites.map { SitePreferenceRow(id.toString(), it.id.toString(), it.name, it.catalogVersion) })
                        if (prepared.state != CaptureState.DRAFT) dao.insertKey(ActiveKeyRow(env.event.id.toString(), prepared.input.rut, id.toString()))
                        dao.insertAudit(AuditRow(UUID.randomUUID().toString(), id.toString(), actor.id.toString(), now.toString(), "CREATED", 1))
                        CreateAdmissionResult.Saved(id, 1, prepared.state)
                    }
                }
            }
        } catch (_: SQLiteException) { CreateAdmissionResult.StorageUnavailable }
    }

    // Fallos de formato se traducen sin reset; no capturar IllegalStateException/CancellationException.
    override suspend fun get(id: UUID): ReadAdmissionResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction ReadAdmissionResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(id.toString()) ?: return@withTransaction ReadAdmissionResult.Unavailable
                if (!LocalCaptureAccess.canRead(actor, UUID.fromString(row.ownerId), UUID.fromString(row.siteId), CaptureState.valueOf(row.state)))
                    return@withTransaction ReadAdmissionResult.Unavailable
                when (row.contractVersion) {
                    "LEGACY_1" -> ReadAdmissionResult.Legacy(row.toDomain(dao.interests(row.id)))
                    "F2027_1" -> ReadAdmissionResult.Found(row.toAdmission(dao.interests(row.id), dao.sites(row.id)))
                    else -> ReadAdmissionResult.StorageUnavailable
                }
            }
        } catch (_: SQLiteException) { ReadAdmissionResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { ReadAdmissionResult.StorageUnavailable }
        catch (_: DateTimeException) { ReadAdmissionResult.StorageUnavailable }

    }
}

private fun AdmissionPreparation.Prepared.toAdmissionRow(id: UUID, env: CaptureEnvironment, now: Instant): CaptureRow {
    val v = input
    val device = location as? CaptureLocation.Device
    val event = location as? CaptureLocation.EventConfirmed
    val coords = device?.coordinates ?: event?.coordinates
    return CaptureRow(id.toString(), 1, state.name, env.actor.id.toString(), env.event.siteId.toString(), env.event.id.toString(),
        env.deviceId.toString(), now.toString(), env.event.name, env.event.timeZone.id,
        listOf(v.givenNames, v.familyNames).filter { it.isNotBlank() }.joinToString(" "), v.rut, v.email, v.phone,
        null, v.establishment, v.contactChannel, v.notes, consent.version, consent.acceptedAt.toString(),
        consent.recordedBy.toString(), consent.mode.name,
        when { device != null -> "DEVICE"; event != null -> "EVENT_CONFIRMED"; else -> null }, coords?.latitude, coords?.longitude,
        device?.measuredAt?.toString(), device?.accuracyMeters, event?.confirmedAt?.toString(), event?.confirmedBy?.toString(),
        "F2027_1", v.givenNames, v.familyNames, v.birthDate, v.residenceCommune, v.currentActivity?.name, v.currentActivityOther,
        v.establishmentCommune, v.formPersonalOption?.name, v.preferredSchedule?.name, v.interestLevel?.name, v.motivation?.name, v.modality)
}

private fun CaptureRow.toAdmission(interests: List<InterestRow>, preferences: List<SitePreferenceRow>): StoredAdmission {
    val programs = interests.map { SelectedProgram(UUID.fromString(it.programId), it.programName, it.catalogVersion,
        requireNotNull(it.section), requireNotNull(it.priority), it.mentionId?.let(UUID::fromString), it.mentionName) }
    val sites = preferences.map { SiteOption(UUID.fromString(it.siteId), it.name, it.catalogVersion) }
    val input = AdmissionInput(
        givenNames = requireNotNull(givenNames), familyNames = requireNotNull(familyNames), rut = rut, email = email, phone = phone,
        birthDate = requireNotNull(birthDate), residenceCommune = requireNotNull(residenceCommune),
        currentActivity = currentActivity?.let(CurrentActivity::valueOf), currentActivityOther = currentActivityOther,
        establishment = establishment, establishmentCommune = establishmentCommune,
        formPersonalOption = formPersonalOption?.let(PersonalOption::valueOf), preferredSchedule = preferredSchedule?.let(PreferredSchedule::valueOf),
        interestLevel = interestLevel?.let(InterestLevel::valueOf), motivation = motivation?.let(Motivation::valueOf),
        modality = modality, contactChannel = contactChannel, notes = notes,
        interests = programs.map { RankedInterest(it.id, it.priority, it.mentionId) }, preferredSiteIds = sites.map { it.id },
    )
    val location = when (locationSource) {
        "DEVICE" -> CaptureLocation.Device(Coordinates(requireNotNull(latitude), requireNotNull(longitude)), Instant.parse(requireNotNull(measuredAt)), requireNotNull(accuracyMeters))
        "EVENT_CONFIRMED" -> CaptureLocation.EventConfirmed(Coordinates(requireNotNull(latitude), requireNotNull(longitude)), Instant.parse(requireNotNull(confirmedAt)), UUID.fromString(requireNotNull(confirmedBy)))
        null -> null
        else -> throw IllegalArgumentException("Fuente de ubicación desconocida")
    }
    return StoredAdmission(UUID.fromString(id), revision, UUID.fromString(ownerId), UUID.fromString(siteId), UUID.fromString(eventId),
        UUID.fromString(deviceId), Instant.parse(capturedAt), eventName, ZoneId.of(eventTimeZone), input, requireNotNull(fullName),
        ConsentEvidence(consentVersion, Instant.parse(acceptedAt), UUID.fromString(recordedBy), AcceptanceMode.valueOf(acceptanceMode)),
        programs, sites, location, CaptureState.valueOf(state))
}
