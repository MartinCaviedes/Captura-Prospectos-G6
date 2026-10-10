package io.github.grupo6.capturaprospectos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.data.device.AndroidConnectivityObserver
import io.github.grupo6.capturaprospectos.data.local.openContextSession
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEnvironment
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormRoute
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormViewModel
import io.github.grupo6.capturaprospectos.ui.context.*
import io.github.grupo6.capturaprospectos.ui.hub.MainHubRoute
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme

enum class AppScreen { CONTEXT_SELECTOR, HUB, FORM, TRAY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext
        val owner = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ContextSessionOwner::class.java)
                return ContextSessionOwner({ openContextSession(appContext) }) as T
            }
        })[ContextSessionOwner::class.java]
        val connectivityObserver = AndroidConnectivityObserver(appContext)

        setContent {
            CapturaProspectosG6Theme {
                val session by owner.uiState.collectAsStateWithLifecycle()
                Scaffold(Modifier.fillMaxSize()) { padding ->
                    val modifier = Modifier.fillMaxSize().padding(padding)
                    when (val current = session) {
                        ContextSessionState.Opening -> DemoContextScreen(DemoContextUiState(), {}, {}, {}, {}, {}, modifier)
                        ContextSessionState.Unavailable -> DemoContextScreen(
                            DemoContextUiState(DemoContextState.StorageUnavailable), {}, owner::retry, {}, {}, {}, modifier)
                        is ContextSessionState.Ready -> {
                            val contextUi by current.viewModel.uiState.collectAsStateWithLifecycle()
                            val readyState = contextUi.context as? DemoContextState.Ready
                            val snapshot = readyState?.snapshot
                            val profile = snapshot?.currentProfile
                            val event = snapshot?.selectedEvent

                            if (profile == null || event == null) {
                                DemoContextRoute(
                                    viewModel = current.viewModel,
                                    modifier = modifier,
                                )
                            } else {
                                var activeScreen by rememberSaveable(profile.actor.id, event.id) {
                                    mutableStateOf(AppScreen.HUB)
                                }
                                when (activeScreen) {
                                    AppScreen.CONTEXT_SELECTOR -> DemoContextRoute(
                                        viewModel = current.viewModel,
                                        modifier = modifier,
                                        onContinueToHub = { activeScreen = AppScreen.HUB },
                                    )
                                    AppScreen.HUB -> MainHubRoute(
                                        profile = profile,
                                        event = event,
                                        connectivityObserver = connectivityObserver,
                                        onNewCapture = { activeScreen = AppScreen.FORM },
                                        onChangeContext = { activeScreen = AppScreen.CONTEXT_SELECTOR },
                                        onOpenTray = { activeScreen = AppScreen.TRAY },
                                        modifier = modifier,
                                    )
                                    AppScreen.FORM -> {
                                        val env = remember(profile.actor.id, event.id) {
                                            AdmissionEnvironment(
                                                context = CaptureEnvironment(
                                                    actor = profile.actor,
                                                    event = event,
                                                    deviceId = snapshot.deviceId,
                                                    captures = emptyList(),
                                                    consentVersion = snapshot.consentVersion,
                                                    consentText = snapshot.consentText,
                                                ),
                                                programs = snapshot.catalog.programs,
                                                sites = snapshot.catalog.sites,
                                            )
                                        }
                                        val formVm = remember(profile.actor.id, event.id, current.admissionRepository) {
                                            AdmissionFormViewModel(
                                                environment = env,
                                                suggestion = snapshot.establishmentSuggestion,
                                                repository = current.admissionRepository,
                                            )
                                        }
                                        AdmissionFormRoute(
                                            vm = formVm,
                                            modifier = modifier,
                                            onNavigateBack = {
                                                formVm.discard()
                                                activeScreen = AppScreen.HUB
                                            },
                                        )
                                    }
                                    AppScreen.TRAY -> {
                                        val trayVm = remember(profile.actor.id, event.id, current.recordRepository) {
                                            io.github.grupo6.capturaprospectos.ui.records.RecordTrayViewModel(
                                                repository = current.recordRepository,
                                            )
                                        }
                                        io.github.grupo6.capturaprospectos.ui.records.RecordTrayRoute(
                                            viewModel = trayVm,
                                            onNavigateBack = { activeScreen = AppScreen.HUB },
                                            modifier = modifier,
                                        )
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
