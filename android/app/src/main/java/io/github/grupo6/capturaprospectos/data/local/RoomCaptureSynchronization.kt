package io.github.grupo6.capturaprospectos.data.local

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonParseException
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.domain.records.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

internal interface CaptureSyncRemote {
    suspend fun verify():UserSummaryDto
    suspend fun send(bearer:String,id:String,request:SyncCaptureRequestDto):SyncCaptureAckDto
}
internal class RetrofitCaptureSync:CaptureSyncRemote {
    override suspend fun verify()=RetrofitAccounts().me()
    override suspend fun send(bearer:String,id:String,request:SyncCaptureRequestDto):SyncCaptureAckDto {
        val response=BackendSession.createService().syncCapture(bearer,id,request)
        if(!response.isSuccessful) {
            val code=runCatching { com.google.gson.JsonParser.parseString(response.errorBody()?.string()).asJsonObject["code"].asString }.getOrDefault("SYNC_FAILED")
            throw AccountApiException(code,response.code())
        }
        return response.body() ?: throw AccountApiException("INVALID_RESPONSE",502)
    }
}

/** La red ocurre fuera de transacciones; ACK/enlace se confirman juntos después. */
internal class RoomCaptureSynchronization(private val db:CaptureDatabase,
    private val remote:CaptureSyncRemote=RetrofitCaptureSync(),private val now:()->Long=System::currentTimeMillis):CaptureSynchronization {
    private val mutex=Mutex()
    override suspend fun overview(actorId:String)=withContext(Dispatchers.IO) {
        val rows=db.sync().all(actorId)
        SyncOverview(rows.count { it.status in listOf("PENDING","RETRY") },
            rows.filter { it.status=="REVIEW" }.map { SyncIssue(it.captureId,it.errorCode ?: "SYNC_FAILED",db.captures().capture(it.captureId)?.fullName ?: "Ficha") },db.sync().legacyCount(actorId))
    }
    override suspend fun drain(identity:SyncIdentity,stillAuthorized:()->Boolean):String?=mutex.withLock {
        withContext(Dispatchers.IO) {
            repeat(20) {
                if(!stillAuthorized()) return@withContext null
                val bearer=BackendSession.bearerHeader() ?: return@withContext null
                val row=db.sync().next(identity.id,now(),now()+300000) ?: return@withContext null
                try {
                    val fresh=remote.verify()
                    if(!stillAuthorized() || BackendSession.bearerHeader()!=bearer) return@withContext null
                    if(fresh.id!=identity.id || fresh.status!="ACTIVE" || fresh.role!=identity.role || fresh.siteIds.toSet()!=identity.siteIds)
                        return@withContext "INVALID_TOKEN"
                    val link=requireNotNull(db.sync().link(row.captureId))
                    if(link.legacy) { block(row,"LEGACY_REVIEW_REQUIRED");return@repeat }
                    val request=Gson().fromJson(row.requestJson,SyncCaptureRequestDto::class.java).copy(expectedRevision=link.acknowledgedRevision)
                    if(request.registration.siteId !in fresh.siteIds) return@withContext "INVALID_TOKEN"
                    if(!stillAuthorized() || BackendSession.bearerHeader()!=bearer) return@withContext null
                    val ack=remote.send(bearer,row.captureId,request)
                    require(ack.localCaptureId==row.captureId && ack.sourceRevision==row.sourceRevision && ack.deviceId==request.deviceId)
                    UUID.fromString(ack.remoteId)
                    require(link.remoteId==null || link.remoteId==ack.remoteId)
                    // Incluso tras logout se puede registrar el ACK de una petición ya aceptada.
                    db.withTransaction {
                        if(db.sync().operation(row.id)!=null) {
                            db.sync().updateLink(link.copy(remoteId=ack.remoteId,acknowledgedRevision=ack.sourceRevision))
                            db.sync().acknowledge(row.id)
                        }
                    }
                } catch(cancelled:CancellationException) { throw cancelled }
                catch(error:AccountApiException) {
                    if(error.httpStatus in listOf(401,403)) {
                        if(stillAuthorized() && BackendSession.bearerHeader()==bearer) return@withContext error.code
                        return@withContext null
                    }
                    if(error.httpStatus==0 || error.httpStatus==429 || error.httpStatus>=500) retry(row,error.code)
                    else block(row,error.code)
                } catch(_:IOException) { retry(row,"NETWORK_UNAVAILABLE") }
                catch(_:JsonParseException) { block(row,"INVALID_RESPONSE") }
                catch(_:IllegalArgumentException) { block(row,"INVALID_RESPONSE") }
                catch(_:NullPointerException) { block(row,"INVALID_RESPONSE") }
            }
            null
        }
    }
    private fun block(row:OutboxRow,code:String) { db.sync().update(row.copy(status="REVIEW",errorCode=code.take(80),attempts=row.attempts+1)) }
    private fun retry(row:OutboxRow,code:String) {
        val wait=(5000L shl row.attempts.coerceIn(0,6)).coerceAtMost(300000)
        db.sync().update(row.copy(status="RETRY",errorCode=code.take(80),attempts=(row.attempts+1).coerceAtMost(1000000),nextAttemptAt=now()+wait))
    }
}
