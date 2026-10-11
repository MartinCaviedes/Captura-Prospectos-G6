package io.github.grupo6.capturaprospectos.ui.access

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.ui.theme.G6Navy

@Composable fun WelcomeScreen(onBegin: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GRUPO 6 · ADMISIÓN", style = MaterialTheme.typography.labelLarge)
            Card(colors = CardDefaults.cardColors(containerColor = G6Navy, contentColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cada encuentro abre un futuro.", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() })
                    Text("Captura de prospectos", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold))
                    Text("Registra intereses y acompaña la admisión, incluso sin conexión.")
                }
            }
            Text("Organiza tu jornada con claridad.", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text("Captura, consulta y revisa tus registros desde un mismo lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onBegin, Modifier.fillMaxWidth().heightIn(min = 56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)) { Text("Comenzar") }
            Text("E1 · Prueba local con datos ficticios", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable fun DemoAccessScreen(state: DemoAccessState, ready: Boolean, contextBusy: Boolean,
    onSubmit: (String, String) -> Unit, modifier: Modifier = Modifier) {
    // Credenciales transitorias; nunca rememberSaveable ni persistencia.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val enabled = ready && !state.busy && !contextBusy
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Bienvenido", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text("Acceso de demostración · sólo este dispositivo")
            OutlinedTextField(email, { email = it }, label = { Text("Correo demo") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                enabled = enabled, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Contraseña demo") }, singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                trailingIcon = { TextButton({ showPassword = !showPassword }, enabled = enabled) { Text(if (showPassword) "Ocultar" else "Mostrar") } },
                enabled = enabled, modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button({ onSubmit(email, password); password = "" }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary)) { Text("Ingresar a la demo") }
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Credenciales de evaluación", style = MaterialTheme.typography.titleMedium)
                    Text("Contraseña ficticia común: ${DemoAccessFixtures.PASSWORD}")
                    DemoAccessFixtures.accounts.forEach { account ->
                        OutlinedButton({ email = account.email }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(account.email) }
                    }
                    Text("Los botones completan el correo; escribe la contraseña. Este acceso no verifica una identidad real.", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton({}, enabled = false, colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Google · Próximamente") }
            OutlinedButton({}, enabled = false, colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Microsoft · Próximamente") }
            Text("Registro de usuarios: disponible en una entrega futura. Los proveedores externos no están conectados.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
