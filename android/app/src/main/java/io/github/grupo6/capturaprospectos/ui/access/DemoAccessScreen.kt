package io.github.grupo6.capturaprospectos.ui.access

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
                    Image(painterResource(R.drawable.g6_welcome_g6_v3_trayectorias),null,
                        Modifier.fillMaxWidth().aspectRatio(310f/164f),contentScale=ContentScale.Fit)
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
@Composable fun DemoAccessRoute(model: DemoAccessViewModel, ready: Boolean, contextBusy: Boolean,
    onSubmit: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val state by model.uiState.collectAsStateWithLifecycle()
    DemoAccessScreen(state, ready, contextBusy, { model.submitDraft(onSubmit) },
        model::editEmail, model::editPassword, model::selectSample, modifier)
}

@Composable fun DemoAccessScreen(state: DemoAccessState, ready: Boolean, contextBusy: Boolean,
    onSubmit: () -> Unit, onEmailChange: (String) -> Unit, onPasswordChange: (String) -> Unit,
    onSelectSample: (String) -> Unit, modifier: Modifier = Modifier) {
    var showPassword by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    val enabled=ready&&!state.busy&&!contextBusy
    ConexionesBackground(modifier) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max=560.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ProductIdentity()
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Bienvenido",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.semantics { heading() })
                Text("Ingresa para comenzar tu jornada.",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GlassCard {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(state.email,onEmailChange,label={Text("Correo electrónico")},singleLine=true,
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,autoCorrectEnabled=false),
                        enabled=enabled,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(state.password,onPasswordChange,label={Text("Contraseña")},singleLine=true,
                        visualTransformation=if(showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false),
                        trailingIcon={IconButton({showPassword=!showPassword},enabled=enabled) {
                            DesignIcon(R.drawable.g6_login_frame1,description=if(showPassword) "Ocultar contraseña" else "Mostrar contraseña")
                        }},enabled=enabled,modifier=Modifier.fillMaxWidth())
                    state.error?.let { Text(it,color=MaterialTheme.colorScheme.error,
                        modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
                    if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Acceso de muestra",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton({},enabled=false,colors=ButtonDefaults.textButtonColors(
                            disabledContentColor=MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Recuperar acceso",style=MaterialTheme.typography.bodySmall) }
                    }
                    Button(onSubmit,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Iniciar sesión") }
                    Text("o continúa con",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    ProviderButton("Continuar con Google",R.drawable.g6_login_g6_v2_brand_google)
                    ProviderButton("Continuar con Microsoft",R.drawable.g6_login_frame)
                    TextButton({},enabled=false,colors=ButtonDefaults.textButtonColors(disabledContentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text("¿No tienes cuenta? Crear cuenta")
                    }
                    GlassCard(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.background)) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            TextButton({showAccounts=!showAccounts},enabled=enabled,modifier=Modifier.fillMaxWidth().semantics {
                                stateDescription=if(showAccounts) "Expandido" else "Contraído"
                            }) {
                                Text("Cuentas de muestra",Modifier.weight(1f))
                                DesignIcon(R.drawable.g6_login_frame2)
                            }
                            if(showAccounts) DemoAccessFixtures.accounts.forEachIndexed { index,account ->
                                OutlinedButton({onSelectSample(account.email)},enabled=enabled,modifier=Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(listOf("Captador A","Captador B","Supervisor")[index])
                                        Text(account.email,style=MaterialTheme.typography.bodySmall)
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

/** Proveedores representados visualmente, con estado deshabilitado anunciado por accesibilidad. */
@Composable private fun ProviderButton(label: String, resource: Int) {
    OutlinedButton({},enabled=false,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics {
        stateDescription="Disponible en una entrega futura"
    },colors=ButtonDefaults.outlinedButtonColors(disabledContainerColor=Color.White,disabledContentColor=G6Navy),
        border=BorderStroke(1.dp,Color(0xFFC4D0E3))) {
        Image(painterResource(resource),null,Modifier.size(if (resource == R.drawable.g6_login_frame) 21.dp else 20.dp),contentScale=ContentScale.Fit)
        Spacer(Modifier.width(12.dp));Text(label)
    }
}
