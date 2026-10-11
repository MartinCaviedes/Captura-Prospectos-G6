package io.github.grupo6.capturaprospectos.ui.hub

import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.records.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainHubMetricsStateTest {
    private val dispatcher=StandardTestDispatcher()
    private val now=Instant.parse("2026-10-10T15:00:00Z")
    private val event=CaptureEvent(UUID(0,1),UUID(0,2),"Evento ficticio",now,now.plusSeconds(3600),ZoneId.of("America/New_York"),emptySet(),null)
    private val store=ViewModelStore()
    private class Repo:RecordRepository {
        var result:MetricsResult=MetricsResult.Calculated(DeviceMetrics(UUID(0,1),3,2,1,1,0))
        val requests=mutableListOf<MetricsQuery>()
        var wait:CompletableDeferred<Unit>?=null
        override suspend fun getMetrics(query:MetricsQuery):MetricsResult { requests+=query; wait?.await(); return result }
        override suspend fun get(id:UUID)=ReadAdmissionResult.Unavailable
        override suspend fun list(query:RecordQuery)=RecordPageResult.Page(emptyList(),null)
        override suspend fun edit(command:EditAdmission)=RecordMutationResult.StorageUnavailable
        override suspend fun cancel(command:CancelRecord)=RecordMutationResult.StorageUnavailable
    }
    private lateinit var repo:Repo
    private fun vm()=MainHubMetricsViewModel(repo,event,Clock.fixed(now,ZoneOffset.UTC)).also { store.put("metrics",it) }
    @Before fun setup() { Dispatchers.setMain(dispatcher); repo=Repo() }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    @Test fun rangeUsesEventTimeZoneAndInclusiveLastDayAcrossDst()=runTest(dispatcher) {
        val vm=vm(); runCurrent(); vm.fromDate("2026-03-08"); vm.untilDate("2026-03-08"); vm.range()!!.join()
        val q=repo.requests.last(); assertEquals(Instant.parse("2026-03-08T05:00:00Z"),q.fromInclusive)
        assertEquals(Instant.parse("2026-03-09T04:00:00Z"),q.untilExclusive)
        assertEquals(23,Duration.between(q.fromInclusive,q.untilExclusive).toHours().toInt())
        vm.all().join(); assertNull(repo.requests.last().fromInclusive); assertEquals("",vm.uiState.value.fromDate)
    }
    @Test fun invalidDatesCancelPendingReadAndCannotBeClearedByItsLateResponse()=runTest(dispatcher) {
        val vm=vm(); runCurrent(); val previous=vm.uiState.value.metrics
        repo.wait=CompletableDeferred(); vm.refresh(); runCurrent(); val count=repo.requests.size
        vm.fromDate("2026-02-30"); vm.untilDate("2026-03-01"); assertNull(vm.range())
        repo.wait!!.complete(Unit); runCurrent()
        assertEquals(count,repo.requests.size); assertSame(previous,vm.uiState.value.metrics)
        assertFalse(vm.uiState.value.isLoading); assertTrue(vm.uiState.value.error!!.contains("Revisa"))
        vm.fromDate("2026-10-11"); vm.untilDate("2026-10-10"); assertNull(vm.range())
    }
    @Test fun storageFailureKeepsLastCalculationWithErrorAndAccessLossClearsIt()=runTest(dispatcher) {
        val vm=vm(); runCurrent(); assertEquals(3,vm.uiState.value.metrics!!.totalCaptures)
        repo.result=MetricsResult.StorageUnavailable; vm.refresh().join()
        assertEquals(3,vm.uiState.value.metrics!!.totalCaptures); assertTrue(vm.uiState.value.error!!.contains("No se interpreta como cero"))
        repo.result=MetricsResult.Unavailable; vm.refresh().join(); assertNull(vm.uiState.value.metrics)
        assertTrue(vm.uiState.value.error!!.contains("contexto autorizado"))
    }
    @Test fun refreshReusesAppliedRangeAndClearOwnerCancelsLateResults()=runTest(dispatcher) {
        val vm=vm(); runCurrent(); vm.today().join(); val q=repo.requests.last()
        assertEquals(Instant.parse("2026-10-10T04:00:00Z"),q.fromInclusive)
        vm.fromDate("sin aplicar"); vm.refresh().join(); assertEquals(q,repo.requests.last())
        repo.wait=CompletableDeferred(); vm.refresh(); runCurrent(); store.clear(); repo.wait!!.complete(Unit); runCurrent()
        assertNull(vm.uiState.value.metrics); assertFalse(vm.uiState.value.isLoading)
    }
}
