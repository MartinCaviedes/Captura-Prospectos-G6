package io.github.grupo6.capturaprospectos.data.local

import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.time.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

class CaptureSyncStorageTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="test-sync-${UUID.randomUUID()}.db"
    private var handle:EncryptedDatabase?=null
    private val instant=Instant.parse("2026-11-22T15:00:00Z")
    private val clock=Clock.fixed(instant,ZoneOffset.UTC)
    private var time=1000000L
    private lateinit var actor:LocalActor
    private lateinit var env:AdmissionEnvironment
    private val source=object:AdmissionContextSource {
        override suspend fun currentActor()=actor
        override suspend fun environment(eventId:UUID)=env.takeIf { it.context.event.id==eventId }
    }
    private fun db():CaptureDatabase {
        if(handle==null) handle=EncryptedDatabase.open(context,name)
        return handle!!.database
    }
    private fun admission()=RoomAdmissionRepository(db(),source,clock=clock)
    private fun records()=RoomRecordRepository(db(),source,clock)
    private fun input()=AdmissionInput(givenNames="Persona Ficticia",familyNames="Prueba",rut="12345678-5",
        email="persona@example.invalid",phone="+56912345678",birthDate="2000-01-01",residenceCommune="Comuna ficticia",
        currentActivity=CurrentActivity.SECONDARY_GRADUATE,interests=listOf(RankedInterest(UUID.fromString(DemoSeed.id("program-1")),1)))
    private fun identity()=SyncIdentity(actor.id.toString(),"CAPTADOR",setOf("site-1"))
    private suspend fun save(draft:Boolean=false):CreateAdmissionResult.Saved = admission().create(NewAdmission(env.context.event.id,input(),
        if(draft) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
        ConsentEvidence(DemoSeed.CONSENT,instant,actor.id,AcceptanceMode.DIRECT),null)) as CreateAdmissionResult.Saved
    private fun account()=UserSummaryDto(actor.id.toString(),"ficticio","persona@example.invalid","Persona ficticia","ACTIVE","CAPTADOR",listOf("site-1"),1,"2026-11-22","2026-11-22")
    private inner class Remote:CaptureSyncRemote {
        var verifyHook:suspend ()->Unit={}
        var sendHook:suspend (SyncCaptureRequestDto)->Unit={}
        var current:UserSummaryDto?=null
        val requests=mutableListOf<SyncCaptureRequestDto>()
        val accepted=mutableMapOf<String,SyncCaptureAckDto>()
        var loseAck=false
        override suspend fun verify():UserSummaryDto { verifyHook();return current ?: account() }
        override suspend fun send(bearer:String,id:String,request:SyncCaptureRequestDto):SyncCaptureAckDto {
            requests+=request;sendHook(request)
            val ack=accepted.getOrPut("$id/${request.sourceRevision}") { SyncCaptureAckDto(id,accepted.values.firstOrNull { it.localCaptureId==id }?.remoteId ?: UUID.randomUUID().toString(),request.sourceRevision,request.deviceId) }
            if(loseAck) { loseAck=false;throw IOException("Simulated lost ACK") }
            return ack
        }
    }
    @Before fun setup()=runBlocking {
        val ctx=RoomDemoContextRepository(db(),clock);ctx.initialize()
        ctx.selectProfile(UUID.fromString(DemoSeed.id("captor-a")));ctx.selectEvent(UUID.fromString(DemoSeed.id("event-a")))
        actor=requireNotNull(ctx.currentActor());env=requireNotNull(ctx.environment(UUID.fromString(DemoSeed.id("event-a"))))
        BackendSession.setToken("FAKE-TEST-TOKEN",listOf("site-1"))
    }
    @After fun cleanup() {
        BackendSession.clear();handle?.close();check(name.startsWith("test-sync-"));context.deleteDatabase(name)
        listOf("", ".bak", ".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("g6.database.$name") }
    }
    @Test fun outboxAndCompleteSnapshotSurviveReopen()=runBlocking {
        val saved=save();val row=db().sync().all(actor.id.toString()).single()
        assertEquals("PENDING",row.status);assertTrue(row.requestJson.contains("interests"));assertTrue(row.requestJson.contains("birthDate"))
        assertFalse(row.requestJson.contains("FAKE-TEST-TOKEN"));assertFalse(db().sync().link(saved.id.toString())!!.legacy)
        handle!!.close();handle=null
        assertEquals(row,db().sync().all(actor.id.toString()).single())
        assertEquals(TransportState.QUEUED,(admission().get(saved.id) as ReadAdmissionResult.Found).capture.transportState)
    }
    @Test fun lostAckRetriesSameRevisionAndPersistsRemoteLink()=runBlocking {
        val saved=save();val remote=Remote().apply { loseAck=true }
        RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
        val retry=db().sync().all(actor.id.toString()).single();assertEquals("RETRY",retry.status);assertEquals(time+5000,retry.nextAttemptAt)
        assertEquals(1,remote.accepted.size)
        handle!!.close();handle=null;time+=5000
        RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
        assertEquals(2,remote.requests.size);assertEquals(remote.requests[0],remote.requests[1]);assertEquals(1,remote.accepted.size)
        assertTrue(db().sync().all(actor.id.toString()).isEmpty());assertNotNull(db().sync().link(saved.id.toString())!!.remoteId)
        assertEquals(TransportState.SYNCED,(admission().get(saved.id) as ReadAdmissionResult.Found).capture.transportState)
    }
    @Test fun editDuringInFlightSendKeepsImmutableSnapshotsAndOrderedAcknowledgements()=runBlocking {
        val saved=save();val remote=Remote()
        remote.sendHook={ request -> if(request.sourceRevision==1L) {
            val old=(admission().get(saved.id) as ReadAdmissionResult.Found).capture
            assertTrue(records().edit(EditAdmission(saved.id,old.revision,old.input.copy(givenNames="Nombre actualizado"),SaveIntent.CONFIRM_REGISTRATION,null)) is RecordMutationResult.Saved)
        } }
        RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
        assertEquals(listOf(1L,2L),remote.requests.map {it.sourceRevision})
        assertEquals(listOf(0L,1L),remote.requests.map {it.expectedRevision})
        assertEquals("Persona Ficticia",remote.requests.first().registration.givenNames)
        assertEquals("Nombre actualizado",remote.requests.last().registration.givenNames)
        assertEquals(2L,db().sync().link(saved.id.toString())!!.acknowledgedRevision)
    }
    @Test fun conflictBlocksLaterRevisionsWithoutChangingLocalCapture()=runBlocking {
        val saved=save();val old=(admission().get(saved.id) as ReadAdmissionResult.Found).capture
        records().edit(EditAdmission(saved.id,1,old.input.copy(notes="Nota conservada"),SaveIntent.CONFIRM_REGISTRATION,null))
        val remote=Remote().apply { sendHook={throw AccountApiException("SYNC_VERSION_CONFLICT",409)} }
        val worker=RoomCaptureSynchronization(db(),remote) {time}
        worker.drain(identity()) {true};worker.drain(identity()) {true}
        assertEquals(1,remote.requests.size);assertEquals(2,db().sync().all(actor.id.toString()).size)
        assertEquals("REVIEW",db().sync().all(actor.id.toString()).first().status)
        assertEquals("Nota conservada",(admission().get(saved.id) as ReadAdmissionResult.Found).capture.input.notes)
        assertEquals(TransportState.REVIEW,(admission().get(saved.id) as ReadAdmissionResult.Found).capture.transportState)
    }
    @Test fun logoutWhileVerifyingStopsSendAndLeavesQueueIntact()=runBlocking {
        save();var authorized=true
        val remote=Remote().apply { verifyHook={authorized=false;BackendSession.clear()} }
        RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {authorized}
        assertTrue(remote.requests.isEmpty());assertEquals(1,db().sync().all(actor.id.toString()).size)
    }
    @Test fun permissionChangeAndServerRejectionStopWithoutDeletingPending()=runBlocking {
        save();val remote=Remote().apply { current=account().copy(status="DISABLED") }
        val worker=RoomCaptureSynchronization(db(),remote) {time}
        assertEquals("INVALID_TOKEN",worker.drain(identity()) {true});assertTrue(remote.requests.isEmpty())
        remote.current=null;remote.verifyHook={throw AccountApiException("ACCOUNT_DISABLED",403)}
        assertEquals("ACCOUNT_DISABLED",worker.drain(identity()) {true});assertEquals("PENDING",db().sync().all(actor.id.toString()).single().status)
    }
    @Test fun anotherAccountCannotDrainOwnersQueue()=runBlocking {
        save();val remote=Remote();val other=identity().copy(id=UUID.randomUUID().toString())
        RoomCaptureSynchronization(db(),remote) {time}.drain(other) {true}
        assertTrue(remote.requests.isEmpty());assertEquals(1,db().sync().all(actor.id.toString()).size)
    }
    @Test fun cancellationPrecedesNewRegistrationOfSamePersonEvenDuringRetry()=runBlocking {
        val old=save();records().cancel(CancelRecord(old.id,1,"Anulación de prueba"));save()
        val remote=Remote().apply { loseAck=true }
        val worker=RoomCaptureSynchronization(db(),remote) {time}
        worker.drain(identity()) {true}
        assertEquals(1,remote.requests.size) // New capture must not jump ahead of the retry/cancel.
        time+=5000;worker.drain(identity()) {true}
        assertEquals(listOf("NEEDS_CONTEXT","NEEDS_CONTEXT","CANCELLED","NEEDS_CONTEXT"),remote.requests.map {it.state})
        assertEquals(listOf(1L,1L,2L,1L),remote.requests.map {it.sourceRevision})
        assertTrue(db().sync().all(actor.id.toString()).isEmpty())
    }
    @Test fun draftCancellationIsLocalButConfirmedCancellationIsQueuedWithReason()=runBlocking {
        val draft=save(true);records().cancel(CancelRecord(draft.id,1,"Anulación de prueba"))
        assertTrue(db().sync().all(actor.id.toString()).isEmpty())
        val saved=save();records().cancel(CancelRecord(saved.id,1,"Anulación de prueba"))
        val remote=Remote();RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
        assertEquals(listOf("NEEDS_CONTEXT","CANCELLED"),remote.requests.map {it.state})
        assertEquals("Anulación de prueba",remote.requests.last().reason)
        assertEquals(CaptureState.CANCELLED,(admission().get(saved.id) as ReadAdmissionResult.Found).capture.state)
    }
    @Test fun outboxFailureRollsBackCaptureAuditAndDeduplicationKey()=runBlocking {
        db().openHelper.writableDatabase.execSQL("CREATE TRIGGER test_outbox_abort BEFORE INSERT ON capture_outbox BEGIN SELECT RAISE(ABORT,'test-only'); END")
        val result=admission().create(NewAdmission(env.context.event.id,input(),SaveIntent.CONFIRM_REGISTRATION,
            ConsentEvidence(DemoSeed.CONSENT,instant,actor.id,AcceptanceMode.DIRECT),null))
        assertEquals(CreateAdmissionResult.StorageUnavailable,result);assertEquals(0,db().captures().count())
        assertNull(db().captures().duplicate(env.context.event.id.toString(),input().rut))
        assertTrue(db().sync().all(actor.id.toString()).isEmpty())
    }
    @Test fun invalidAckDoesNotMarkCaptureAsSynced()=runBlocking {
        val saved=save();val remote=object:CaptureSyncRemote {
            override suspend fun verify()=account()
            override suspend fun send(bearer:String,id:String,request:SyncCaptureRequestDto)=SyncCaptureAckDto(id,UUID.randomUUID().toString(),999,request.deviceId)
        }
        RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
        assertNull(db().sync().link(saved.id.toString())!!.remoteId);assertEquals("REVIEW",db().sync().all(actor.id.toString()).single().status)
    }
    /** Requiere los tres JAR aislados en host:18080; cuentas exclusivamente ficticias. */
    @io.github.grupo6.capturaprospectos.testing.RequiresExternalFixture
    @Test fun actualGatewayRecoversLostAckThenEditsAndCancelsOneRemoteCapture()=runBlocking {
        val original=BackendSession.baseUrl
        BackendSession.baseUrl="http://10.0.2.2:18080/"
        try {
            val token=RetrofitAccounts().login("captor-a","test-captor-password-only")
            BackendSession.setToken(token.accessToken,token.siteIds,token.expiresIn)
            actor=actor.copy(id=UUID.fromString(token.userId))
            env=env.copy(context=env.context.copy(actor=actor,event=env.context.event.copy(id=UUID.randomUUID(),assignedCaptorIds=setOf(actor.id))))
            val saved=save();val real=RetrofitCaptureSync();var lose=true
            val remote=object:CaptureSyncRemote by real {
                override suspend fun send(bearer:String,id:String,request:SyncCaptureRequestDto):SyncCaptureAckDto {
                    val ack=real.send(bearer,id,request)
                    if(lose) { lose=false;throw IOException("ACK deliberately discarded after real server commit") }
                    return ack
                }
            }
            RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
            assertEquals("RETRY",db().sync().all(actor.id.toString()).single().status)
            handle!!.close();handle=null;time+=5000
            RoomCaptureSynchronization(db(),remote) {time}.drain(identity()) {true}
            val link=requireNotNull(db().sync().link(saved.id.toString()));assertNotNull(link.remoteId)
            val old=(admission().get(saved.id) as ReadAdmissionResult.Found).capture
            assertTrue(records().edit(EditAdmission(saved.id,1,old.input.copy(givenNames="Edición remota ficticia"),SaveIntent.CONFIRM_REGISTRATION,null)) is RecordMutationResult.Saved)
            assertTrue(records().cancel(CancelRecord(saved.id,2,"Anulación remota ficticia")) is RecordMutationResult.Saved)
            RoomCaptureSynchronization(db(),real) {time}.drain(identity()) {true}
            assertTrue(db().sync().all(actor.id.toString()).isEmpty())
            val api=BackendSession.createService();val bearer=requireNotNull(BackendSession.bearerHeader())
            val capture=api.getProspect(bearer,requireNotNull(link.remoteId)).body()!!
            assertEquals("CANCELLED",capture.status);assertEquals("Edición remota ficticia",capture.givenNames)
            assertEquals(1,api.listProspects(bearer,0,100).body()!!.items.count { it.eventId==env.context.event.id.toString() })
            assertEquals(3L,db().sync().link(saved.id.toString())!!.acknowledgedRevision)
        } finally { BackendSession.clear();BackendSession.baseUrl=original }
    }
}
