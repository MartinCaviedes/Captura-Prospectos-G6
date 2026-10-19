package io.github.grupo6.capturaprospectos.ui.shell

import android.content.Context
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.ui.access.DemoDataState
import io.github.grupo6.capturaprospectos.ui.theme.*
import java.time.*
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class ProfileAppearanceUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test-appearance-${UUID.randomUUID()}"
    private val preferences by lazy { context.getSharedPreferences(name,Context.MODE_PRIVATE) }
    private val store = ViewModelStore()
    @After fun cleanup() { compose.runOnIdle { store.clear() }; context.deleteSharedPreferences(name) }
    @Test fun profileChangesThemeAndNewOwnerReadsOnlyAppearancePreference() {
        val appearance = AppearanceViewModel(preferences)
        store.put("appearance",appearance)
        var background = Color.Unspecified
        val actor = LocalActor(UUID(0,1),LocalRole.CAPTOR,setOf(UUID(0,2)))
        val event = CaptureEvent(UUID(0,3),UUID(0,2),"Evento ficticio",Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600),ZoneOffset.UTC,setOf(actor.id),null)
        compose.setContent {
            val dark by appearance.darkMode.collectAsState()
            CapturaProspectosG6Theme(darkTheme=dark) {
                val colors = MaterialTheme.colorScheme
                SideEffect { background = colors.background; applySystemBarAppearance(compose.activity.window,dark) }
                ProfileScreen(DemoProfile(actor,"Perfil ficticio"),event,DemoDataState(),{},{},{},
                    Modifier.fillMaxSize(),dark,appearance::setDarkMode)
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Modo oscuro"))
        compose.onNodeWithContentDescription("Modo oscuro").assertIsOn().performClick()
        compose.onNodeWithContentDescription("Modo oscuro").assertIsOff()
        compose.onNodeWithText("Claro").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(background.luminance() > .5f)
            assertTrue(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars)
            assertTrue(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightNavigationBars)
            val fresh = AppearanceViewModel(context.getSharedPreferences(name,Context.MODE_PRIVATE))
            store.put("fresh",fresh)
            assertFalse(fresh.darkMode.value)
            assertEquals(mapOf("dark_mode" to false),preferences.all)
        }
        compose.onNodeWithContentDescription("Modo oscuro").performClick().assertIsOn()
        compose.runOnIdle { assertTrue(background.luminance() < .1f)
            assertFalse(WindowCompat.getInsetsController(compose.activity.window,compose.activity.window.decorView).isAppearanceLightStatusBars)
        }
    }
}
