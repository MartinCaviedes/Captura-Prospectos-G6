package io.github.grupo6.capturaprospectos.ui.access

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import org.junit.*
import org.junit.Assert.*

/** Activity real de pruebas recreada sin Room, permisos ni cambios de orientación del sistema. */
private class AccessHarness : Application.ActivityLifecycleCallbacks {
    lateinit var model: DemoAccessViewModel
    var submitted: Pair<String,String>? = null
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.javaClass != ComponentActivity::class.java) return
        activity as ComponentActivity
        model = ViewModelProvider(activity)[DemoAccessViewModel::class.java]
        model.begin()
        activity.setContent { CapturaProspectosG6Theme {
            DemoAccessRoute(model, true, false, { email,password -> submitted = email to password })
        } }
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

class DemoAccessRecreationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
    private val harness = AccessHarness()
    private var scenario: ActivityScenario<ComponentActivity>? = null
    @Before fun setup() { application.registerActivityLifecycleCallbacks(harness); start() }
    @After fun cleanup() { scenario?.close(); application.unregisterActivityLifecycleCallbacks(harness) }
    private fun start() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        scenario = ActivityScenario.launch(Intent(context, ComponentActivity::class.java)
            .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
    }
    private fun scroll(text: String) { compose.onNode(hasScrollAction()).performScrollToNode(hasText(text)) }
    private fun emailField() = compose.onNode(hasText("Correo electrónico") and hasSetTextAction())
    private fun passwordField() = compose.onNode(hasText("Contraseña") and hasSetTextAction())
    private fun chooseSample() {
        if (compose.onAllNodesWithText("Captador A").fetchSemanticsNodes().isEmpty()) {
            scroll("Cuentas de muestra"); compose.onNodeWithText("Cuentas de muestra").performClick()
        }
        scroll("Captador A"); compose.onNodeWithText("Captador A").performClick()
    }
    private fun recreate() {
        val owner = harness.model
        scenario!!.recreate()
        scenario!!.onActivity { assertSame(owner, harness.model) }
        compose.waitForIdle()
    }
    private fun submit() { scroll("Iniciar sesión"); compose.onNodeWithText("Iniciar sesión").performClick() }

    @Test fun sampleCredentialsSurviveRecreationWithoutAutomaticLoginAndPasswordRemainsMasked() {
        chooseSample()
        assertNull(harness.submitted)
        recreate()
        scroll("Correo electrónico"); emailField().assertTextContains("captador.a@g6.example")
        scroll("Contraseña"); compose.onNodeWithContentDescription("Mostrar contraseña").assertExists()
        recreate()
        assertNull(harness.submitted)
        submit()
        compose.runOnIdle { assertEquals("captador.a@g6.example" to DemoAccessFixtures.PASSWORD, harness.submitted) }
    }

    @Test fun manualEditsSurviveRecreationAndSubmittedPasswordIsNotRestored() {
        chooseSample()
        scroll("Correo electrónico"); emailField().performTextReplacement("captador.b@g6.example")
        scroll("Contraseña"); passwordField().performTextReplacement("ClaveFicticiaDePrueba")
        recreate()
        submit()
        compose.runOnIdle { assertEquals("captador.b@g6.example" to "ClaveFicticiaDePrueba", harness.submitted) }
        recreate()
        harness.submitted = null
        submit()
        compose.runOnIdle { assertEquals("captador.b@g6.example" to "", harness.submitted) }
    }

    @Test fun logoutAndNewActivityRequireFreshCredentials() {
        chooseSample()
        compose.runOnIdle { harness.model.logout() }
        submit()
        compose.runOnIdle { assertEquals("" to "", harness.submitted) }
        chooseSample()
        scenario!!.close(); scenario = null
        harness.submitted = null
        start()
        submit()
        compose.runOnIdle { assertEquals("" to "", harness.submitted) }
    }
}
