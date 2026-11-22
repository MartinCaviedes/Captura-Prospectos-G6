package io.github.grupo6.capturaprospectos.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import io.github.grupo6.capturaprospectos.domain.profile.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import io.github.grupo6.capturaprospectos.ui.access.UserAdminScreen
import io.github.grupo6.capturaprospectos.ui.access.UserAdminState
import java.util.UUID
import org.junit.*
import org.junit.Assert.*

class PersonalProfileUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private val models=ViewModelStore()
    private val a=UUID.randomUUID().toString();private val b=UUID.randomUUID().toString()
    private val saved=mutableMapOf(a to PersonalProfile("Alias A"))
    private val repository=object:PersonalProfileStore {
        override suspend fun load(accountId:String)=saved[accountId] ?: PersonalProfile()
        override suspend fun nickname(accountId:String,value:String)=load(accountId).copy(nickname=value).also {saved[accountId]=it}
        override suspend fun photo(accountId:String,jpeg:ByteArray?)=load(accountId).copy(photoJpeg=jpeg).also {saved[accountId]=it}
    }
    @After fun cleanup() {compose.runOnIdle {models.clear()}}
    @Test fun nicknameEditingKeepsOfficialIdentityAndRoleReadOnly() {
        var draft="";var saves=0
        compose.setContent {CapturaProspectosG6Theme {Surface(Modifier.fillMaxSize()) {
            var state by remember {mutableStateOf(PersonalProfileState(ownerId=a,ready=true))}
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PersonalProfileCard(state,"Nombre oficial","persona@example.invalid","Captador",
                    onNickname={draft=it;state=state.copy(nicknameDraft=it)},onSave={saves++},onChangePhoto={},onRetry={})
            }
        }}}
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        compose.onNodeWithText("Nombre de cuenta: Nombre oficial").assertIsDisplayed()
        compose.onNodeWithText("Rol: Captador").assertIsDisplayed()
        compose.onNodeWithText("Apodo").performScrollTo().performTextInput("Alias ficticio")
        compose.onNodeWithText("Guardar apodo").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle {assertEquals("Alias ficticio",draft);assertEquals(1,saves)}
    }
    @Test fun photoChoicesAreEnabledAndCancellingDoesNotStartAnImport() {
        lateinit var model:PersonalProfileViewModel
        compose.runOnIdle {model=PersonalProfileViewModel(repository);models.put("profile",model);model.openFor(a)}
        compose.setContent {CapturaProspectosG6Theme {Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {PersonalProfileRoute(model,a,"Nombre oficial","persona@example.invalid","Captador")}
        }}}
        compose.waitUntil(5000) {model.uiState.value.ready}
        compose.onNodeWithText("Cambiar foto").performScrollTo().performClick()
        compose.onNodeWithText("Elegir de galería").assertIsEnabled()
        compose.onNodeWithText("Tomar foto").assertIsEnabled()
        compose.onNodeWithText("Eliminar foto").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        compose.waitUntil(5000) {compose.onAllNodesWithText("Elegir de galería").fetchSemanticsNodes().isEmpty()}
        compose.runOnIdle {assertFalse(model.uiState.value.mediaPending);assertEquals("Alias A",saved[a]?.nickname)}
    }
    @Test fun changingAccountRemovesPreviousAliasAndPendingSelection() {
        lateinit var model:PersonalProfileViewModel
        val owner=mutableStateOf(a)
        compose.runOnIdle {model=PersonalProfileViewModel(repository);models.put("profile",model);model.openFor(a)}
        compose.setContent {CapturaProspectosG6Theme {Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {PersonalProfileRoute(model,owner.value,
                if(owner.value==a) "Usuario A" else "Usuario B","persona@example.invalid","Supervisor")}
        }}}
        compose.waitUntil(5000) {model.uiState.value.ready}
        compose.onNode(hasText("Alias A") and !hasSetTextAction()).assertIsDisplayed()
        lateinit var token:String
        compose.runOnIdle {token=model.beginMedia()!!;owner.value=b;model.openFor(b)}
        compose.waitUntil(5000) {model.uiState.value.ownerId==b && model.uiState.value.ready}
        compose.runOnIdle {model.receivePhoto(token) {throw AssertionError("Old selection must not be imported")}}
        compose.onNodeWithText("Alias A").assertDoesNotExist()
        compose.onNodeWithText("Nombre de cuenta: Usuario B").assertIsDisplayed()
        compose.onNodeWithContentDescription("Foto de perfil").assertDoesNotExist()
    }
    @Test fun storageErrorKeepsIdentityVisibleAndOffersRetryWithoutEditablePermissions() {
        var retries=0
        compose.setContent {CapturaProspectosG6Theme {Surface(Modifier.fillMaxSize()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PersonalProfileCard(PersonalProfileState(ownerId=a,error="No se pudo cargar tu personalización."),
                    "Administradora ficticia","admin@example.invalid","Administrador",{},{},{},{retries++})
            }
        }}}
        compose.onNodeWithText("Nombre de cuenta: Administradora ficticia").assertIsDisplayed()
        compose.onNodeWithText("Rol: Administrador").assertIsDisplayed()
        compose.onNodeWithText("Cambiar foto").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Reintentar personalización").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(1,retries)}
    }
    @Test fun administratorCanOpenPersonalProfileWithoutSelectingAnEvent() {
        var opened=false
        compose.setContent {CapturaProspectosG6Theme {Surface(Modifier.fillMaxSize()) {
            UserAdminScreen(UserAdminState(),"Administrador ficticio",{_,_->},{},{},{},{},{},{},{},onOpenProfile={opened=true})
        }}}
        compose.onNodeWithText("Mi perfil").performScrollTo().performClick()
        compose.runOnIdle {assertTrue(opened)}
        compose.onNodeWithText("Seleccionar evento").assertDoesNotExist()
    }
}
