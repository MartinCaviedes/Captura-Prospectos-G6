package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionContextSource
import io.github.grupo6.capturaprospectos.domain.capture.CaptureState
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole
import io.github.grupo6.capturaprospectos.domain.capture.RutValidator
import io.github.grupo6.capturaprospectos.domain.records.*
import java.text.Normalizer
import java.time.Clock
import java.time.DateTimeException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun list(query: RecordQuery): RecordPageResult = withContext(dispatcher) {
        if (!query.isValid()) return@withContext RecordPageResult.InvalidQuery
        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordPageResult.AccessDenied
                val dao = db.captures()

                val rawRows = when (actor.role) {
                    LocalRole.CAPTOR -> dao.capturesByOwner(actor.id.toString())
                    LocalRole.SUPERVISOR -> {
                        val siteIds = actor.siteScopes.map { it.toString() }
                        if (siteIds.isEmpty()) emptyList() else dao.capturesBySitesExcludingDraft(siteIds)
                    }
                }

                val filter = query.filter
                var filtered = rawRows

                if (filter.eventId != null) {
                    val eventStr = filter.eventId.toString()
                    filtered = filtered.filter { it.eventId == eventStr }
                }

                if (filter.states.isNotEmpty()) {
                    val stateNames = filter.states.map { it.name }.toSet()
                    filtered = filtered.filter { it.state in stateNames }
                }

                if (filter.fromInclusive != null) {
                    filtered = filtered.filter { Instant.parse(it.capturedAt) >= filter.fromInclusive }
                }

                if (filter.untilExclusive != null) {
                    filtered = filtered.filter { Instant.parse(it.capturedAt) < filter.untilExclusive }
                }

                if (filter.query.isNotBlank()) {
                    val queryNorm = normalize(filter.query)
                    filtered = filtered.filter { row ->
                        val name = normalize(row.fullName ?: listOfNotNull(row.givenNames, row.familyNames).joinToString(" "))
                        val rut = normalize(RutValidator.normalize(row.rut))
                        name.contains(queryNorm) || rut.contains(queryNorm)
                    }
                }

                val after = query.after
                if (after != null) {
                    val afterTime = after.capturedAt
                    val afterIdStr = after.id.toString()
                    filtered = filtered.filter { row ->
                        val t = Instant.parse(row.capturedAt)
                        t < afterTime || (t == afterTime && row.id < afterIdStr)
                    }
                }

                val paged = filtered.take(query.limit)
                val hasMore = filtered.size > query.limit
                val nextCursor = if (hasMore && paged.isNotEmpty()) {
                    val last = paged.last()
                    RecordCursor(Instant.parse(last.capturedAt), UUID.fromString(last.id))
                } else null

                val summaries = paged.map { row ->
                    RecordSummary(
                        id = UUID.fromString(row.id),
                        revision = row.revision,
                        capturedAt = Instant.parse(row.capturedAt),
                        eventId = UUID.fromString(row.eventId),
                        displayName = row.fullName ?: listOfNotNull(row.givenNames, row.familyNames).joinToString(" ").ifBlank { "Sin nombre" },
                        rut = row.rut,
                        state = CaptureState.valueOf(row.state),
                        contract = if (row.contractVersion == "F2027_1") RecordContract.F2027 else RecordContract.LEGACY,
                    )
                }

                RecordPageResult.Page(summaries, nextCursor)
            }
        } catch (_: SQLiteException) {
            RecordPageResult.StorageUnavailable
        } catch (_: IllegalArgumentException) {
            RecordPageResult.StorageUnavailable
        } catch (_: DateTimeException) {
            RecordPageResult.StorageUnavailable
        }
    }

    override suspend fun cancel(command: CancelRecord): RecordMutationResult = withContext(dispatcher) {
        val reason = command.reason.trim()
        if (reason.isEmpty()) return@withContext RecordMutationResult.ReasonRequired
        if (reason.length !in 5..300) return@withContext RecordMutationResult.InvalidReason
        if (command.expectedRevision <= 0) return@withContext RecordMutationResult.InvalidRevision

        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordMutationResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(command.id.toString()) ?: return@withTransaction RecordMutationResult.Unavailable

                val isOwner = row.ownerId == actor.id.toString()
                val inScope = UUID.fromString(row.siteId) in actor.siteScopes
                val canCancel = isOwner || (actor.role == LocalRole.SUPERVISOR && inScope && row.state != CaptureState.DRAFT.name)
                if (!canCancel) return@withTransaction RecordMutationResult.Unavailable

                if (row.revision != command.expectedRevision) return@withTransaction RecordMutationResult.RevisionConflict
                if (row.state == CaptureState.CANCELLED.name) return@withTransaction RecordMutationResult.InvalidTransition

                val nextRevision = row.revision + 1
                val updated = dao.updateState(row.id, CaptureState.CANCELLED.name, nextRevision, row.revision)
                if (updated == 0) return@withTransaction RecordMutationResult.RevisionConflict

                dao.deleteKey(row.id)
                dao.insertAudit(
                    AuditRow(
                        id = UUID.randomUUID().toString(),
                        captureId = row.id,
                        actorId = actor.id.toString(),
                        occurredAt = clock.instant().toString(),
                        action = "CANCELLED",
                        resultingRevision = nextRevision,
                    )
                )

                RecordMutationResult.Saved(command.id, nextRevision, CaptureState.CANCELLED)
            }
        } catch (_: SQLiteException) {
            RecordMutationResult.StorageUnavailable
        } catch (_: IllegalArgumentException) {
            RecordMutationResult.StorageUnavailable
        } catch (_: DateTimeException) {
            RecordMutationResult.StorageUnavailable
        }
    }

    override suspend fun edit(command: EditAdmission): RecordMutationResult = withContext(dispatcher) {
        if (command.expectedRevision <= 0) return@withContext RecordMutationResult.InvalidRevision

        try {
            db.withTransaction {
                val actor = context.currentActor() ?: return@withTransaction RecordMutationResult.Unavailable
                val dao = db.captures()
                val row = dao.capture(command.id.toString()) ?: return@withTransaction RecordMutationResult.Unavailable

                val isOwner = row.ownerId == actor.id.toString()
                val inScope = UUID.fromString(row.siteId) in actor.siteScopes
                val canEdit = isOwner || (actor.role == LocalRole.SUPERVISOR && inScope && row.state != CaptureState.DRAFT.name)
                if (!canEdit) return@withTransaction RecordMutationResult.Unavailable

                if (row.revision != command.expectedRevision) return@withTransaction RecordMutationResult.RevisionConflict
                if (row.contractVersion != "F2027_1") return@withTransaction RecordMutationResult.UnsupportedContract
                if (row.state == CaptureState.CANCELLED.name) return@withTransaction RecordMutationResult.InvalidTransition

                if (row.state == CaptureState.READY.name && command.input.rut != row.rut) {
                    return@withTransaction RecordMutationResult.InvalidTransition
                }

                if (actor.role == LocalRole.SUPERVISOR && !isOwner) {
                    val reason = command.reason?.trim().orEmpty()
                    if (reason.isEmpty()) return@withTransaction RecordMutationResult.ReasonRequired
                    if (reason.length !in 5..300) return@withTransaction RecordMutationResult.InvalidReason
                }

                val input = command.input
                val nextRevision = row.revision + 1
                val updatedRow = row.copy(
                    revision = nextRevision,
                    givenNames = input.givenNames.ifBlank { row.givenNames },
                    familyNames = input.familyNames.ifBlank { row.familyNames },
                    fullName = listOf(input.givenNames, input.familyNames).filter { it.isNotBlank() }.joinToString(" ").ifBlank { row.fullName },
                    email = input.email.ifBlank { row.email },
                    phone = input.phone.ifBlank { row.phone },
                    birthDate = input.birthDate.ifBlank { row.birthDate },
                    residenceCommune = input.residenceCommune.ifBlank { row.residenceCommune },
                    currentActivity = input.currentActivity?.name ?: row.currentActivity,
                    currentActivityOther = input.currentActivityOther ?: row.currentActivityOther,
                    establishment = input.establishment ?: row.establishment,
                    establishmentCommune = input.establishmentCommune ?: row.establishmentCommune,
                    formPersonalOption = input.formPersonalOption?.name ?: row.formPersonalOption,
                    preferredSchedule = input.preferredSchedule?.name ?: row.preferredSchedule,
                    interestLevel = input.interestLevel?.name ?: row.interestLevel,
                    motivation = input.motivation?.name ?: row.motivation,
                    notes = input.notes ?: row.notes,
                )

                val updatedCount = dao.updateCapture(updatedRow)
                if (updatedCount == 0) return@withTransaction RecordMutationResult.StorageUnavailable

                dao.insertAudit(
                    AuditRow(
                        id = UUID.randomUUID().toString(),
                        captureId = row.id,
                        actorId = actor.id.toString(),
                        occurredAt = clock.instant().toString(),
                        action = if (isOwner) "EDITED" else "SUPERVISOR_EDITED",
                        resultingRevision = nextRevision,
                    )
                )

                RecordMutationResult.Saved(command.id, nextRevision, CaptureState.valueOf(row.state))
            }
        } catch (_: SQLiteException) {
            RecordMutationResult.StorageUnavailable
        } catch (_: IllegalArgumentException) {
            RecordMutationResult.StorageUnavailable
        } catch (_: DateTimeException) {
            RecordMutationResult.StorageUnavailable
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(java.util.Locale.ROOT)
}
