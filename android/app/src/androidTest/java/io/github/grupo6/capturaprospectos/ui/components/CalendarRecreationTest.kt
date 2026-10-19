package io.github.grupo6.capturaprospectos.ui.components

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.domain.device.*
import io.github.grupo6.capturaprospectos.ui.admission.*
import io.github.grupo6.capturaprospectos.ui.hub.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import kotlinx.coroutines.flow.flowOf
import java.time.*
import java.util.*
import org.junit.*
import org.junit.Assert.*

/** Recompone la Activity de pruebas de Compose en cada creación, sin abrir Room. */
private data class CalendarOwners(val activity: ComponentActivity,
    val form: AdmissionFormViewModel, val hub: MainHubMetricsViewModel)

private class CalendarHarness(var birth: Boolean) : Application.ActivityLifecycleCallbacks {
    var current: CalendarOwners? = null
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.javaClass != ComponentActivity::class.java) return
        activity as ComponentActivity
        val actor=LocalActor(UUID(0,1),LocalRole.CAPTOR,setOf(UUID(0,2)))
        val event=CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),null)
        val env=AdmissionEnvironment(CaptureEnvironment(actor,event,UUID(0,4),emptyList(),
            "test","Consentimiento ficticio"),emptyList(),emptyList())
        val factory=object:ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T =
                (if(modelClass==AdmissionFormViewModel::class.java) AdmissionFormViewModel(env)
                else MainHubMetricsViewModel(null,event)) as T
        }
        val form=ViewModelProvider(activity,factory)[AdmissionFormViewModel::class.java]
        val hub=ViewModelProvider(activity,factory)[MainHubMetricsViewModel::class.java]
        current=CalendarOwners(activity,form,hub)
        activity.setContent { CapturaProspectosG6Theme {
            if(birth) AdmissionFormRoute(form)
            else MainHubRoute(DemoProfile(actor,"Captador ficticio"),event,
                object:ConnectivityObserver { override fun observe()=flowOf(ConnectivityStatus.OFFLINE) },
                {},{},suppliedMetrics=hub,summaryOnly=true)
        } }
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

class CalendarRecreationTest {
    @get:Rule val compose=createEmptyComposeRule()
    private var scenario:ActivityScenario<ComponentActivity>?=null
    private val application=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val harness=CalendarHarness(false)
    @Before fun setup() { application.registerActivityLifecycleCallbacks(harness) }
    @After fun cleanup() { scenario?.close();application.unregisterActivityLifecycleCallbacks(harness);harness.current=null }
    private fun start(birth:Boolean):CalendarOwners {
        harness.birth=birth
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        scenario=ActivityScenario.launch(Intent(context,ComponentActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
        return harness.current!!
    }
    private fun scroll(text:String) { compose.onNode(hasScrollAction()).performScrollToNode(hasText(text)) }
    @OptIn(ExperimentalMaterial3Api::class)
    private fun selectDay(iso:String) {
        val millis=LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val label=DatePickerDefaults.dateFormatter().formatDate(millis,Locale.getDefault(),forContentDescription=true)!!
        val matcher=hasText(label,substring=true,ignoreCase=true) or hasContentDescription(label,substring=true,ignoreCase=true)
        if(compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()) compose.onNode(isDialog()).performTouchInput { swipeUp() }
        compose.onNode(matcher).performScrollTo().performClick()
    }
    @Test fun birthDialogAndCandidateSurviveRecreationUntilExplicitConfirmationOrCancel() {
        val first=start(true)
        scroll("Nacimiento (aaaa-mm-dd)")
        compose.onNode(hasText("Nacimiento (aaaa-mm-dd)") and hasSetTextAction()).performTextReplacement("2000-01-01")
        compose.onNodeWithContentDescription("Elegir fecha de nacimiento").performClick()
        selectDay("2000-01-02")
        compose.waitForIdle()
        val draft=first.form.birthCalendar.draft.value!!
        assertEquals("2000-01-01",first.form.uiState.value.input.birthDate)
        scenario!!.recreate()
        scenario!!.onActivity { assertNotSame(first.activity,it);assertSame(first.form,harness.current!!.form) }
        compose.onNodeWithText("Cancelar fecha").assertIsDisplayed()
        assertEquals(draft,first.form.birthCalendar.draft.value)
        compose.onNodeWithText("Usar fecha").performClick()
        compose.runOnIdle { assertEquals("2000-01-02",first.form.uiState.value.input.birthDate) }
        scroll("Nacimiento (aaaa-mm-dd)")
        compose.onNodeWithContentDescription("Elegir fecha de nacimiento").performClick()
        selectDay("2000-01-03")
        scenario!!.recreate()
        compose.onNodeWithText("Cancelar fecha").performClick()
        compose.runOnIdle { assertEquals("2000-01-02",first.form.uiState.value.input.birthDate);assertNull(first.form.birthCalendar.draft.value) }
        scenario!!.close();scenario=null
        assertEquals("",first.form.uiState.value.input.birthDate)
        assertNull(first.form.birthCalendar.draft.value)
    }
    @Test fun bothRangeDialogsAndExpandedFieldsSurviveRecreationWithoutApplyingRange() {
        val first=start(false)
        scenario!!.onActivity { first.hub.fromDate("2000-01-01");first.hub.untilDate("2000-01-03") }
        scroll("Elegir período");compose.onNodeWithText("Elegir período").performClick()
        scroll("Desde (aaaa-mm-dd)");compose.onNodeWithContentDescription("Elegir fecha desde").performClick()
        selectDay("2000-01-02");compose.waitForIdle()
        val draft=first.hub.calendar.draft.value!!
        scenario!!.recreate()
        scenario!!.onActivity { assertNotSame(first.activity,it);assertSame(first.hub,harness.current!!.hub) }
        compose.onNodeWithText("Usar fecha").assertIsDisplayed()
        assertEquals(draft,first.hub.calendar.draft.value)
        assertTrue(first.hub.uiState.value.showRange)
        assertEquals("2000-01-01",first.hub.uiState.value.fromDate)
        compose.onNodeWithText("Usar fecha").performClick()
        scroll("Desde (aaaa-mm-dd)")
        compose.onNode(hasText("Desde (aaaa-mm-dd)") and hasSetTextAction()).assertTextContains("2000-01-02")
        scroll("Hasta inclusive (aaaa-mm-dd)");compose.onNodeWithContentDescription("Elegir fecha hasta").performClick()
        selectDay("2000-01-04");compose.waitForIdle()
        scenario!!.recreate()
        compose.onNodeWithText("Cancelar fecha").performClick()
        assertEquals("2000-01-03",first.hub.uiState.value.untilDate)
        scroll("Aplicar rango");compose.onNodeWithText("Aplicar rango").assertIsDisplayed()
        assertNull(first.hub.uiState.value.metrics)
        scenario!!.close();scenario=null
        assertFalse(first.hub.uiState.value.showRange)
        assertNull(first.hub.calendar.draft.value)
    }
}
