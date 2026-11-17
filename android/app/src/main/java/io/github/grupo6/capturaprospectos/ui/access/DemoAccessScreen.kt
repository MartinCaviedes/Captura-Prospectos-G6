package io.github.grupo6.capturaprospectos.ui.access

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.R
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.ui.components.*
import io.github.grupo6.capturaprospectos.ui.theme.G6Navy

@Composable fun WelcomeScreen(onBegin: () -> Unit, modifier: Modifier = Modifier) {
    ConexionesBackground(modifier) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max=600.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ProductIdentity()
            GlassCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("Datos de contacto", "Intereses académicos", "Consentimiento").forEachIndexed { index, label ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary) {
                                        Text("0${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    Text("Conecta personas.\nAbre futuros.",style=MaterialTheme.typography.headlineLarge,
                        modifier=Modifier.semantics { heading() })
                    Text("Conecta el interés de hoy con las oportunidades de mañana.")
                    GlassCard {
                        Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            DesignIcon(R.drawable.g6_welcome_frame)
                            Text("Tu jornada, en un solo lugar.",style=MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Text("El siguiente paso comienza aquí.",style=MaterialTheme.typography.titleMedium)
            Text("Registra prospectos, organiza tus eventos y revisa tus resultados.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onBegin,Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Comenzar") }
            Text("Captura · Organiza · Acompaña",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Conecta el acceso con su dueño de estado de Activity. */
@Composable fun DemoAccessRoute(
    model: DemoAccessViewModel,
    ready: Boolean,
    contextBusy: Boolean,
    onSubmit: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    availableSites: List<io.github.grupo6.capturaprospectos.domain.admission.SiteOption> = emptyList(),
) {
    val state by model.uiState.collectAsStateWithLifecycle()
    DemoAccessScreen(
        state = state,
        ready = ready,
        contextBusy = contextBusy,
        onSubmit = { model.submitDraft(onSubmit) },
        onEmailChange = model::editEmail,
        onPasswordChange = model::editPassword,
        onSelectSample = model::selectSample,
        modifier = modifier,
        onDisplayNameChange = model::editDisplayName,
        onRoleSelected = model::selectRole,
        onToggleSite = model::toggleSite,
        onToggleRegisterMode = model::toggleRegisterMode,
        availableSites = availableSites,
    )
}

@Composable fun DemoAccessScreen(
    state: DemoAccessState,
    ready: Boolean,
    contextBusy: Boolean,
    onSubmit: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectSample: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onDisplayNameChange: (String) -> Unit = {},
    onRoleSelected: (io.github.grupo6.capturaprospectos.domain.capture.LocalRole) -> Unit = {},
    onToggleSite: (java.util.UUID) -> Unit = {},
    onToggleRegisterMode: () -> Unit = {},
    availableSites: List<io.github.grupo6.capturaprospectos.domain.admission.SiteOption> = emptyList(),
) {
    var showPassword by remember { mutableStateOf(false) }
    val enabled = ready && !state.busy && !contextBusy
    ConexionesBackground(modifier) {
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProductIdentity()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (state.registerMode) "Crear cuenta" else "Bienvenido",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    if (state.registerMode) "Registra tus datos para comenzar tu jornada."
                    else "Ingresa para comenzar tu jornada.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlassCard {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnimatedVisibility(visible = state.registerMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.displayName,
                                onValueChange = onDisplayNameChange,
                                label = { Text("Nombre completo") },
                                singleLine = true,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Rol en terreno", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                io.github.grupo6.capturaprospectos.domain.capture.LocalRole.entries.forEach { role ->
                                    val label = if (role == io.github.grupo6.capturaprospectos.domain.capture.LocalRole.CAPTOR) "Captador" else "Supervisor"
                                    FilterChip(
                                        selected = state.selectedRole == role,
                                        onClick = { onRoleSelected(role) },
                                        enabled = enabled,
                                        label = { Text(label) },
                                    )
                                }
                            }
                            if (availableSites.isNotEmpty()) {
                                Text("Sedes asignadas", style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    availableSites.forEach { site ->
                                        FilterChip(
                                            selected = site.id in state.selectedSiteIds,
                                            onClick = { onToggleSite(site.id) },
                                            enabled = enabled,
                                            label = { Text(site.name) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        trailingIcon = {
                            IconButton({ showPassword = !showPassword }, enabled = enabled) {
                                DesignIcon(
                                    R.drawable.g6_login_frame1,
                                    description = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                )
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = state.error != null) {
                        state.error?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Button(
                        onClick = onSubmit,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(if (state.registerMode) "Registrar y entrar" else "Iniciar sesión")
                    }
                    TextButton(
                        onClick = onToggleRegisterMode,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.registerMode) "¿Ya tienes cuenta? Iniciar sesión"
                            else "¿No tienes cuenta? Crear cuenta"
                        )
                    }
                }
            }
        }
    }
}
