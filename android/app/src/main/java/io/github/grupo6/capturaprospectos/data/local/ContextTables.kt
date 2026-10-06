package io.github.grupo6.capturaprospectos.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "demo_catalog_versions")
internal data class CatalogVersionRow(@PrimaryKey val id: String, val sourceKind: String, val sourceReference: String, val academicYear: Int)

@Entity(tableName = "demo_sites", foreignKeys = [ForeignKey(entity = CatalogVersionRow::class, parentColumns = ["id"], childColumns = ["catalogVersion"])], indices = [Index("catalogVersion")])
internal data class DemoSiteRow(@PrimaryKey val id: String, val name: String, val catalogVersion: String, val enabled: Boolean)

@Entity(tableName = "demo_profiles")
internal data class DemoProfileRow(@PrimaryKey val id: String, val displayName: String, val role: String)

@Entity(tableName = "demo_profile_sites", primaryKeys = ["profileId", "siteId"], foreignKeys = [
    ForeignKey(entity = DemoProfileRow::class, parentColumns = ["id"], childColumns = ["profileId"]),
    ForeignKey(entity = DemoSiteRow::class, parentColumns = ["id"], childColumns = ["siteId"]),
], indices = [Index("siteId")])
internal data class ProfileSiteRow(val profileId: String, val siteId: String)

@Entity(tableName = "demo_events", foreignKeys = [ForeignKey(entity = DemoSiteRow::class, parentColumns = ["id"], childColumns = ["siteId"])], indices = [Index("siteId")])
internal data class DemoEventRow(
    @PrimaryKey val id: String, val siteId: String, val name: String, val startsAt: String, val endsAt: String,
    val timeZone: String, val latitude: Double?, val longitude: Double?, val establishment: String?, val establishmentCommune: String?,
)

@Entity(tableName = "demo_assignments", primaryKeys = ["eventId", "profileId"], foreignKeys = [
    ForeignKey(entity = DemoEventRow::class, parentColumns = ["id"], childColumns = ["eventId"]),
    ForeignKey(entity = DemoProfileRow::class, parentColumns = ["id"], childColumns = ["profileId"]),
], indices = [Index("profileId")])
internal data class DemoAssignmentRow(val eventId: String, val profileId: String)

@Entity(tableName = "demo_programs", foreignKeys = [ForeignKey(entity = CatalogVersionRow::class, parentColumns = ["id"], childColumns = ["catalogVersion"])], indices = [Index("catalogVersion")])
internal data class DemoProgramRow(@PrimaryKey val id: String, val name: String, val catalogVersion: String, val section: String, val enabled: Boolean)

@Entity(tableName = "demo_mentions", foreignKeys = [ForeignKey(entity = DemoProgramRow::class, parentColumns = ["id"], childColumns = ["programId"])], indices = [Index("programId")])
internal data class DemoMentionRow(@PrimaryKey val id: String, val programId: String, val name: String)

@Entity(tableName = "demo_context", foreignKeys = [
    ForeignKey(entity = DemoProfileRow::class, parentColumns = ["id"], childColumns = ["profileId"]),
    ForeignKey(entity = DemoEventRow::class, parentColumns = ["id"], childColumns = ["eventId"]),
    ForeignKey(entity = CatalogVersionRow::class, parentColumns = ["id"], childColumns = ["catalogVersion"]),
    ForeignKey(entity = ConsentTextRow::class, parentColumns = ["version"], childColumns = ["consentVersion"]),
], indices = [Index("profileId"), Index("eventId"), Index("catalogVersion"), Index("consentVersion")])
internal data class DemoContextRow(
    @PrimaryKey val singletonId: Int = 1, val seedVersion: String, val deviceId: String, val installedAt: String,
    val catalogVersion: String, val consentVersion: String, val profileId: String?, val eventId: String?,
)

@Dao
internal interface ContextDao {
    @Query("SELECT * FROM demo_context WHERE singletonId = 1") fun state(): DemoContextRow?
    @Query("SELECT * FROM demo_context WHERE singletonId = 1") fun observeState(): Flow<DemoContextRow?>
    @Query("SELECT * FROM demo_profiles ORDER BY displayName, id") fun profiles(): List<DemoProfileRow>
    @Query("SELECT * FROM demo_profile_sites WHERE profileId = :id") fun scopes(id: String): List<ProfileSiteRow>
    @Query("SELECT * FROM demo_events ORDER BY name, id") fun events(): List<DemoEventRow>
    @Query("SELECT * FROM demo_assignments WHERE eventId = :id") fun assignments(id: String): List<DemoAssignmentRow>
    @Query("SELECT * FROM demo_catalog_versions WHERE id = :id") fun catalog(id: String): CatalogVersionRow?
    @Query("SELECT * FROM demo_programs WHERE catalogVersion = :version AND enabled = 1 ORDER BY name, id") fun programs(version: String): List<DemoProgramRow>
    @Query("SELECT * FROM demo_sites WHERE catalogVersion = :version AND enabled = 1 ORDER BY name, id") fun sites(version: String): List<DemoSiteRow>
    @Query("SELECT * FROM demo_mentions WHERE programId = :id ORDER BY name, id") fun mentions(id: String): List<DemoMentionRow>
    @Query("SELECT (SELECT COUNT(*) FROM demo_profiles) + (SELECT COUNT(*) FROM demo_sites) + (SELECT COUNT(*) FROM demo_catalog_versions) + (SELECT COUNT(*) FROM demo_events) + (SELECT COUNT(*) FROM demo_programs)") fun existingRows(): Int
    @Insert fun insertCatalog(row: CatalogVersionRow)
    @Insert fun insertSites(rows: List<DemoSiteRow>)
    @Insert fun insertProfiles(rows: List<DemoProfileRow>)
    @Insert fun insertScopes(rows: List<ProfileSiteRow>)
    @Insert fun insertEvents(rows: List<DemoEventRow>)
    @Insert fun insertAssignments(rows: List<DemoAssignmentRow>)
    @Insert fun insertPrograms(rows: List<DemoProgramRow>)
    @Insert fun insertMentions(rows: List<DemoMentionRow>)
    @Insert fun insertState(row: DemoContextRow)
    @Query("UPDATE demo_context SET profileId = :profileId, eventId = :eventId WHERE singletonId = 1") fun select(profileId: String?, eventId: String?)
}
