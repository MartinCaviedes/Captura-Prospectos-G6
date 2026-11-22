package io.github.grupo6.capturaprospectos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.grupo6.capturaprospectos.data.session.AndroidOfflineClock
import io.github.grupo6.capturaprospectos.data.session.AndroidOfflineSessionStore
import io.github.grupo6.capturaprospectos.data.session.AndroidRememberedSessionStore
import io.github.grupo6.capturaprospectos.domain.device.ConnectivityStatus
import io.github.grupo6.capturaprospectos.domain.admission.AdmissionEnvironment
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEnvironment
import io.github.grupo6.capturaprospectos.domain.context.DemoContextState
import io.github.grupo6.capturaprospectos.domain.records.SyncIdentity
import io.github.grupo6.capturaprospectos.ui.admission.AdmissionFormRoute
import io.github.grupo6.capturaprospectos.ui.context.*
import io.github.grupo6.capturaprospectos.ui.access.*
import io.github.grupo6.capturaprospectos.ui.hub.MainHubRoute
import io.github.grupo6.capturaprospectos.ui.shell.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme
import io.github.grupo6.capturaprospectos.ui.theme.AppearanceViewModel
import io.github.grupo6.capturaprospectos.ui.theme.applySystemBarAppearance
import io.github.grupo6.capturaprospectos.data.profile.AndroidPersonalProfileStore
import io.github.grupo6.capturaprospectos.ui.profile.*

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
        val access = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == DemoAccessViewModel::class.java)
                return DemoAccessViewModel(offlineStore = AndroidOfflineSessionStore(appContext),
                    clock = AndroidOfflineClock(appContext), rememberedStore = AndroidRememberedSessionStore(appContext)) as T
            }
        })[DemoAccessViewModel::class.java]
        val usersAdmin = ViewModelProvider(this)[UserAdminViewModel::class.java]
        val appearance = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == AppearanceViewModel::class.java)
                return AppearanceViewModel(appContext.getSharedPreferences("appearance", MODE_PRIVATE)) as T
            }
        })[AppearanceViewModel::class.java]
        val personalProfile=ViewModelProvider(this,object:ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T {
                require(modelClass==PersonalProfileViewModel::class.java)
                return PersonalProfileViewModel(AndroidPersonalProfileStore(appContext),
                    isCurrent={id ->access.uiState.value.account?.id==id}) as T
            }
        })[PersonalProfileViewModel::class.java]
        val connectivity = AndroidConnectivityObserver(appContext)
        setContent {
            val darkMode by appearance.darkMode.collectAsStateWithLifecycle()
            SideEffect { applySystemBarAppearance(window, darkMode) }
            CapturaProspectosG6Theme(darkTheme = darkMode) {
                val session by owner.uiState.collectAsStateWithLifecycle()
                val accessState by access.uiState.collectAsStateWithLifecycle()
                val personalState by personalProfile.uiState.collectAsStateWithLifecycle()
                var showPersonalProfile by rememberSaveable(accessState.account?.id) {mutableStateOf(false)}
                LaunchedEffect(accessState.account?.id) {personalProfile.openFor(accessState.account?.id)}
                if(showPersonalProfile && accessState.account?.role=="ADMIN") {
                    val account=requireNotNull(accessState.account)
                    AlertDialog(onDismissRequest={showPersonalProfile=false},title={Text("Mi perfil")},
                        text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState())) {
                            PersonalProfileRoute(personalProfile,account.id,account.displayName,account.email,"Administrador")
                        }},confirmButton={TextButton({showPersonalProfile=false}) {Text("Cerrar")}})
                }
                val syncState by owner.synchronizationState.collectAsStateWithLifecycle()
                val syncUnavailable by owner.synchronizationUnavailable.collectAsStateWithLifecycle()
                var showSyncIssues by remember { mutableStateOf(false) }
                LaunchedEffect(accessState.account?.id) { showSyncIssues=false }
                LaunchedEffect(session,accessState.account?.id,accessState.offline) {
                    val account=accessState.account
                    if(session is ContextSessionState.Ready && account!=null && account.role in listOf("CAPTADOR","SUPERVISOR")) {
                        val identity=SyncIdentity(account.id,requireNotNull(account.role),account.siteIds.toSet())
                        owner.synchronize(identity,authorized={
                            val live=access.uiState.value
                            live.account?.id==identity.id && live.account?.role==identity.role && !live.offline
                        },onInvalid=access::invalidate)
                    }
                }
                if(showSyncIssues && accessState.account!=null) AlertDialog(
                    onDismissRequest={showSyncIssues=false},title={Text("Estado de sincronización")},
                    text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())) {
                        Text("${syncState.pending} operaciones pendientes. Los datos permanecen guardados en este dispositivo.")
                        if(syncState.legacy>0) Text("${syncState.legacy} fichas anteriores requieren conciliación para evitar duplicados.")
                        syncState.issues.distinctBy { it.captureId }.take(10).forEach {
                            Text("${it.label}: "+when(it.code) {
                                "DUPLICATE_EVENT_REGISTRATION" -> "ya existe una inscripción en este evento."
                                "LEGACY_REVIEW_REQUIRED" -> "requiere conciliación con las fichas anteriores."
                                "SYNC_VERSION_CONFLICT", "SYNC_REPLAY_CONFLICT", "CONCURRENT_UPDATE" -> "hay cambios en el servidor; se conservó tu versión local."
                                else -> "el servidor no confirmó el envío; requiere revisión."
                            })
                        }
                        if(syncUnavailable) Text("No se pudo consultar el almacenamiento. Se volverá a intentar.")
                    }},confirmButton={TextButton({showSyncIssues=false}) {Text("Cerrar")}})
                var networkStatus by remember { mutableStateOf(ConnectivityStatus.UNKNOWN) }
                LaunchedEffect(connectivity) { connectivity.observe().collect { networkStatus = it; access.networkChanged(it) } }
                (session as? ContextSessionState.Ready)?.let { ready ->
                    val restoreContext by ready.viewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(restoreContext.context,restoreContext.busy,networkStatus) {
                        access.restoreRemembered(ready.viewModel)
                    }
                }
                val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
                DisposableEffect(lifecycle) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            access.revalidate()
                            (owner.uiState.value as? ContextSessionState.Ready)?.let { access.restoreRemembered(it.viewModel) }
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }
                Scaffold(Modifier.fillMaxSize(), topBar = {
                    if (accessState.account != null && (accessState.sessionNotice!=null || syncState.pending>0 || syncState.issues.isNotEmpty() || syncState.legacy>0 || syncUnavailable)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                                accessState.sessionNotice?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
                                TextButton({showSyncIssues=true}) {
                                    Text(if(syncUnavailable) "Sincronización no disponible" else "Sincronización: ${syncState.pending} pendientes · Ver estado")
                                }
                            }
                        }
                    }
                }) { padding ->
                    val modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                    if (accessState.welcome) WelcomeScreen(access::begin, modifier)
                    else when (val current = session) {
                        ContextSessionState.Opening -> DemoContextScreen(DemoContextUiState(), {}, {}, {}, {}, {}, modifier)
                        ContextSessionState.Unavailable -> DemoContextScreen(
                            DemoContextUiState(DemoContextState.StorageUnavailable), {}, owner::retry, {}, {}, {}, modifier)
                        is ContextSessionState.Ready -> {
                            val contextUi by current.viewModel.uiState.collectAsStateWithLifecycle()
                            LaunchedEffect(contextUi.context, contextUi.busy) {
                                if (contextUi.context == DemoContextState.NotInitialized && !contextUi.busy) {
                                    current.viewModel.ensureCatalogInitialized()
                                }
                            }
                            val snapshot = (contextUi.context as? DemoContextState.Ready)?.snapshot
                            val profile = snapshot?.currentProfile
                            val event = snapshot?.selectedEvent
                            when {
                                accessState.account?.role == "ADMIN" -> UserAdminRoute(
                                    model = usersAdmin, account = requireNotNull(accessState.account),
                                    onLogout = { usersAdmin.reset(); current.viewModel.clearSelection(); access.logout() },
                                    onSessionError = { usersAdmin.reset(); access.invalidate(it) }, modifier = modifier,
                                    onOpenProfile={showPersonalProfile=true})
                                snapshot == null -> DemoContextRoute(current.viewModel, modifier, showProfiles = false)
                                accessState.actorId == null || profile == null || profile.actor.id != accessState.actorId -> DemoAccessRoute(
                                    model = access,
                                    ready = true,
                                    contextBusy = contextUi.busy,
                                    onSubmit = { email, password -> access.submit(email, password, current.viewModel) },
                                    onResumeOffline = { access.resumeOffline(current.viewModel) },
                                    modifier = modifier,
                                )
                                else -> {
                                    var active by rememberSaveable(accessState.actorId) { mutableStateOf(AppScreen.CONTEXT_SELECTOR) }
                                    val canNavigate = !contextUi.busy && contextUi.eventEditor == null
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
                                        androidx.compose.animation.Crossfade(targetState = active, label = "app_screen_transition") { currentScreen ->
                                            when (currentScreen) {
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
                                                    AppNavigation(currentScreen, trayNavigation, { destination ->
                                                        if (trayNavigation && owner.releaseTray()) active = destination
                                                    }, modifier) { body ->
                                                        io.github.grupo6.capturaprospectos.ui.records.RecordTrayRoute(tray,
                                                            onNavigateBack = { if (owner.releaseTray()) active = AppScreen.HUB },
                                                            modifier = body)
                                                    }
                                                }
                                                else -> AppNavigation(currentScreen, canNavigate, { active = it }, modifier) { body ->
                                                    if (currentScreen == AppScreen.PROFILE) ProfileScreen(
                                                        profile = profile,
                                                        event = event,
                                                        onChangeEvent = { active = AppScreen.CONTEXT_SELECTOR },
                                                        onLogout = {
                                                            if (canNavigate && owner.releaseForm() && owner.releaseTray()) {
                                                                current.viewModel.clearSelection()
                                                                access.logout()
                                                            }
                                                        },
                                                        modifier = body,
                                                        darkMode = darkMode,
                                                        onDarkModeChange = appearance::setDarkMode,
                                                        personalization={PersonalProfileRoute(personalProfile,profile.actor.id.toString(),profile.displayName,profile.email,
                                                            if(profile.actor.role==io.github.grupo6.capturaprospectos.domain.capture.LocalRole.SUPERVISOR) "Supervisor" else "Captador")},
                                                    )
                                                    else MainHubRoute(profile.copy(displayName=if(personalState.ownerId==profile.actor.id.toString() && personalState.ready)
                                                        personalState.data.nickname.ifBlank {profile.displayName} else profile.displayName), event, connectivity,
                                                        onNewCapture = { active = AppScreen.FORM },
                                                        onChangeContext = { active = AppScreen.CONTEXT_SELECTOR },
                                                        modifier = body,
                                                        onOpenTray = { active = AppScreen.TRAY },
                                                        recordRepository = current.recordRepository,
                                                        suppliedMetrics = owner.hub(profile.actor, event),
                                                        contextViewModel = current.viewModel,
                                                        summaryOnly = currentScreen == AppScreen.SUMMARY,
                                                        onOpenSummary = { active = AppScreen.SUMMARY },
                                                        profilePhoto = personalState.data.photoJpeg.takeIf {
                                                            personalState.ready && personalState.ownerId == profile.actor.id.toString()
                                                        })
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
}
