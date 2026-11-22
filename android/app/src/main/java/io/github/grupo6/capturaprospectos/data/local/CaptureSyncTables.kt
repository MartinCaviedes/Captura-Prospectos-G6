package io.github.grupo6.capturaprospectos.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import io.github.grupo6.capturaprospectos.data.remote.*
import java.util.UUID

@Entity(tableName="capture_sync_links",foreignKeys=[ForeignKey(entity=CaptureRow::class,parentColumns=["id"],childColumns=["captureId"])])
internal data class SyncLinkRow(@PrimaryKey val captureId:String,val remoteId:String?=null,
    val acknowledgedRevision:Long=0,val legacy:Boolean=false)

@Entity(tableName="capture_outbox",indices=[Index(value=["captureId","sourceRevision"],unique=true),Index("actorId"),Index("registrationKey")],
    foreignKeys=[ForeignKey(entity=CaptureRow::class,parentColumns=["id"],childColumns=["captureId"])])
internal data class OutboxRow(@PrimaryKey val id:String,val captureId:String,val actorId:String,val sourceRevision:Long,
    val requestJson:String,val sequence:Long,val registrationKey:String,val status:String="PENDING",val attempts:Int=0,val nextAttemptAt:Long=0,val errorCode:String?=null) {
    override fun toString()="OutboxRow(payload=redacted)"
}
@Dao internal interface CaptureSyncDao {
    @Insert fun insertLink(link:SyncLinkRow)
    @Update fun updateLink(link:SyncLinkRow)
    @Query("SELECT * FROM capture_sync_links WHERE captureId=:id") fun link(id:String):SyncLinkRow?
    @Insert fun enqueue(row:OutboxRow)
    @Query("SELECT * FROM capture_outbox WHERE id=:id") fun operation(id:String):OutboxRow?
    @Query("SELECT o.* FROM capture_outbox o WHERE actorId=:actor AND status IN ('PENDING','RETRY') AND (nextAttemptAt<=:now OR nextAttemptAt>:latest) AND NOT EXISTS (SELECT 1 FROM capture_outbox prior WHERE (prior.captureId=o.captureId AND prior.sourceRevision<o.sourceRevision) OR (prior.registrationKey=o.registrationKey AND prior.sequence<o.sequence)) ORDER BY sequence,id LIMIT 1")
    fun next(actor:String,now:Long,latest:Long):OutboxRow?
    @Query("SELECT COALESCE(MAX(sequence),0)+1 FROM capture_outbox") fun nextSequence():Long
    @Query("SELECT * FROM capture_outbox WHERE actorId=:actor ORDER BY captureId,sourceRevision") fun all(actor:String):List<OutboxRow>
    @Query("SELECT * FROM capture_outbox WHERE captureId=:id ORDER BY sourceRevision") fun forCapture(id:String):List<OutboxRow>
    @Update fun update(row:OutboxRow)
    @Query("DELETE FROM capture_outbox WHERE id=:id") fun acknowledge(id:String)
    @Query("SELECT COUNT(*) FROM capture_sync_links l JOIN captures c ON c.id=l.captureId WHERE l.legacy=1 AND c.ownerId=:actor AND c.state!='DRAFT'") fun legacyCount(actor:String):Int
}

internal object CaptureMigration4To5:Migration(4,5) {
    override fun migrate(db:SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS capture_sync_links (captureId TEXT NOT NULL PRIMARY KEY,remoteId TEXT,acknowledgedRevision INTEGER NOT NULL,legacy INTEGER NOT NULL,FOREIGN KEY(captureId) REFERENCES captures(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE TABLE IF NOT EXISTS capture_outbox (id TEXT NOT NULL PRIMARY KEY,captureId TEXT NOT NULL,actorId TEXT NOT NULL,sourceRevision INTEGER NOT NULL,requestJson TEXT NOT NULL,sequence INTEGER NOT NULL,registrationKey TEXT NOT NULL,status TEXT NOT NULL,attempts INTEGER NOT NULL,nextAttemptAt INTEGER NOT NULL,errorCode TEXT,FOREIGN KEY(captureId) REFERENCES captures(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_capture_outbox_captureId_sourceRevision ON capture_outbox(captureId,sourceRevision)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_outbox_actorId ON capture_outbox(actorId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_capture_outbox_registrationKey ON capture_outbox(registrationKey)")
        db.execSQL("INSERT INTO capture_sync_links(captureId,remoteId,acknowledgedRevision,legacy) SELECT id,NULL,0,1 FROM captures")
    }
}

/** Invocar dentro de la misma transacción del guardado; nunca envía HTTP. */
internal fun enqueueCapture(db:CaptureDatabase,id:String,actor:String,reason:String?=null) {
    val row=requireNotNull(db.captures().capture(id))
    if(row.state=="DRAFT") return
    val dao=db.sync()
    var link=dao.link(id)
    if(link==null) { link=SyncLinkRow(id,legacy=true);dao.insertLink(link) }
    // Anular un borrador nunca transmitido no debe crear una inscripción remota.
    if(row.state=="CANCELLED" && link.acknowledgedRevision==0L && dao.forCapture(id).isEmpty()) return
    val snapshot=Gson().toJson(mapOf("schema" to row.contractVersion,"capture" to row,
        "interests" to db.captures().interests(id),"sites" to db.captures().sites(id),
        "consent" to db.captures().consent(row.consentVersion),"audit" to db.captures().audit(id)))
    val request=SyncCaptureRequestDto(row.deviceId,row.ownerId,row.revision,0,
        CreateProspectRequestDto(row.rut,row.eventId,BackendSession.mapLocalSiteToBackend(UUID.fromString(row.siteId)),
            row.givenNames.orEmpty(),row.familyNames.orEmpty(),row.email,row.phone,true,row.consentVersion),row.state,reason,snapshot)
    dao.enqueue(OutboxRow("$id/${row.revision}",id,actor,row.revision,Gson().toJson(request),dao.nextSequence(),"${row.eventId}:${row.rut}",
        status=if(link.legacy || row.contractVersion!="F2027_1") "REVIEW" else "PENDING",
        errorCode=if(link.legacy) "LEGACY_REVIEW_REQUIRED" else null))
}

internal fun captureTransport(db:CaptureDatabase,id:String):io.github.grupo6.capturaprospectos.domain.capture.TransportState {
    val rows=db.sync().forCapture(id)
    val link=db.sync().link(id)
    return when {
        rows.any { it.status=="REVIEW" } || link?.legacy==true -> io.github.grupo6.capturaprospectos.domain.capture.TransportState.REVIEW
        rows.isNotEmpty() -> io.github.grupo6.capturaprospectos.domain.capture.TransportState.QUEUED
        (link?.acknowledgedRevision ?: 0)>0 -> io.github.grupo6.capturaprospectos.domain.capture.TransportState.SYNCED
        else -> io.github.grupo6.capturaprospectos.domain.capture.TransportState.NOT_QUEUED
    }
}
