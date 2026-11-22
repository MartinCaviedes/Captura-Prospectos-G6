package io.github.grupo6.capturaprospectos.ui.access

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.grupo6.capturaprospectos.data.remote.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme as ProductTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@Composable private fun CapturaProspectosG6Theme(content: @Composable () -> Unit) {
    ProductTheme { Surface(Modifier.fillMaxSize(), content = content) }
}

class AccountScreensTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private fun sample(role:String?=null)=UserSummaryDto("11111111-1111-4111-8111-111111111111","persona","persona@example.invalid","Persona ficticia","PENDING",role,emptyList(),0,"2026-11-21T12:00:00Z","2026-11-21T12:00:00Z")
    private fun image(name:String) {
        val file=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,name)
        compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().let { bitmap -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } }
    }
    @Test fun offlineAccessRequiresExplicitButtonAndDoesNotRequestPassword() {
        var resumed=false
        compose.setContent { CapturaProspectosG6Theme { DemoAccessScreen(
            DemoAccessState(welcome=false,offlineAvailable=true),true,false,{},{},{},onResumeOffline={resumed=true}) } }
        compose.onNodeWithText("Continuar sin conexión").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(resumed) }
        image("accounts-offline.png")
    }
    @Test fun registerDoesNotOfferRolesOrSites() {
        compose.setContent { CapturaProspectosG6Theme { DemoAccessScreen(DemoAccessState(welcome=false,registerMode=true),true,false,{},{},{}) } }
        compose.onNodeWithText("Solicitar cuenta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rol en terreno").assertDoesNotExist()
        compose.onNodeWithText("Sedes asignadas").assertDoesNotExist()
        image("accounts-register.png")
    }
    @Test fun pendingScreenHasNoOperationalAccessOrPasswordForm() {
        var dismissed=false
        compose.setContent { CapturaProspectosG6Theme { DemoAccessScreen(DemoAccessState(welcome=false,pendingMessage="El administrador debe habilitar tu cuenta."),true,false,{},{},{},onPendingDismiss={dismissed=true}) } }
        compose.onNodeWithText("Solicitud pendiente").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        image("accounts-pending.png")
        compose.onNodeWithText("Volver al acceso").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
    @Test fun administrationListsPendingWithoutRequiringEventSelection() {
        var selected=false
        compose.setContent { CapturaProspectosG6Theme { UserAdminScreen(UserAdminState(users=listOf(sample()),total=1),"Administrador ficticio",{_,_->},{selected=true},{},{},{},{},{},{}) } }
        compose.onNodeWithText("Administración de usuarios").assertIsDisplayed()
        compose.onNodeWithText("Revisar cuenta").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(selected) }
        image("accounts-admin.png")
    }
    @Test fun editingRequiresConfirmationBeforeSaving() {
        var saves=0
        val state=UserAdminState(selected=sample(),sites=listOf(ManagedSiteDto("site-1","Sede de prueba",true)),assigned=setOf("site-1"))
        compose.setContent { CapturaProspectosG6Theme { UserAdminScreen(state,"Administrador ficticio",{_,_->},{},{},{},{},{},{saves++},{}) } }
        compose.onNodeWithText("Guardar permisos").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0,saves) }
        compose.onNodeWithText("Confirmar permisos").assertIsDisplayed()
        image("accounts-confirm.png")
        compose.onNodeWithText("Confirmar",useUnmergedTree=true).performClick()
        compose.runOnIdle { assertEquals(1,saves) }
    }
    @Test fun administratorHasNoEditablePrivilegeControls() {
        compose.setContent { CapturaProspectosG6Theme { UserAdminScreen(UserAdminState(selected=sample("ADMIN")),"Administrador ficticio",{_,_->},{},{},{},{},{},{},{}) } }
        compose.onNodeWithText("Esta cuenta administradora está protegida.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar permisos").assertDoesNotExist()
    }
}
