package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.text.Normalizer
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Autorización, revisión y reserva se resuelven dentro de la misma transacción. */
internal class RoomRecordRepository(
    private val db: CaptureDatabase,
    private val context: AdmissionContextSource,
    private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RecordRepository {
    private val admissionRepo = RoomAdmissionRepository(db, context, clock = clock, dispatcher = dispatcher)
    override suspend fun get(id: UUID): ReadAdmissionResult = admissionRepo.get(id)

    override suspend fun getMetrics(eventId: UUID): DeviceMetrics = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction DeviceMetrics(eventId, 0, 0, 0, 0, 0)
                val catalog = context.environment(eventId)
                val dao = db.captures()

                val rawRows = when (actor.role) {
                    LocalRole.CAPTOR -> dao.capturesByOwner(actor.id.toString())
                    LocalRole.SUPERVISOR -> {
                        val siteIds = actor.siteScopes.map { it.toString() }
                        if (siteIds.isEmpty()) emptyList() else dao.capturesBySitesExcludingDraft(siteIds)
                    }
                }

                val eventRows = rawRows.filter { it.eventId == eventId.toString() }
                val total = eventRows.size
                val ready = eventRows.count { it.state == CaptureState.READY.name }
                val draft = eventRows.count { it.state == CaptureState.DRAFT.name }
                val needsContext = eventRows.count { it.state == CaptureState.NEEDS_CONTEXT.name }
                val cancelled = eventRows.count { it.state == CaptureState.CANCELLED.name }

                val siteMetrics = if (actor.role == LocalRole.SUPERVISOR && catalog != null) {
                    val siteMap = catalog.sites.associateBy { it.id }
                    val groupedBySite = eventRows.groupBy { it.siteId }
                    actor.siteScopes.mapNotNull { siteId ->
                        val siteOpt = siteMap[siteId] ?: return@mapNotNull null
                        val siteRows = groupedBySite[siteId.toString()] ?: emptyList()
                        SiteMetrics(
                            siteId = siteId,
                            siteName = siteOpt.name,
                            totalCount = siteRows.size,
                            readyCount = siteRows.count { it.state == CaptureState.READY.name },
                            draftCount = siteRows.count { it.state == CaptureState.DRAFT.name },
                            needsContextCount = siteRows.count { it.state == CaptureState.NEEDS_CONTEXT.name },
                            cancelledCount = siteRows.count { it.state == CaptureState.CANCELLED.name },
                        )
                    }
                } else emptyList()

                DeviceMetrics(
                    eventId = eventId,
                    totalCaptures = total,
                    readyCount = ready,
                    draftCount = draft,
                    needsContextCount = needsContext,
                    cancelledCount = cancelled,
                    siteMetrics = siteMetrics,
                )
            }
        } catch (_: Exception) {
            DeviceMetrics(eventId, 0, 0, 0, 0, 0)
        }
    }

    override suspend fun editContext(id: UUID): RecordEditContextResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordEditContextResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(id.toString()) ?: return@withTransaction RecordEditContextResult.Unavailable
                if (!canAccess(actor, row) || row.contractVersion != "F2027_1" || row.state == "CANCELLED")
                    return@withTransaction RecordEditContextResult.Unavailable
                val old = row.toAdmission(dao.interests(row.id), dao.sites(row.id))
                val catalog = db.context()
                val eventRow = catalog.events().singleOrNull { it.id == row.eventId }
                    ?: return@withTransaction RecordEditContextResult.Unavailable
                val version = catalog.state()?.catalogVersion ?: return@withTransaction RecordEditContextResult.Unavailable
                val coordinates = if (eventRow.latitude == null && eventRow.longitude == null) null
                    else Coordinates(requireNotNull(eventRow.latitude), requireNotNull(eventRow.longitude))
                val event = CaptureEvent(old.eventId, old.siteId, eventRow.name,
                    Instant.parse(eventRow.startsAt), Instant.parse(eventRow.endsAt), java.time.ZoneId.of(eventRow.timeZone),
                    catalog.assignments(row.eventId).map { UUID.fromString(it.profileId) }.toSet(), coordinates)
                val retainedPrograms = old.programs.map { p -> ProgramOption(p.id, p.name, p.catalogVersion, p.section,
                    mentions = if (p.mentionId == null) emptyMap() else mapOf(p.mentionId to requireNotNull(p.mentionName))) }
                val programs = catalog.programs(version).map { p -> ProgramOption(UUID.fromString(p.id), p.name,
                    p.catalogVersion, p.section, p.enabled, catalog.mentions(p.id).associate { UUID.fromString(it.id) to it.name }) }
                val sites = catalog.sites(version).map { SiteOption(UUID.fromString(it.id), it.name, it.catalogVersion, it.enabled) }
                val consentText = dao.consent(old.consent.version)?.text ?: return@withTransaction RecordEditContextResult.Unavailable
                RecordEditContextResult.Available(AdmissionEnvironment(CaptureEnvironment(actor, event, old.deviceId,
                    emptyList(), old.consent.version, consentText),
                    retainedPrograms.map { oldProgram -> oldProgram.copy(mentions = programs.firstOrNull { it.id == oldProgram.id }?.mentions.orEmpty() + oldProgram.mentions) } + programs.filterNot { p -> retainedPrograms.any { it.id == p.id } },
                    old.sites + sites.filterNot { s -> old.sites.any { it.id == s.id } }),
                    eventRow.establishment?.let { io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion(it, eventRow.establishmentCommune) })
            }
        } catch (_: SQLiteException) { RecordEditContextResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { RecordEditContextResult.StorageUnavailable }
        catch (_: DateTimeException) { RecordEditContextResult.StorageUnavailable }
    }

    override suspend fun list(query: RecordQuery): RecordPageResult = withContext(dispatcher) {
        if (!query.isValid()) return@withContext RecordPageResult.InvalidQuery
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordPageResult.AccessDenied
                val filter = query.filter
                val search = normalize(filter.query.trim())
                val rutSearch = normalize(RutValidator.normalize(filter.query.trim()))
                val rows = db.captures().capturesForActor(actor.id.toString(), actor.siteIds.map(UUID::toString),
                    actor.role == LocalRole.SUPERVISOR).filter { row ->
                    val at = Instant.parse(row.capturedAt)
                    val id = UUID.fromString(row.id)
                    require(canAccess(actor, row))
                    require(row.contractVersion in setOf("F2027_1", "LEGACY_1"))
                    val state = CaptureState.valueOf(row.state)
                    require(row.revision > 0)
                    (filter.eventId == null || row.eventId == filter.eventId.toString()) &&
                        (filter.states.isEmpty() || state in filter.states) &&
                        (filter.fromInclusive == null || at >= filter.fromInclusive) &&
                        (filter.untilExclusive == null || at < filter.untilExclusive) &&
                        (search.isBlank() || normalize(row.fullName.orEmpty()).contains(search) ||
                            (rutSearch.isNotBlank() && normalize(row.rut).contains(rutSearch))) &&
                        (query.after == null || at < query.after.capturedAt || (at == query.after.capturedAt && id.toString() < query.after.id.toString()))
                }.sortedWith(compareByDescending<CaptureRow> { Instant.parse(it.capturedAt) }.thenByDescending { it.id })
                val page = rows.take(query.limit)
                val next = if (rows.size > query.limit) page.last().let {
                    RecordCursor(Instant.parse(it.capturedAt), UUID.fromString(it.id))
                } else null
                RecordPageResult.Page(page.map { row -> RecordSummary(UUID.fromString(row.id), row.revision,
                    Instant.parse(row.capturedAt), UUID.fromString(row.eventId), row.fullName.orEmpty().ifBlank { "Sin nombre" },
                    row.rut, CaptureState.valueOf(row.state), if (row.contractVersion == "F2027_1") RecordContract.F2027 else RecordContract.LEGACY)
                }, next)
            }
        } catch (_: SQLiteException) { RecordPageResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { RecordPageResult.StorageUnavailable }
        catch (_: DateTimeException) { RecordPageResult.StorageUnavailable }
    }

    override suspend fun cancel(command: CancelRecord): RecordMutationResult = withContext(dispatcher) {
        if (command.expectedRevision <= 0) return@withContext RecordMutationResult.InvalidRevision
        val reason = command.reason.trim()
        reasonError(reason, required = true)?.let { return@withContext it }
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordMutationResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(command.id.toString()) ?: return@withTransaction RecordMutationResult.Unavailable
                if (!canAccess(actor, row)) return@withTransaction RecordMutationResult.Unavailable
                if (row.revision != command.expectedRevision) return@withTransaction RecordMutationResult.RevisionConflict
                if (row.state == CaptureState.CANCELLED.name) return@withTransaction RecordMutationResult.InvalidTransition
                val next = row.revision + 1
                if (dao.updateState(row.id, CaptureState.CANCELLED.name, next, row.revision) != 1)
                    return@withTransaction RecordMutationResult.RevisionConflict
                dao.deleteKey(row.id)
                dao.insertAudit(AuditRow(UUID.randomUUID().toString(), row.id, actor.id.toString(), clock.instant().toString(),
                    "CANCELLED", next, row.revision, reason, "[\"state\"]"))
                RecordMutationResult.Saved(command.id, next, CaptureState.CANCELLED)
            }
        } catch (_: SQLiteException) { RecordMutationResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { RecordMutationResult.StorageUnavailable }
        catch (_: DateTimeException) { RecordMutationResult.StorageUnavailable }
    }

    override suspend fun edit(command: EditAdmission): RecordMutationResult = withContext(dispatcher) {
        if (command.expectedRevision <= 0) return@withContext RecordMutationResult.InvalidRevision
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordMutationResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(command.id.toString()) ?: return@withTransaction RecordMutationResult.Unavailable
                if (!canAccess(actor, row)) return@withTransaction RecordMutationResult.Unavailable
                if (row.revision != command.expectedRevision) return@withTransaction RecordMutationResult.RevisionConflict
                if (row.state == CaptureState.CANCELLED.name) return@withTransaction RecordMutationResult.InvalidTransition
                if (row.contractVersion != "F2027_1") return@withTransaction RecordMutationResult.UnsupportedContract
                val old = row.toAdmission(dao.interests(row.id), dao.sites(row.id))
                if (old.state != CaptureState.DRAFT && (command.intent != SaveIntent.CONFIRM_REGISTRATION ||
                        RutValidator.normalize(command.input.rut) != old.input.rut))
                    return@withTransaction RecordMutationResult.InvalidTransition
                val reason = command.reason?.trim().orEmpty()
                reasonError(reason, actor.role == LocalRole.SUPERVISOR && old.ownerId != actor.id)?.let { return@withTransaction it }
                val catalog = db.context()
                val version = catalog.state()?.catalogVersion
                val currentPrograms = version?.let { v -> catalog.programs(v).map { p ->
                    ProgramOption(UUID.fromString(p.id), p.name, p.catalogVersion, p.section, p.enabled,
                        catalog.mentions(p.id).associate { UUID.fromString(it.id) to it.name })
                } }.orEmpty()
                // Sólo selecciones históricas sin cambiar conservan su snapshot de catálogo.
                val retainedPrograms = old.programs.filter { p -> command.input.interests.any {
                    it.programId == p.id && it.mentionId == p.mentionId
                } }.map { p -> ProgramOption(p.id, p.name, p.catalogVersion, p.section,
                    mentions = if (p.mentionId != null) mapOf(p.mentionId to requireNotNull(p.mentionName)) else emptyMap()) }
                val availablePrograms = retainedPrograms + currentPrograms.filterNot { p -> retainedPrograms.any { it.id == p.id } }
                val retainedSites = old.sites.filter { it.id in command.input.preferredSiteIds }
                val currentSites = version?.let { v -> catalog.sites(v).map {
                    SiteOption(UUID.fromString(it.id), it.name, it.catalogVersion, it.enabled)
                } }.orEmpty()
                val availableSites = retainedSites + currentSites.filterNot { p -> retainedSites.any { it.id == p.id } }
                val event = catalog.events().singleOrNull { it.id == row.eventId }
                val declared = event?.let {
                    if (it.latitude == null && it.longitude == null) null
                    else Coordinates(requireNotNull(it.latitude), requireNotNull(it.longitude))
                }
                val newLocation = command.location?.takeUnless { it == old.location }
                // La evidencia original se conserva; sólo ubicación nueva se valida con edad actual.
                val prepare = PrepareAdmission(AdmissionValidator(setOfNotNull(old.input.contactChannel), setOfNotNull(old.input.modality)))
                val preparation = prepare.prepare(command.input, command.intent, old.consent, old.consent.version,
                    actor.id, availablePrograms, availableSites, newLocation, declared, clock.instant(),
                    old.eventTimeZone, consentActorId = old.consent.recordedBy)
                if (preparation is AdmissionPreparation.Invalid) return@withTransaction RecordMutationResult.Invalid(preparation.issues)
                if (preparation !is AdmissionPreparation.Prepared) return@withTransaction RecordMutationResult.Unavailable
                val location = newLocation ?: old.location
                val state = when {
                    command.intent == SaveIntent.SAVE_DRAFT -> CaptureState.DRAFT
                    location != null -> CaptureState.READY
                    else -> CaptureState.NEEDS_CONTEXT
                }
                if (state != CaptureState.DRAFT) {
                    val duplicate = dao.duplicate(row.eventId, preparation.input.rut)
                    if (duplicate != null && duplicate.id != row.id) return@withTransaction if (duplicate.ownerId == actor.id.toString())
                        RecordMutationResult.DuplicateOwn(UUID.fromString(duplicate.id)) else RecordMutationResult.DuplicateRestricted
                }
                val updated = row.edited(preparation.input, location, state)
                val fields = changedFields(old.input, preparation.input).toMutableList()
                if (old.state != state) fields += "state"
                if (old.location != location) fields += "location"
                if (fields.isEmpty()) return@withTransaction RecordMutationResult.Saved(command.id, row.revision, state)
                if (dao.updateCapture(updated) != 1) return@withTransaction RecordMutationResult.RevisionConflict
                dao.deleteInterests(row.id); dao.deleteSites(row.id)
                dao.insertInterests(preparation.programs.map { InterestRow(row.id, it.id.toString(), null,
                    it.catalogVersion, it.name, null, it.priority, it.section, it.mentionId?.toString(), it.mentionName) })
                dao.insertSites(preparation.sites.map { SitePreferenceRow(row.id, it.id.toString(), it.name, it.catalogVersion) })
                dao.deleteKey(row.id)
                if (state != CaptureState.DRAFT) dao.insertKey(ActiveKeyRow(row.eventId, preparation.input.rut, row.id))
                dao.insertAudit(AuditRow(UUID.randomUUID().toString(), row.id, actor.id.toString(), clock.instant().toString(),
                    "EDITED", updated.revision, row.revision, reason.takeIf(String::isNotEmpty),
                    fields.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")))
                RecordMutationResult.Saved(command.id, updated.revision, state)
            }
        } catch (_: SQLiteException) { RecordMutationResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { RecordMutationResult.StorageUnavailable }
        catch (_: DateTimeException) { RecordMutationResult.StorageUnavailable }
    }

    private fun canAccess(actor: LocalActor, row: CaptureRow) = LocalCaptureAccess.canRead(actor,
        UUID.fromString(row.ownerId), UUID.fromString(row.siteId), CaptureState.valueOf(row.state))
    private fun reasonError(reason: String, required: Boolean): RecordMutationResult? = when {
        required && reason.isEmpty() -> RecordMutationResult.ReasonRequired
        reason.isNotEmpty() && reason.codePointCount(0, reason.length) !in 5..300 -> RecordMutationResult.InvalidReason
        else -> null
    }
    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
}

private fun CaptureRow.edited(v: AdmissionInput, location: CaptureLocation?, state: CaptureState): CaptureRow {
    val device = location as? CaptureLocation.Device
    val event = location as? CaptureLocation.EventConfirmed
    val coordinates = device?.coordinates ?: event?.coordinates
    return copy(revision = revision + 1, state = state.name,
        fullName = listOf(v.givenNames, v.familyNames).filter(String::isNotBlank).joinToString(" "),
        givenNames = v.givenNames, familyNames = v.familyNames, rut = v.rut, email = v.email, phone = v.phone,
        birthDate = v.birthDate, residenceCommune = v.residenceCommune, currentActivity = v.currentActivity?.name,
        currentActivityOther = v.currentActivityOther, establishment = v.establishment, establishmentCommune = v.establishmentCommune,
        formPersonalOption = v.formPersonalOption?.name, preferredSchedule = v.preferredSchedule?.name,
        interestLevel = v.interestLevel?.name, motivation = v.motivation?.name, modality = v.modality, contactChannel = v.contactChannel, notes = v.notes,
        locationSource = when { device != null -> "DEVICE"; event != null -> "EVENT_CONFIRMED"; else -> null },
        latitude = coordinates?.latitude, longitude = coordinates?.longitude, measuredAt = device?.measuredAt?.toString(),
        accuracyMeters = device?.accuracyMeters, confirmedAt = event?.confirmedAt?.toString(), confirmedBy = event?.confirmedBy?.toString())
}

/** Únicamente nombres de campos; nunca valores personales en el rastro. */
private fun changedFields(a: AdmissionInput, b: AdmissionInput) = mapOf(
    "givenNames" to (a.givenNames to b.givenNames), "familyNames" to (a.familyNames to b.familyNames),
    "rut" to (a.rut to b.rut), "email" to (a.email to b.email), "phone" to (a.phone to b.phone),
    "birthDate" to (a.birthDate to b.birthDate), "residenceCommune" to (a.residenceCommune to b.residenceCommune),
    "currentActivity" to (a.currentActivity to b.currentActivity), "currentActivityOther" to (a.currentActivityOther to b.currentActivityOther),
    "establishment" to (a.establishment to b.establishment), "establishmentCommune" to (a.establishmentCommune to b.establishmentCommune),
    "formPersonalOption" to (a.formPersonalOption to b.formPersonalOption), "preferredSchedule" to (a.preferredSchedule to b.preferredSchedule),
    "interestLevel" to (a.interestLevel to b.interestLevel), "motivation" to (a.motivation to b.motivation),
    "modality" to (a.modality to b.modality), "contactChannel" to (a.contactChannel to b.contactChannel),
    "notes" to (a.notes to b.notes), "interests" to (a.interests to b.interests),
    "preferredSiteIds" to (a.preferredSiteIds.toSet() to b.preferredSiteIds.toSet()),
).filterValues { it.first != it.second }.keys.toList()
