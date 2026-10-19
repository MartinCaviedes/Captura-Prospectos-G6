package io.github.grupo6.capturaprospectos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.data.device.AndroidConnectivityObserver
import io.github.grupo6.capturaprospectos.data.local.openContextSession
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEnvironment
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormRoute
import io.github.grupo6.capturaprospectos.ui.context.*
import io.github.grupo6.capturaprospectos.ui.access.*
import io.github.grupo6.capturaprospectos.ui.hub.MainHubRoute
import io.github.grupo6.capturaprospectos.ui.shell.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme

enum class AppScreen { CONTEXT_SELECTOR, HUB, FORM, TRAY, SUMMARY, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext
        val owner = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ContextSessionOwner::class.java)
                return ContextSessionOwner({ openContextSession(appContext) }) as T
            }
        })[ContextSessionOwner::class.java]
        val access = ViewModelProvider(this)[DemoAccessViewModel::class.java]
        val connectivity = AndroidConnectivityObserver(appContext)
        setContent {
            CapturaProspectosG6Theme {
                val session by owner.uiState.collectAsStateWithLifecycle()
                val accessState by access.uiState.collectAsStateWithLifecycle()
                Scaffold(Modifier.fillMaxSize()) { padding ->
                    val modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                    if (accessState.welcome) WelcomeScreen(access::begin, modifier)
                    else when (val current = session) {
                        ContextSessionState.Opening -> DemoContextScreen(DemoContextUiState(), {}, {}, {}, {}, {}, modifier)
                        ContextSessionState.Unavailable -> DemoContextScreen(
                            DemoContextUiState(DemoContextState.StorageUnavailable), {}, owner::retry, {}, {}, {}, modifier)
                        is ContextSessionState.Ready -> {
                            val contextUi by current.viewModel.uiState.collectAsStateWithLifecycle()
                            val snapshot = (contextUi.context as? DemoContextState.Ready)?.snapshot
                            val profile = snapshot?.currentProfile
                            val event = snapshot?.selectedEvent
                            when {
                                snapshot == null -> DemoContextRoute(current.viewModel, modifier, showProfiles = false)
                                accessState.actorId == null || profile == null || profile.actor.id != accessState.actorId -> DemoAccessRoute(
                                    access, true, contextUi.busy,
                                    { email, password -> access.submit(email, password, current.viewModel) }, modifier)
                                else -> {
                                    var active by rememberSaveable(accessState.actorId) { mutableStateOf(AppScreen.CONTEXT_SELECTOR) }
                                    val dataVm = owner.data()
                                    val data by dataVm.uiState.collectAsStateWithLifecycle()
                                    LaunchedEffect(profile.actor.id, event?.id) { dataVm.clearMessage() }
                                    val canNavigate = !data.busy && !contextUi.busy && contextUi.eventEditor == null
                                    BackHandler(active in setOf(AppScreen.SUMMARY, AppScreen.PROFILE)) {
                                        if (canNavigate) active = AppScreen.HUB
                                    }
                                    if (active == AppScreen.CONTEXT_SELECTOR || event == null) {
                                        DemoContextRoute(current.viewModel, modifier,
                                            onContinueToHub = { active = AppScreen.HUB }, showProfiles = false)
                                    } else {
                                        val env = remember(profile.actor.id, event.id) {
                                            AdmissionEnvironment(CaptureEnvironment(profile.actor, event, snapshot.deviceId,
                                                emptyList(), snapshot.consentVersion, snapshot.consentText),
                                                snapshot.catalog.programs, snapshot.catalog.sites)
                                        }
                                        when (active) {
                                            AppScreen.FORM -> {
                                                val form = owner.form(env, snapshot.establishmentSuggestion)
                                                Box(modifier, contentAlignment = Alignment.TopCenter) {
                                                    AdmissionFormRoute(form, Modifier.widthIn(max = 840.dp).fillMaxSize(),
                                                        onNavigateBack = { if (owner.releaseForm()) active = AppScreen.HUB })
                                                }
                                            }
                                            AppScreen.TRAY -> {
                                                val tray = owner.tray(profile.actor)
                                                val trayState by tray.uiState.collectAsStateWithLifecycle()
                                                val trayNavigation = canNavigate && !trayState.isMutating &&
                                                    !trayState.isDetailOpen && !trayState.isReadingDetail &&
                                                    !trayState.isEditing && trayState.targetForCancellation == null
                                                AppNavigation(active, trayNavigation, { destination ->
                                                    if (trayNavigation && owner.releaseTray()) active = destination
                                                }, modifier) { body ->
                                                    io.github.grupo6.capturaprospectos.ui.records.RecordTrayRoute(tray,
                                                        onNavigateBack = { if (owner.releaseTray()) active = AppScreen.HUB },
                                                        modifier = body)
                                                }
                                            }
                                            else -> AppNavigation(active, canNavigate, { active = it }, modifier) { body ->
                                                if (active == AppScreen.PROFILE) ProfileScreen(profile, event, data,
                                                    onChangeEvent = { active = AppScreen.CONTEXT_SELECTOR },
                                                    onLoadData = { dataVm.load(env) },
                                                    onLogout = {
                                                        if (canNavigate && owner.releaseForm() && owner.releaseTray()) {
                                                            current.viewModel.clearSelection()
                                                            access.logout()
                                                        }
                                                    }, modifier = body)
                                                else MainHubRoute(profile, event, connectivity,
                                                    onNewCapture = { active = AppScreen.FORM },
                                                    onChangeContext = { active = AppScreen.CONTEXT_SELECTOR },
                                                    modifier = body,
                                                    onOpenTray = { active = AppScreen.TRAY },
                                                    recordRepository = current.recordRepository,
                                                    suppliedMetrics = owner.hub(profile.actor, event),
                                                    contextViewModel = current.viewModel,
                                                    summaryOnly = active == AppScreen.SUMMARY,
                                                    onOpenSummary = { active = AppScreen.SUMMARY })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
