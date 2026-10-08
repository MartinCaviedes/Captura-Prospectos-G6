package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.capture.*
import java.time.DateTimeException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomCaptureRepository(
    private val db: CaptureDatabase,
    private val context: CaptureContextSource,
    private val prepare: PrepareCapture,
    private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CaptureRepository {
    override suspend fun create(request: NewCapture): CreateCaptureResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction CreateCaptureResult.AccessDenied
                val environment = context.environment(request.eventId) ?: return@withTransaction CreateCaptureResult.AccessDenied
                val event = environment.event
                if (environment.actor != actor || event.id != request.eventId || !LocalCaptureAccess.canCreate(actor, event)) {
                    return@withTransaction CreateCaptureResult.AccessDenied
                }
                val now = clock.instant()
                val result = prepare.prepare(request.input, request.intent, request.consent,
                    environment.consentVersion, actor.id, event.siteId, request.offeringIds,
                    environment.offerings, request.location, event.location, now)
                when (result) {
                    PreparationResult.ConsentRequired -> return@withTransaction CreateCaptureResult.ConsentRequired
                    is PreparationResult.Invalid -> return@withTransaction CreateCaptureResult.Invalid(result.issues)
                    is PreparationResult.Prepared -> {
                        val dao = db.captures()
                        if (result.state != CaptureState.DRAFT) {
                            val duplicate = dao.duplicate(event.id.toString(), result.input.rut)
                            if (duplicate != null) return@withTransaction if (duplicate.ownerId == actor.id.toString()) {
                                CreateCaptureResult.DuplicateOwn(UUID.fromString(duplicate.id))
                            } else CreateCaptureResult.DuplicateRestricted
                        }
                        if (environment.consentText.isBlank()) return@withTransaction CreateCaptureResult.Invalid(
                            listOf(FieldIssue(CaptureField.CONSENT, ValidationCode.INVALID_EVIDENCE)))
                        val existingText = dao.consent(environment.consentVersion)
                        if (existingText != null && existingText.text != environment.consentText) {
                            return@withTransaction CreateCaptureResult.Invalid(listOf(FieldIssue(CaptureField.CONSENT, ValidationCode.INVALID_EVIDENCE)))
                        }
                        if (existingText == null) dao.insertConsent(ConsentTextRow(environment.consentVersion, environment.consentText))
                        val id = UUID.randomUUID()
                        dao.insertCapture(result.toRow(id, environment, now))
                        dao.insertInterests(result.interests.map {
                            InterestRow(id.toString(), it.programId.toString(), it.id.toString(), it.catalogVersion, it.programName, it.modality)
                        })
                        if (result.state != CaptureState.DRAFT) dao.insertKey(ActiveKeyRow(event.id.toString(), result.input.rut, id.toString()))
                        dao.insertAudit(AuditRow(UUID.randomUUID().toString(), id.toString(), actor.id.toString(), now.toString(), "CREATED", 1))
                        // withTransaction no devuelve este éxito al llamador hasta completar el commit.
                        CreateCaptureResult.Saved(id, 1, result.state)
                    }
                }
            }
        } catch (_: SQLiteException) { CreateCaptureResult.StorageUnavailable }
    }

    // Fallos de formato se traducen sin reset; no capturar IllegalStateException/CancellationException.
    override suspend fun get(id: UUID): ReadCaptureResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction ReadCaptureResult.Unavailable
                val row = db.captures().capture(id.toString()) ?: return@withTransaction ReadCaptureResult.Unavailable
                if (row.contractVersion != "LEGACY_1") return@withTransaction ReadCaptureResult.Unavailable
                val state = CaptureState.valueOf(row.state)
                if (!LocalCaptureAccess.canRead(actor, UUID.fromString(row.ownerId), UUID.fromString(row.siteId), state)) {
                    return@withTransaction ReadCaptureResult.Unavailable
                }
                ReadCaptureResult.Found(row.toDomain(db.captures().interests(row.id)))
            }
        } catch (_: SQLiteException) { ReadCaptureResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { ReadCaptureResult.StorageUnavailable }
        catch (_: DateTimeException) { ReadCaptureResult.StorageUnavailable }
    }
}

private fun PreparationResult.Prepared.toRow(id: UUID, environment: CaptureEnvironment, now: Instant): CaptureRow {
    val device = location as? CaptureLocation.Device
    val event = location as? CaptureLocation.EventConfirmed
    val coordinates = device?.coordinates ?: event?.coordinates
    return CaptureRow(id.toString(), 1, state.name, environment.actor.id.toString(), environment.event.siteId.toString(),
        environment.event.id.toString(), environment.deviceId.toString(), now.toString(), environment.event.name, environment.event.timeZone.id,
        input.fullName, input.rut, input.email, input.phone, input.educationLevel, input.establishment, input.contactChannel, input.notes,
        consent.version, consent.acceptedAt.toString(), consent.recordedBy.toString(), consent.mode.name,
        when { device != null -> "DEVICE"; event != null -> "EVENT_CONFIRMED"; else -> null },
        coordinates?.latitude, coordinates?.longitude, device?.measuredAt?.toString(), device?.accuracyMeters,
        event?.confirmedAt?.toString(), event?.confirmedBy?.toString())
}

internal fun CaptureRow.toDomain(interests: List<InterestRow>): StoredCapture {
    val location = when (locationSource) {
        "DEVICE" -> CaptureLocation.Device(Coordinates(requireNotNull(latitude), requireNotNull(longitude)), Instant.parse(requireNotNull(measuredAt)), requireNotNull(accuracyMeters))
        "EVENT_CONFIRMED" -> CaptureLocation.EventConfirmed(Coordinates(requireNotNull(latitude), requireNotNull(longitude)), Instant.parse(requireNotNull(confirmedAt)), UUID.fromString(requireNotNull(confirmedBy)))
        null -> null
        else -> throw IllegalArgumentException("Fuente de ubicación local desconocida")
    }
    return StoredCapture(UUID.fromString(id), revision, UUID.fromString(ownerId), UUID.fromString(siteId), UUID.fromString(eventId),
        UUID.fromString(deviceId), Instant.parse(capturedAt), eventName, ZoneId.of(eventTimeZone),
        CaptureInput(requireNotNull(fullName), rut, email, phone, requireNotNull(educationLevel), requireNotNull(establishment), contactChannel, notes),
        ConsentEvidence(consentVersion, Instant.parse(acceptedAt), UUID.fromString(recordedBy), AcceptanceMode.valueOf(acceptanceMode)),
        interests.map { Offering(UUID.fromString(requireNotNull(it.offeringId)), UUID.fromString(it.programId), UUID.fromString(siteId), it.catalogVersion, it.programName, true, it.modality) }, location, CaptureState.valueOf(state))
}
