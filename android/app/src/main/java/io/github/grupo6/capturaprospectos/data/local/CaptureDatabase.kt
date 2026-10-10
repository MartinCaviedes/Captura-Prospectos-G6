package io.github.grupo6.capturaprospectos.data.local

import androidx.room.*

@Entity(tableName = "consent_text")
internal data class ConsentTextRow(@PrimaryKey val version: String, val text: String)

@Entity(tableName = "captures", foreignKeys = [ForeignKey(
    entity = ConsentTextRow::class, parentColumns = ["version"], childColumns = ["consentVersion"],
)], indices = [Index("consentVersion"), Index("ownerId"), Index("siteId")])
internal data class CaptureRow(
    @PrimaryKey val id: String, val revision: Long, val state: String,
    val ownerId: String, val siteId: String, val eventId: String, val deviceId: String,
    val capturedAt: String, val eventName: String, val eventTimeZone: String,
    val fullName: String?, val rut: String, val email: String, val phone: String,
    val educationLevel: String?, val establishment: String?, val contactChannel: String?, val notes: String?,
    val consentVersion: String, val acceptedAt: String, val recordedBy: String, val acceptanceMode: String,
    val locationSource: String?, val latitude: Double?, val longitude: Double?, val measuredAt: String?,
    val accuracyMeters: Double?, val confirmedAt: String?, val confirmedBy: String?,
    @ColumnInfo(defaultValue = "'LEGACY_1'") val contractVersion: String = "LEGACY_1",
    val givenNames: String? = null,
    val familyNames: String? = null,
    val birthDate: String? = null,
    val residenceCommune: String? = null,
    val currentActivity: String? = null,
    val currentActivityOther: String? = null,
    val establishmentCommune: String? = null,
    val formPersonalOption: String? = null,
    val preferredSchedule: String? = null,
    val interestLevel: String? = null,
    val motivation: String? = null,
    val modality: String? = null,
) { override fun toString() = "CaptureRow(redacted)" }

@Entity(tableName = "capture_interests", indices = [Index(value = ["captureId", "priority"], unique = true)], primaryKeys = ["captureId", "programId"], foreignKeys = [ForeignKey(
    entity = CaptureRow::class, parentColumns = ["id"], childColumns = ["captureId"],
)])
internal data class InterestRow(
    val captureId: String, val programId: String, val offeringId: String?,
    val catalogVersion: String, val programName: String, val modality: String?,
    val priority: Int? = null, val section: String? = null, val mentionId: String? = null, val mentionName: String? = null,
)

@Entity(tableName = "active_registration_keys", primaryKeys = ["eventId", "rut"],
    indices = [Index(value = ["captureId"], unique = true)], foreignKeys = [ForeignKey(
        entity = CaptureRow::class, parentColumns = ["id"], childColumns = ["captureId"],
    )])
internal data class ActiveKeyRow(val eventId: String, val rut: String, val captureId: String) {
    override fun toString() = "ActiveKeyRow(redacted)"
}

@Entity(tableName = "local_audit", foreignKeys = [ForeignKey(
    entity = CaptureRow::class, parentColumns = ["id"], childColumns = ["captureId"],
)], indices = [Index("captureId")])
internal data class AuditRow(@PrimaryKey val id: String, val captureId: String, val actorId: String, val occurredAt: String, val action: String, val resultingRevision: Long)

@Entity(tableName = "capture_site_preferences", primaryKeys = ["captureId", "siteId"], foreignKeys = [ForeignKey(
    entity = CaptureRow::class, parentColumns = ["id"], childColumns = ["captureId"],
)])
internal data class SitePreferenceRow(val captureId: String, val siteId: String, val name: String, val catalogVersion: String)

@Dao
internal interface CaptureDao {
    @Query("SELECT * FROM consent_text WHERE version = :version") fun consent(version: String): ConsentTextRow?
    @Insert fun insertConsent(row: ConsentTextRow)
    @Insert fun insertCapture(row: CaptureRow)
    @Insert fun insertInterests(rows: List<InterestRow>)
    @Insert fun insertSites(rows: List<SitePreferenceRow>)
    @Query("SELECT * FROM capture_site_preferences WHERE captureId = :id ORDER BY siteId") fun sites(id: String): List<SitePreferenceRow>
    @Insert fun insertKey(row: ActiveKeyRow)
    @Insert fun insertAudit(row: AuditRow)
    @Query("SELECT * FROM captures WHERE id = :id") fun capture(id: String): CaptureRow?
    @Query("SELECT * FROM capture_interests WHERE captureId = :id ORDER BY priority, programId") fun interests(id: String): List<InterestRow>
    @Query("SELECT captures.* FROM captures INNER JOIN active_registration_keys ON captures.id = active_registration_keys.captureId WHERE active_registration_keys.eventId = :eventId AND active_registration_keys.rut = :rut")
    fun duplicate(eventId: String, rut: String): CaptureRow?
    @Query("SELECT COUNT(*) FROM captures") fun count(): Int
    @Query("DELETE FROM active_registration_keys WHERE captureId = :captureId") fun deleteKey(captureId: String): Int
    @Query("UPDATE captures SET state = :state, revision = :newRevision WHERE id = :id AND revision = :expectedRevision")
    fun updateState(id: String, state: String, newRevision: Long, expectedRevision: Long): Int
    @Query("SELECT * FROM captures WHERE ownerId = :ownerId ORDER BY capturedAt DESC, id DESC")
    fun capturesByOwner(ownerId: String): List<CaptureRow>
    @Query("SELECT * FROM captures WHERE siteId IN (:siteIds) AND state != 'DRAFT' ORDER BY capturedAt DESC, id DESC")
    fun capturesBySitesExcludingDraft(siteIds: List<String>): List<CaptureRow>
}

@Database(entities = [ConsentTextRow::class, CaptureRow::class, InterestRow::class, ActiveKeyRow::class, AuditRow::class, SitePreferenceRow::class, CatalogVersionRow::class, DemoSiteRow::class, DemoProfileRow::class, ProfileSiteRow::class, DemoEventRow::class, DemoAssignmentRow::class, DemoProgramRow::class, DemoMentionRow::class, DemoContextRow::class], version = 3, exportSchema = true)
internal abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captures(): CaptureDao
    abstract fun context(): ContextDao
}
