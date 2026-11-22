package io.github.grupo6.capturaprospectos.data.local

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.DateTimeException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Contexto local cifrado: catálogo base, cuentas de usuario reales y eventos creados en el dispositivo. */
internal class RoomDemoContextRepository(
    private val db: CaptureDatabase, private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DemoContextRepository {
    private val remoteEmails = java.util.concurrent.ConcurrentHashMap<String, String>()
    private suspend fun change(block: () -> ContextChange): ContextChange = withContext(dispatcher) {
        try { db.withTransaction { block() } } catch (_: SQLiteException) { ContextChange.STORAGE_UNAVAILABLE }
        catch (_: IllegalArgumentException) { ContextChange.STORAGE_UNAVAILABLE }
        catch (_: DateTimeException) { ContextChange.STORAGE_UNAVAILABLE }
    }

    private fun ensureAccountsTable() {
        db.openHelper.writableDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS `local_user_accounts` (`id` TEXT NOT NULL PRIMARY KEY, `email` TEXT NOT NULL UNIQUE, `passwordHash` TEXT NOT NULL)"
        )
    }

    private fun accountEmails(): Map<String, String> {
        ensureAccountsTable()
        val result = mutableMapOf<String, String>()
        db.openHelper.writableDatabase.query("SELECT id, email FROM local_user_accounts").use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return result
    }

    private fun initializeCatalogInternal(includeSeedFixtures: Boolean): ContextChange {
        val dao = db.context()
        ensureAccountsTable()
        val current = dao.state()
        if (current != null) return if (current.seedVersion == DemoSeed.VERSION) ContextChange.ALREADY_INITIALIZED else ContextChange.INCOMPATIBLE_SEED
        if (dao.existingRows() != 0) return ContextChange.INCOMPATIBLE_SEED
        val consent = db.captures().consent(DemoSeed.CONSENT)
        if (consent != null && consent.text != DemoSeed.CONSENT_TEXT) return ContextChange.INCOMPATIBLE_SEED
        if (consent == null) db.captures().insertConsent(ConsentTextRow(DemoSeed.CONSENT, DemoSeed.CONSENT_TEXT))
        dao.insertCatalog(DemoSeed.catalog)
        dao.insertSites(DemoSeed.sites)
        dao.insertPrograms(DemoSeed.programs)
        dao.insertMentions(DemoSeed.mentions)
        if (includeSeedFixtures) {
            dao.insertProfiles(DemoSeed.profiles)
            dao.insertScopes(DemoSeed.scopes)
            dao.insertEvents(DemoSeed.events)
            dao.insertAssignments(DemoSeed.assignments)
        }
        dao.insertState(DemoContextRow(
            seedVersion = DemoSeed.VERSION,
            deviceId = UUID.randomUUID().toString(),
            installedAt = clock.instant().toString(),
            catalogVersion = DemoSeed.CATALOG,
            consentVersion = DemoSeed.CONSENT,
            profileId = null,
            eventId = null,
        ))
        return ContextChange.APPLIED
    }

    override suspend fun initializeCatalog(): ContextChange = change {
        initializeCatalogInternal(includeSeedFixtures = false)
    }

    override suspend fun initialize(): ContextChange = change {
        initializeCatalogInternal(includeSeedFixtures = true)
    }

    override suspend fun snapshot(): DemoContextState = withContext(dispatcher) {
        try { db.withTransaction { readSnapshot()?.let(DemoContextState::Ready) ?: DemoContextState.NotInitialized } }
        catch (_: SQLiteException) { DemoContextState.StorageUnavailable }
        catch (_: IllegalArgumentException) { DemoContextState.StorageUnavailable }
        catch (_: DateTimeException) { DemoContextState.StorageUnavailable }
    }

    override fun observe(): Flow<DemoContextState> = db.context().observeState().map { snapshot() }.catch {
        if (it is SQLiteException) emit(DemoContextState.StorageUnavailable) else throw it
    }

    override suspend fun selectProfile(id: UUID): ContextChange = change {
        val dao = db.context()
        if (dao.state() == null) return@change ContextChange.NOT_INITIALIZED
        if (dao.profiles().none { it.id == id.toString() }) return@change ContextChange.INVALID_SELECTION
        dao.select(id.toString(), null)
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

    override suspend fun registerAccount(command: RegisterAccountCommand): AuthResult = withContext(dispatcher) {
        val name = command.displayName.trim()
        val email = command.email.trim().lowercase(Locale.ROOT)
        val password = command.password
        if (name.codePointCount(0, name.length) !in 2..120) {
            return@withContext AuthResult.Invalid("El nombre debe tener entre 2 y 120 caracteres.")
        }
        if (email.length > 160 || !ContactFormats.validEmail(email)) {
            return@withContext AuthResult.Invalid("Ingresa un correo electrónico válido.")
        }
        if (password.length !in 6..128) {
            return@withContext AuthResult.Invalid("La contraseña debe tener entre 6 y 128 caracteres.")
        }
        try {
            db.withTransaction {
                val dao = db.context()
                if (dao.state() == null) {
                    val initResult = initializeCatalogInternal(includeSeedFixtures = false)
                    if (initResult != ContextChange.APPLIED && initResult != ContextChange.ALREADY_INITIALIZED) {
                        return@withTransaction AuthResult.StorageUnavailable
                    }
                }
                ensureAccountsTable()
                val writable = db.openHelper.writableDatabase
                var existingIdByEmail: String? = null
                writable.query("SELECT id FROM local_user_accounts WHERE LOWER(email) = ?", arrayOf(email)).use { cursor ->
                    if (cursor.moveToFirst()) existingIdByEmail = cursor.getString(0)
                }
                if (existingIdByEmail != null && command.explicitId == null) {
                    return@withTransaction AuthResult.DuplicateAccount
                }
                val targetId = command.explicitId?.toString() ?: existingIdByEmail ?: UUID.randomUUID().toString()
                val catalogSites = dao.sites(DemoSeed.CATALOG).filter { it.enabled }.map { UUID.fromString(it.id) }.toSet()
                if (catalogSites.isEmpty()) return@withTransaction AuthResult.StorageUnavailable
                val resolvedSites = command.siteIds.intersect(catalogSites).ifEmpty { setOf(catalogSites.first()) }
                val hashed = hashPassword(password)

                writable.execSQL(
                    "INSERT OR REPLACE INTO local_user_accounts (id, email, passwordHash) VALUES (?, ?, ?)",
                    arrayOf<Any>(targetId, email, hashed),
                )
                dao.upsertProfile(DemoProfileRow(targetId, name, command.role.name))
                dao.deleteScopes(targetId)
                dao.insertScopes(resolvedSites.map { ProfileSiteRow(targetId, it.toString()) })
                dao.select(targetId, null)

                val profile = readSnapshot()?.currentProfile ?: return@withTransaction AuthResult.StorageUnavailable
                AuthResult.Authenticated(profile)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: SQLiteException) {
            AuthResult.StorageUnavailable
        } catch (_: IllegalArgumentException) {
            AuthResult.StorageUnavailable
        }
    }

    /** Importa identidad ya verificada remotamente; nunca recibe ni guarda contraseña del servidor. */
    override suspend fun acceptRemoteProfile(profile: DemoProfile): AuthResult = withContext(dispatcher) {
        try {
            db.withTransaction {
                val dao = db.context()
                if (dao.state() == null) {
                    val result = initializeCatalogInternal(includeSeedFixtures = false)
                    if (result !in setOf(ContextChange.APPLIED, ContextChange.ALREADY_INITIALIZED))
                        throw SQLiteException("Catálogo no disponible")
                }
                val allowed = dao.sites(DemoSeed.CATALOG).filter { it.enabled }.map { UUID.fromString(it.id) }.toSet()
                require(profile.actor.siteIds.isNotEmpty() && allowed.containsAll(profile.actor.siteIds))
                val id = profile.actor.id.toString()
                dao.upsertProfile(DemoProfileRow(id, profile.displayName, profile.actor.role.name))
                dao.deleteScopes(id)
                dao.insertScopes(profile.actor.siteIds.map { ProfileSiteRow(id, it.toString()) })
                dao.select(id, null)
            }
            remoteEmails[profile.actor.id.toString()] = profile.email
            AuthResult.Authenticated(profile)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: SQLiteException) { AuthResult.StorageUnavailable }
        catch (_: IllegalArgumentException) { AuthResult.StorageUnavailable }
    }

    override suspend fun authenticate(emailOrUsername: String, password: String): AuthResult = withContext(dispatcher) {
        val identifier = emailOrUsername.trim().lowercase(Locale.ROOT)
        if (identifier.isEmpty() || password.isEmpty()) return@withContext AuthResult.InvalidCredentials
        try {
            db.withTransaction {
                val dao = db.context()
                if (dao.state() == null) {
                    val initResult = initializeCatalogInternal(includeSeedFixtures = false)
                    if (initResult != ContextChange.APPLIED && initResult != ContextChange.ALREADY_INITIALIZED) {
                        return@withTransaction AuthResult.StorageUnavailable
                    }
                }
                ensureAccountsTable()
                val writable = db.openHelper.writableDatabase
                var matchedId: String? = null
                var storedHash: String? = null
                writable.query(
                    "SELECT id, email, passwordHash FROM local_user_accounts WHERE LOWER(email) = ? OR LOWER(id) = ?",
                    arrayOf(identifier, identifier),
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        matchedId = cursor.getString(0)
                        storedHash = cursor.getString(2)
                    }
                }
                if (matchedId == null) {
                    writable.query("SELECT id, email, passwordHash FROM local_user_accounts").use { cursor ->
                        while (cursor.moveToNext()) {
                            val rowEmail = cursor.getString(1).lowercase(Locale.ROOT)
                            if (rowEmail.substringBefore('@') == identifier) {
                                matchedId = cursor.getString(0)
                                storedHash = cursor.getString(2)
                                break
                            }
                        }
                    }
                }
                val accountId = matchedId ?: return@withTransaction AuthResult.InvalidCredentials
                val hash = storedHash ?: return@withTransaction AuthResult.InvalidCredentials
                if (!verifyPassword(password, hash)) return@withTransaction AuthResult.InvalidCredentials
                if (dao.profiles().none { it.id == accountId }) return@withTransaction AuthResult.InvalidCredentials
                dao.select(accountId, null)
                val profile = readSnapshot()?.currentProfile ?: return@withTransaction AuthResult.StorageUnavailable
                AuthResult.Authenticated(profile)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: SQLiteException) {
            AuthResult.StorageUnavailable
        } catch (_: IllegalArgumentException) {
            AuthResult.StorageUnavailable
        }
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
        val emails = accountEmails()
        val profiles = dao.profiles().map {
            DemoProfile(
                LocalActor(UUID.fromString(it.id), LocalRole.valueOf(it.role),
                    dao.scopes(it.id).map { scope -> UUID.fromString(scope.siteId) }.toSet()),
                it.displayName,
                remoteEmails[it.id] ?: emails[it.id].orEmpty(),
            )
        }
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
        require(version.sourceKind in setOf("SYNTHETIC", "OFFICIAL_REVIEWED"))
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
                if(command.siteId !in actor.siteIds)
                    return@withTransaction SaveEventResult.Unauthorized
                val existing=command.eventId?.let { dao.event(it.toString()) }
                if(command.eventId != null && (existing==null || UUID.fromString(existing.siteId) !in actor.siteIds))
                    return@withTransaction SaveEventResult.Unauthorized
                if(snapshot.catalog.sites.none { it.id==command.siteId && it.enabled }) return@withTransaction SaveEventResult.Unauthorized
                val effectiveCaptorIds = if (actor.role == LocalRole.CAPTOR) command.assignedCaptorIds + actor.id else command.assignedCaptorIds
                val issues=eventIssues(command.copy(assignedCaptorIds = effectiveCaptorIds)).toMutableList()
                val captors=snapshot.profiles.filter { it.actor.role==LocalRole.CAPTOR && command.siteId in it.actor.siteIds }.map { it.actor.id }.toSet()
                if(!captors.containsAll(effectiveCaptorIds)) issues += "Sólo se asignan captadores habilitados para la sede elegida."
                if(issues.isNotEmpty()) return@withTransaction SaveEventResult.Invalid(issues)
                val id=command.eventId ?: UUID.randomUUID()
                val row=DemoEventRow(id.toString(),command.siteId.toString(),command.name.trim(),command.startsAt.toString(),command.endsAt.toString(),command.timeZone.id,
                    command.coordinates?.latitude,command.coordinates?.longitude,command.establishment?.trim()?.ifEmpty { null },command.establishmentCommune?.trim()?.ifEmpty { null })
                if(existing==null) dao.insertEvent(row) else dao.updateEvent(row)
                dao.deleteAssignments(id.toString())
                dao.insertAssignments(effectiveCaptorIds.map { DemoAssignmentRow(id.toString(),it.toString()) })
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

    private fun hashPassword(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(salt)
            update(password.toByteArray(Charsets.UTF_8))
        }.digest()
        return "${salt.toHex()}:${digest.toHex()}"
    }

    private fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val salt = parts[0].fromHex() ?: return false
        val expected = parts[1].fromHex() ?: return false
        val actual = MessageDigest.getInstance("SHA-256").apply {
            update(salt)
            update(password.toByteArray(Charsets.UTF_8))
        }.digest()
        return MessageDigest.isEqual(expected, actual)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
