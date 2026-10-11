package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import java.time.DateTimeException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Contexto demo cifrado. Inicialización explícita e idempotente; no se ejecuta al migrar ni crea capturas. */
internal class RoomDemoContextRepository(
    private val db: CaptureDatabase, private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DemoContextRepository {
    private suspend fun change(block: () -> ContextChange): ContextChange = withContext(dispatcher) {
        try { db.withTransaction { block() } } catch (_: SQLiteException) { ContextChange.STORAGE_UNAVAILABLE }
        catch (_: IllegalArgumentException) { ContextChange.STORAGE_UNAVAILABLE }
        catch (_: DateTimeException) { ContextChange.STORAGE_UNAVAILABLE }
    }
    override suspend fun initialize(): ContextChange = change {
        val dao = db.context()
        val current = dao.state()
        if (current != null) return@change if (current.seedVersion == DemoSeed.VERSION) ContextChange.ALREADY_INITIALIZED else ContextChange.INCOMPATIBLE_SEED
        if (dao.existingRows() != 0) return@change ContextChange.INCOMPATIBLE_SEED
        val consent = db.captures().consent(DemoSeed.CONSENT)
        if (consent != null && consent.text != DemoSeed.CONSENT_TEXT) return@change ContextChange.INCOMPATIBLE_SEED
        if (consent == null) db.captures().insertConsent(ConsentTextRow(DemoSeed.CONSENT, DemoSeed.CONSENT_TEXT))
        dao.insertCatalog(DemoSeed.catalog); dao.insertSites(DemoSeed.sites); dao.insertProfiles(DemoSeed.profiles)
        dao.insertScopes(DemoSeed.scopes); dao.insertEvents(DemoSeed.events); dao.insertAssignments(DemoSeed.assignments)
        dao.insertPrograms(DemoSeed.programs); dao.insertMentions(DemoSeed.mentions)
        dao.insertState(DemoContextRow(seedVersion = DemoSeed.VERSION, deviceId = UUID.randomUUID().toString(),
            installedAt = clock.instant().toString(), catalogVersion = DemoSeed.CATALOG, consentVersion = DemoSeed.CONSENT, profileId = null, eventId = null))
        ContextChange.APPLIED
    }
    override suspend fun snapshot(): DemoContextState = withContext(dispatcher) {
        try { db.withTransaction { readSnapshot()?.let(DemoContextState::Ready) ?: DemoContextState.NotInitialized } }
        catch (_: SQLiteException) { DemoContextState.StorageUnavailable }
        catch (_: IllegalArgumentException) { DemoContextState.StorageUnavailable }
        catch (_: DateTimeException) { DemoContextState.StorageUnavailable }
    }
    // Catálogos/perfiles son inmutables en este incremento. Mutaciones posteriores deberán ampliar invalidación.
    override fun observe(): Flow<DemoContextState> = db.context().observeState().map { snapshot() }.catch {
        if (it is SQLiteException) emit(DemoContextState.StorageUnavailable) else throw it
    }
    override suspend fun selectProfile(id: UUID): ContextChange = change {
        val dao = db.context()
        if (dao.state() == null) return@change ContextChange.NOT_INITIALIZED
        if (dao.profiles().none { it.id == id.toString() }) return@change ContextChange.INVALID_SELECTION
        dao.select(id.toString(), null) // Cambiar perfil no reasigna ni elimina capturas.
        ContextChange.APPLIED
    }
    override suspend fun selectEvent(id: UUID): ContextChange = change {
        val snapshot = readSnapshot() ?: return@change ContextChange.NOT_INITIALIZED
        if (snapshot.availableEvents.none { it.id == id }) return@change ContextChange.INVALID_SELECTION
        db.context().select(requireNotNull(snapshot.currentProfile).actor.id.toString(), id.toString())
        ContextChange.APPLIED
    }
    override suspend fun clearSelection(): ContextChange = change {
        if (db.context().state() == null) return@change ContextChange.NOT_INITIALIZED
        db.context().select(null, null)
        ContextChange.APPLIED
    }
    private suspend fun <T> sourceRead(block: () -> T): T = withContext(dispatcher) {
        try { db.withTransaction { block() } }
        catch (_: IllegalArgumentException) { throw SQLiteException("Contexto local inválido") }
        catch (_: DateTimeException) { throw SQLiteException("Fecha del contexto local inválida") }
    }
    override suspend fun currentActor(): LocalActor? = sourceRead { readSnapshot()?.currentProfile?.actor }
    override suspend fun environment(eventId: UUID): AdmissionEnvironment? = sourceRead {
        val snapshot = readSnapshot() ?: return@sourceRead null
        val actor = snapshot.currentProfile?.actor ?: return@sourceRead null
        val event = snapshot.selectedEvent?.takeIf { it.id == eventId } ?: return@sourceRead null
        if (!LocalCaptureAccess.canCreate(actor, event)) return@sourceRead null
        AdmissionEnvironment(CaptureEnvironment(actor, event, snapshot.deviceId, emptyList(), snapshot.consentVersion, snapshot.consentText),
            snapshot.catalog.programs, snapshot.catalog.sites)
    }
    private fun readSnapshot(): DemoContextSnapshot? {
        val dao = db.context()
        val state = dao.state() ?: return null
        require(state.seedVersion == DemoSeed.VERSION)
        val profiles = dao.profiles().map { DemoProfile(LocalActor(UUID.fromString(it.id), LocalRole.valueOf(it.role),
            dao.scopes(it.id).map { scope -> UUID.fromString(scope.siteId) }.toSet()), it.displayName) }
        val profile = state.profileId?.let { id -> requireNotNull(profiles.singleOrNull { it.actor.id.toString() == id }) }
        val rows = dao.events()
        val available = rows.map { row ->
            val coordinates = if (row.latitude == null && row.longitude == null) null else Coordinates(requireNotNull(row.latitude), requireNotNull(row.longitude)).also { require(it.isValid()) }
            CaptureEvent(UUID.fromString(row.id), UUID.fromString(row.siteId), row.name, Instant.parse(row.startsAt), Instant.parse(row.endsAt), ZoneId.of(row.timeZone),
                dao.assignments(row.id).map { UUID.fromString(it.profileId) }.toSet(), coordinates)
        }.filter { profile != null && LocalCaptureAccess.canSelectEvent(profile.actor, it) }
        val selected = state.eventId?.let { id -> UUID.fromString(id); available.singleOrNull { it.id.toString() == id } }
        val eventRow = rows.singleOrNull { it.id == selected?.id.toString() }
        val suggestion = eventRow?.establishment?.let { EstablishmentSuggestion(it, eventRow.establishmentCommune) }
        val version = requireNotNull(dao.catalog(state.catalogVersion))
        require(version.sourceKind == "SYNTHETIC")
        val catalog = DemoCatalog(version.id, version.sourceKind, version.sourceReference, version.academicYear,
            dao.programs(version.id).map { ProgramOption(UUID.fromString(it.id), it.name, it.catalogVersion, it.section, it.enabled,
                dao.mentions(it.id).associate { mention -> UUID.fromString(mention.id) to mention.name }) },
            dao.sites(version.id).map { SiteOption(UUID.fromString(it.id), it.name, it.catalogVersion, it.enabled) })
        val consent = requireNotNull(db.captures().consent(state.consentVersion))
        return DemoContextSnapshot(UUID.fromString(state.deviceId), profiles, profile, available, selected, suggestion, catalog, consent.version, consent.text)
    }

    /** Autoriza origen/destino y asignaciones en la misma transacción que escribe el evento. */
    override suspend fun saveEvent(command: SaveDemoEvent): SaveEventResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val snapshot=readSnapshot() ?: return@withTransaction SaveEventResult.Unauthorized
                val actor=snapshot.currentProfile?.actor ?: return@withTransaction SaveEventResult.Unauthorized
                val dao=db.context()
                if(actor.role != LocalRole.SUPERVISOR || command.siteId !in actor.siteIds)
                    return@withTransaction SaveEventResult.Unauthorized
                val existing=command.eventId?.let { dao.event(it.toString()) }
                if(command.eventId != null && (existing==null || UUID.fromString(existing.siteId) !in actor.siteIds))
                    return@withTransaction SaveEventResult.Unauthorized
                if(snapshot.catalog.sites.none { it.id==command.siteId && it.enabled }) return@withTransaction SaveEventResult.Unauthorized
                val issues=eventIssues(command).toMutableList()
                val captors=snapshot.profiles.filter { it.actor.role==LocalRole.CAPTOR && command.siteId in it.actor.siteIds }.map { it.actor.id }.toSet()
                if(!captors.containsAll(command.assignedCaptorIds)) issues += "Sólo se asignan captadores habilitados para la sede elegida."
                if(issues.isNotEmpty()) return@withTransaction SaveEventResult.Invalid(issues)
                val id=command.eventId ?: UUID.randomUUID()
                val row=DemoEventRow(id.toString(),command.siteId.toString(),command.name.trim(),command.startsAt.toString(),command.endsAt.toString(),command.timeZone.id,
                    command.coordinates?.latitude,command.coordinates?.longitude,command.establishment?.trim()?.ifEmpty { null },command.establishmentCommune?.trim()?.ifEmpty { null })
                if(existing==null) dao.insertEvent(row) else dao.updateEvent(row)
                dao.deleteAssignments(id.toString())
                dao.insertAssignments(command.assignedCaptorIds.map { DemoAssignmentRow(id.toString(),it.toString()) })
                // Invalida el contexto observado sin seleccionar por inferencia ni modificar capturas.
                val state=requireNotNull(dao.state());dao.select(state.profileId,state.eventId)
                SaveEventResult.Success(id)
            }
        } catch(cancelled:kotlinx.coroutines.CancellationException) { throw cancelled }
        catch(_:SQLiteException) { SaveEventResult.StorageUnavailable }
        catch(_:IllegalArgumentException) { SaveEventResult.StorageUnavailable }
        catch(_:DateTimeException) { SaveEventResult.StorageUnavailable }
        catch(_:IllegalStateException) { SaveEventResult.StorageUnavailable }
    }
}
