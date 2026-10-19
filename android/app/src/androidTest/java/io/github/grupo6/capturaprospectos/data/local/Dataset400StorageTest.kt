package io.github.grupo6.capturaprospectos.data.local

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.io.File
import java.security.KeyStore
import java.time.*
import java.util.UUID
import kotlin.math.ceil
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

/** Mide repositorios reales en una base cifrada test-*; no escribe en captures-demo.db. */
class Dataset400StorageTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val name = "test-dataset400-${UUID.randomUUID()}.db"
    private var handle: EncryptedDatabase? = null
    private val now = Instant.parse("2026-10-10T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private fun id(key: String) = UUID.fromString(DemoSeed.id(key))
    private fun db(): CaptureDatabase {
        if (handle == null) handle = EncryptedDatabase.open(context,name)
        return handle!!.database
    }
    private fun ctx() = RoomDemoContextRepository(db(),clock)
    private val source = object : AdmissionContextSource {
        override suspend fun currentActor() = ctx().currentActor()
        override suspend fun environment(eventId: UUID) = ctx().environment(eventId)
    }
    private fun records() = RoomRecordRepository(db(),source,clock)
    private suspend fun select(profile: String,event: String) {
        ctx().selectProfile(id(profile));ctx().selectEvent(id(event))
        assertNotNull(ctx().currentActor())
    }
    private fun rut(index: Int): String {
        val body=(10_000_000+index).toString()
        var weight=2
        val sum=body.reversed().sumOf { digit -> (digit.digitToInt()*weight).also { weight=if(weight==7) 2 else weight+1 } }
        val dv=11-sum%11
        return "$body-${when(dv) { 11 -> "0";10 -> "K";else -> dv.toString() }}"
    }
    private fun input(index: Int) = AdmissionInput(givenNames="Persona Ficticia",familyNames="Demostracion",
        rut=rut(index),email="demo$index@example.invalid",phone="+56900000000",birthDate="2000-01-01",
        residenceCommune="Comuna ficticia",currentActivity=CurrentActivity.SECONDARY_GRADUATE,
        interests=if(index%2==0) listOf(RankedInterest(id("program-1"),1),RankedInterest(id("program-2"),2))
            else listOf(RankedInterest(id("program-1"),1)))
    private fun count(table: String): Int = db().openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table").use {
        assertTrue(it.moveToFirst());it.getInt(0)
    }
    private suspend fun pages(expected: Int): List<RecordSummary> {
        val rows=mutableListOf<RecordSummary>();var cursor:RecordCursor?=null
        do {
            val page=records().list(RecordQuery(after=cursor,limit=30)) as RecordPageResult.Page
            assertTrue(page.items.size<=30);rows+=page.items;cursor=page.next
            assertTrue("El cursor debe terminar sin repetir páginas",rows.size<=expected)
        } while(cursor!=null)
        assertEquals(expected,rows.size);assertEquals(expected,rows.map { it.id }.toSet().size)
        assertEquals(rows.map { it.id.toString() }.sortedDescending(),rows.map { it.id.toString() })
        return rows
    }
    private fun timings(values: List<Double>): JSONObject {
        val ordered=values.sorted()
        fun p(percent:Double)=ordered[ceil(ordered.size*percent).toInt()-1]
        return JSONObject().put("samples",ordered.size).put("p50_ms",p(.50)).put("p95_ms",p(.95))
            .put("p99_ms",p(.99)).put("max_ms",ordered.last()).put("p95_below_150_ms",p(.95)<150)
    }
    @After fun cleanup() {
        handle?.close();check(name.startsWith("test-dataset400-"));context.deleteDatabase(name)
        listOf("",".bak",".new").forEach { File(context.noBackupFilesDir,"$name.wrapped-key$it").delete() }
        KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("g6.database.$name") }
    }
    @Test fun encrypted400RowsSurviveReopenAndRespectRealCursorScopeAndMetrics() = runBlocking {
        ctx().initialize()
        val creates=mutableListOf<Double>();val cancels=mutableListOf<Double>()
        for((profile,event) in listOf("captor-a" to "event-a","captor-b" to "event-b")) {
            select(profile,event)
            val actor=requireNotNull(ctx().currentActor())
            val admissions=RoomAdmissionRepository(db(),source,clock=clock)
            for(index in 0 until 200) {
                val draft=index%20 in 5..6
                val needs=index%20 in 2..4
                val request=NewAdmission(id(event),input(index),if(draft) SaveIntent.SAVE_DRAFT else SaveIntent.CONFIRM_REGISTRATION,
                    ConsentEvidence(DemoSeed.CONSENT,now,actor.id,AcceptanceMode.DIRECT),
                    if(draft||needs) null else CaptureLocation.Device(Coordinates(-33.0,-71.0),now,20.0))
                val start=SystemClock.elapsedRealtimeNanos()
                val created=admissions.create(request)
                creates+=(SystemClock.elapsedRealtimeNanos()-start)/1_000_000.0
                assertTrue("Alta ficticia rechazada: $created",created is CreateAdmissionResult.Saved)
                val saved=created as CreateAdmissionResult.Saved
                assertEquals(if(draft) CaptureState.DRAFT else if(needs) CaptureState.NEEDS_CONTEXT else CaptureState.READY,saved.state)
                if(index%20 in 0..1) {
                    val cancelStart=SystemClock.elapsedRealtimeNanos()
                    val cancelled=records().cancel(CancelRecord(saved.id,saved.revision,"Anulación ficticia de prueba"))
                    cancels+=(SystemClock.elapsedRealtimeNanos()-cancelStart)/1_000_000.0
                    assertTrue(cancelled is RecordMutationResult.Saved)
                }
            }
        }
        assertEquals(400,count("captures"));assertEquals(600,count("capture_interests"))
        assertEquals(320,count("active_registration_keys"));assertEquals(440,count("local_audit"))
        db().openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        val queryTimes=JSONObject()
        for((profile,event) in listOf("captor-a" to "event-a","captor-b" to "event-b")) {
            select(profile,event)
            val start=SystemClock.elapsedRealtimeNanos();val rows=pages(200)
            queryTimes.put("$profile-all-pages-ms",(SystemClock.elapsedRealtimeNanos()-start)/1_000_000.0)
            assertEquals(mapOf(CaptureState.READY to 130,CaptureState.NEEDS_CONTEXT to 30,CaptureState.DRAFT to 20,CaptureState.CANCELLED to 20),rows.groupingBy { it.state }.eachCount())
            val metricsStart=SystemClock.elapsedRealtimeNanos()
            val metrics=(records().getMetrics(MetricsQuery(id(event))) as MetricsResult.Calculated).metrics
            queryTimes.put("$profile-metrics-ms",(SystemClock.elapsedRealtimeNanos()-metricsStart)/1_000_000.0)
            assertEquals(160,metrics.totalCaptures);assertEquals(160,metrics.distinctPeople)
            assertEquals(mapOf(id("program-1") to 160,id("program-2") to 80),metrics.programMetrics.associate { it.programId to it.registrations })
            assertEquals(MetricsResult.Unavailable,records().getMetrics(MetricsQuery(id(if(event=="event-a") "event-b" else "event-a"))))
        }
        ctx().selectProfile(id("supervisor"));assertEquals(360,pages(360).size)
        handle!!.close();handle=null
        assertEquals(400,count("captures"));assertEquals(320,count("active_registration_keys"));assertEquals(440,count("local_audit"))
        assertEquals(360,pages(360).size)
        val header=context.getDatabasePath(name).inputStream().use { it.readNBytes(16) }
        assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray()))
        val report=JSONObject().put("rows",400).put("states",JSONObject(mapOf("READY" to 260,"NEEDS_CONTEXT" to 60,"DRAFT" to 40,"CANCELLED" to 40)))
            .put("active_keys",320).put("audits",440).put("schema",4).put("model",Build.MODEL).put("api",Build.VERSION.SDK_INT)
            .put("create",timings(creates)).put("cancel",timings(cancels)).put("all_writes",timings(creates+cancels)).put("queries",queryTimes)
            .put("method","elapsedRealtimeNanos around complete real repository calls; initialization/context changes/native GPS excluded; debug build; nearest-rank percentiles")
        instrumentation.sendStatus(2,Bundle().apply { putString("stream","G6_DATASET400_RESULT=$report\n") })
    }
}
