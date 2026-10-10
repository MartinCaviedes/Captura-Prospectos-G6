package io.github.grupo6.capturaprospectos.ui.admission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import java.text.Normalizer
import java.util.UUID

class FormActions(
    val text: (AdmissionField, String) -> Unit, val activity: (CurrentActivity) -> Unit,
    val discardActivity: (Boolean) -> Unit, val suggestion: () -> Unit,
    val personal: (PersonalOption?) -> Unit, val schedule: (PreferredSchedule?) -> Unit,
    val level: (InterestLevel?) -> Unit, val motivation: (Motivation?) -> Unit,
    val program: (UUID) -> Unit, val move: (UUID, Int) -> Unit, val mention: (UUID, UUID?) -> Unit,
    val site: (UUID) -> Unit, val mode: (AcceptanceMode) -> Unit, val consent: (Boolean) -> Unit,
    val validate: (SaveIntent) -> Unit, val discard: () -> Unit,
    val save: ((SaveIntent) -> Unit)? = null,
    val reset: (() -> Unit)? = null,
    val onBack: (() -> Unit)? = null,
    val requestLocation: (() -> Unit)? = null,
    val confirmEventLocation: (() -> Unit)? = null,
    val clearLocation: (() -> Unit)? = null,
    val openQrScanner: (() -> Unit)? = null,
    val closeQrScanner: (() -> Unit)? = null,
    val processQrPayload: ((String) -> Unit)? = null,
    val applyQrCandidate: (() -> Unit)? = null,
    val dismissQrError: (() -> Unit)? = null,
)

/** Compone el formulario a partir del estado y las acciones del ViewModel.
 * La navegación y la confirmación de guardado pertenecen al consumidor. */
@Composable fun AdmissionFormRoute(
    vm: AdmissionFormViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    AdmissionFormComponents(state, vm.environment, vm.suggestion, remember(vm, onNavigateBack) {
        FormActions(
            vm::text, vm::activity, vm::confirmActivityDiscard, vm::acceptEstablishmentSuggestion,
            vm::personal, vm::schedule, vm::interestLevel, vm::motivation, vm::program, vm::moveProgram,
            vm::mention, vm::site, vm::mode, vm::consent, vm::validate, vm::discard,
            save = vm::save, reset = vm::reset, onBack = onNavigateBack,
            requestLocation = { vm.requestLocation() },
            confirmEventLocation = vm::confirmEventLocation,
            clearLocation = vm::clearLocation,
            openQrScanner = vm::openQrScanner,
            closeQrScanner = vm::closeQrScanner,
            processQrPayload = vm::processQrPayload,
            applyQrCandidate = vm::applyQrCandidate,
            dismissQrError = vm::dismissQrError,
        )
    }, modifier)
}

@Composable fun AdmissionFormComponents(
    state: AdmissionFormState, environment: AdmissionEnvironment,
    suggestion: EstablishmentSuggestion?, actions: FormActions, modifier: Modifier = Modifier,
) {
    // La búsqueda se conserva sólo durante esta composición para evitar restaurar datos personales.
    var search by remember { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val input = state.input

    if (state.pendingActivity != null) AlertDialog(onDismissRequest = { actions.discardActivity(false) },
        title = { Text("¿Cambiar actividad?") }, text = { Text("Se quitará el detalle escrito para Otra actividad.") },
        confirmButton = { TextButton(onClick = { actions.discardActivity(true) }) { Text("Cambiar y quitar detalle") } },
        dismissButton = { TextButton(onClick = { actions.discardActivity(false) }) { Text("Conservar") } })

    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text("¿Descartar formulario?") }, text = { Text("Se quitarán los datos que aún están sólo en memoria.") },
        confirmButton = { TextButton(onClick = { actions.discard(); search = ""; confirmDiscard = false; actions.onBack?.invoke() }) { Text("Descartar datos") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } })

    // Modal de asistencia por cámara y lectura de QR sintético (RF11)
    if (state.isQrScannerOpen) {
        var testPayload by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { actions.closeQrScanner?.invoke() },
            title = { Text("Lectura de código QR") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enfoca un código QR de formato sintético G6DEMO:1. Los campos reconocidos requieren tu confirmación antes de rellenar el formulario.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (state.qrCandidate != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Candidato reconocido:", style = MaterialTheme.typography.titleSmall)
                                Text("Nombre: ${state.qrCandidate.fullName}", style = MaterialTheme.typography.bodyMedium)
                                Text("RUT: ${state.qrCandidate.rut}", style = MaterialTheme.typography.bodyMedium)
                                Text("Confirma para cargar al formulario sin alterar campos manuales.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (state.qrError != null) {
                        Text(state.qrError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = testPayload,
                        onValueChange = { testPayload = it },
                        label = { Text("Contenido del código QR") },
                        placeholder = { Text("G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { actions.processQrPayload?.invoke(testPayload) },
                            enabled = testPayload.isNotBlank(),
                        ) {
                            Text("Procesar QR")
                        }
                        OutlinedButton(
                            onClick = {
                                testPayload = "G6DEMO:1:{\"fullName\":\"Persona Ficticia Alfa\",\"rut\":\"12345678-5\"}"
                                actions.processQrPayload?.invoke(testPayload)
                            },
                        ) {
                            Text("Cargar fixture")
                        }
                    }
                }
            },
            confirmButton = {
                if (state.qrCandidate != null) {
                    Button(onClick = { actions.applyQrCandidate?.invoke() }) {
                        Text("Aplicar al formulario")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { actions.closeQrScanner?.invoke() }) {
                    Text("Cerrar")
                }
            },
        )
    }

    LazyColumn(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Inscripción de prospecto", style = MaterialTheme.typography.headlineMedium)
            Text("Los datos permanecen sin guardar. Validar no guarda ni envía la inscripción.")
        }

        // Asistente de escaneo QR (RF11)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Asistencia por cámara (QR sintético)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lee un código QR de formato sintético (G6DEMO:1) para precargar nombres y RUT tras confirmación manual. No almacena imágenes ni datos crudos.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { actions.openQrScanner?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Escanear código QR asistido")
                    }
                }
            }
        }

        item { FormText("Nombres", AdmissionField.GIVEN_NAMES, input.givenNames, state, actions) }
        item { FormText("Apellidos", AdmissionField.FAMILY_NAMES, input.familyNames, state, actions) }
        item { FormText("RUT", AdmissionField.RUT, input.rut, state, actions) }
        item { FormText("Correo electrónico", AdmissionField.EMAIL, input.email, state, actions, KeyboardType.Email) }
        item { FormText("Teléfono", AdmissionField.PHONE, input.phone, state, actions, KeyboardType.Phone) }
        item { FormText("Nacimiento (aaaa-mm-dd)", AdmissionField.BIRTH_DATE, input.birthDate, state, actions) }
        item { FormText("Comuna de residencia", AdmissionField.RESIDENCE_COMMUNE, input.residenceCommune, state, actions) }
        item { Choices("Actividad actual", CurrentActivity.entries, input.currentActivity, ::activityLabel, actions.activity) }
        if (input.currentActivity == CurrentActivity.OTHER) item { FormText("Detalle de otra actividad", AdmissionField.ACTIVITY_OTHER, input.currentActivityOther.orEmpty(), state, actions) }
        if (suggestion != null) item {
            Text("Establecimiento indicado en el evento: ${suggestion.name}. Confirma si corresponde.")
            OutlinedButton(onClick = actions.suggestion) { Text("Usar establecimiento del evento") }
        }
        item { FormText("Establecimiento", AdmissionField.ESTABLISHMENT, input.establishment.orEmpty(), state, actions) }
        item { FormText("Comuna del establecimiento (opcional)", AdmissionField.ESTABLISHMENT_COMMUNE, input.establishmentCommune.orEmpty(), state, actions) }
        item { OptionalChoices("Opción personal (opcional)", PersonalOption.entries, input.formPersonalOption,
            { when (it) { PersonalOption.FEMALE -> "Femenino"; PersonalOption.MALE -> "Masculino"; PersonalOption.NOT_STATED -> "No indica" } }, actions.personal) }
        item { OptionalChoices("Jornada (opcional)", PreferredSchedule.entries, input.preferredSchedule,
            { if (it == PreferredSchedule.DAY) "Diurna" else "Vespertina" }, actions.schedule) }
        item { OptionalChoices("Nivel de interés (opcional)", InterestLevel.entries, input.interestLevel,
            { when (it) { InterestLevel.HIGH -> "Alto"; InterestLevel.MEDIUM -> "Medio"; InterestLevel.LOW -> "Bajo" } }, actions.level) }
        item { OptionalChoices("Motivación (opcional)", Motivation.entries, input.motivation, ::motivationLabel, actions.motivation) }
        item { Text("Carreras de interés · máximo 3, en orden de prioridad", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(search, { search = it }, label = { Text("Buscar carrera") }, modifier = Modifier.fillMaxWidth()) }
        item {
            input.interests.forEach { selected ->
                val program = environment.programs.firstOrNull { it.id == selected.programId }
                Text("${selected.priority}. ${program?.name ?: "Carrera no disponible"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ actions.move(selected.programId, -1) }, enabled = selected.priority > 1) { Text("Subir") }
                    TextButton({ actions.move(selected.programId, 1) }, enabled = selected.priority < input.interests.size) { Text("Bajar") }
                    TextButton({ actions.program(selected.programId) }) { Text("Quitar") }
                }
                if (program != null && program.mentions.isNotEmpty()) OptionalChoices("Mención de ${program.name}",
                    program.mentions.keys.toList(), selected.mentionId, { program.mentions.getValue(it) }, { actions.mention(program.id, it) })
            }
            environment.programs.filter { it.enabled && normalized(it.name).contains(normalized(search)) }.forEach { program ->
                val selected = input.interests.any { it.programId == program.id }
                FilterChip(selected, { actions.program(program.id) }, label = { Text(program.name) },
                    enabled = selected || input.interests.size < 3, modifier = Modifier.fillMaxWidth())
            }
        }
        item { Text("Sedes de interés (opcional, independientes del evento)", style = MaterialTheme.typography.titleMedium)
            environment.sites.filter { it.enabled }.forEach { site -> FilterChip(site.id in input.preferredSiteIds,
                { actions.site(site.id) }, label = { Text(site.name) }, modifier = Modifier.fillMaxWidth()) } }
        item { Text("Modalidad y canal de contacto se habilitarán cuando exista un catálogo definido.") }
        item { FormText("Notas (opcional, máximo 500; no amplían la motivación)", AdmissionField.NOTES, input.notes.orEmpty(), state, actions) }

        // Ubicación de captura (RF04)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ubicación de captura (RF04)", style = MaterialTheme.typography.titleMedium)
                    val loc = state.location
                    when (loc) {
                        is CaptureLocation.Device -> {
                            Text(
                                "Ubicación GPS confirmada (${loc.coordinates.latitude}, ${loc.coordinates.longitude}) · Precisión: ${loc.accuracyMeters.toInt()}m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            OutlinedButton(onClick = { actions.clearLocation?.invoke() }) {
                                Text("Quitar ubicación")
                            }
                        }
                        is CaptureLocation.EventConfirmed -> {
                            Text(
                                "Ubicación confirmada según sede del evento (${loc.coordinates.latitude}, ${loc.coordinates.longitude})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            OutlinedButton(onClick = { actions.clearLocation?.invoke() }) {
                                Text("Quitar ubicación")
                            }
                        }
                        null -> {
                            Text(
                                "Sin ubicación asociada. La captura completa se guardará como NEEDS_CONTEXT si no se confirma ubicación.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (state.isAcquiringLocation) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Adquiriendo señal GPS (máximo 10s)…", style = MaterialTheme.typography.bodySmall)
                    }

                    state.locationStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { actions.requestLocation?.invoke() },
                            enabled = !state.isAcquiringLocation,
                        ) {
                            Text("Adquirir GPS")
                        }
                        OutlinedButton(
                            onClick = { actions.confirmEventLocation?.invoke() },
                            enabled = !state.isAcquiringLocation && environment.context.event.location != null,
                        ) {
                            Text("Confirmar evento")
                        }
                    }
                }
            }
        }

        // Consentimiento
        item { Text("Consentimiento", style = MaterialTheme.typography.titleMedium)
            Text(environment.context.consentText)
            Choices("Forma de aceptación", AcceptanceMode.entries, state.mode,
                { if (it == AcceptanceMode.DIRECT) "Directa" else "Verbal registrada" }, actions.mode)
            Row(Modifier.fillMaxWidth().toggleable(value = state.consent != null, enabled = state.mode != null,
                role = Role.Checkbox, onValueChange = actions.consent)) {
                Checkbox(state.consent != null, null, enabled = state.mode != null); Text("Consentimiento aceptado")
            }
        }
        item {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Guardando en base de datos local…", style = MaterialTheme.typography.bodyMedium)
            }
            if (state.isSaved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "¡Registro guardado con éxito!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            state.message ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { actions.reset?.invoke() }) {
                                Text("Nueva inscripción")
                            }
                            if (actions.onBack != null) {
                                OutlinedButton(onClick = { actions.onBack.invoke() }) {
                                    Text("Volver al panel")
                                }
                            }
                        }
                    }
                }
            } else {
                state.issues.forEach { Text("${fieldLabel(it.field)}: ${issueLabel(it.code)}", color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (actions.save != null) {
                    Button(
                        onClick = { actions.save.invoke(SaveIntent.CONFIRM_REGISTRATION) },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Guardar inscripción")
                    }
                    OutlinedButton(
                        onClick = { actions.save.invoke(SaveIntent.SAVE_DRAFT) },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Guardar borrador")
                    }
                }
                Button({ actions.validate(SaveIntent.CONFIRM_REGISTRATION) }, modifier = Modifier.fillMaxWidth()) { Text("Validar inscripción") }
                OutlinedButton({ actions.validate(SaveIntent.SAVE_DRAFT) }, modifier = Modifier.fillMaxWidth()) { Text("Validar borrador") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ confirmDiscard = true }) { Text("Descartar formulario") }
                    if (actions.onBack != null) {
                        TextButton(onClick = { confirmDiscard = true }) { Text("Volver al panel") }
                    }
                }
            }
        }
    }
}
@Composable private fun FormText(label: String, field: AdmissionField, value: String, state: AdmissionFormState,
    actions: FormActions, keyboard: KeyboardType = KeyboardType.Text) {
    val isPhone = field == AdmissionField.PHONE
    val displayedValue = if (isPhone) value.removePrefix("+56") else value
    OutlinedTextField(displayedValue, { edited ->
        val input = if (isPhone && edited.isNotBlank() && !edited.startsWith("+56")) "+56$edited" else edited
        actions.text(field, input)
    }, label = { Text(label) }, prefix = if (isPhone) ({ Text("+56") }) else null,
        modifier = Modifier.fillMaxWidth(),
        isError = state.issues.any { it.field == field }, keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = field != AdmissionField.NOTES)
}
@Composable private fun <T> Choices(title: String, options: List<T>, selected: T?, label: (T) -> String, action: (T) -> Unit) {
    Column { Text(title); options.forEach { option -> FilterChip(option == selected, { action(option) }, label = { Text(label(option)) }, modifier = Modifier.fillMaxWidth()) } }
}
@Composable private fun <T> OptionalChoices(title: String, options: List<T>, selected: T?, label: (T) -> String, action: (T?) -> Unit) {
    Choices(title, options, selected, label, action)
    TextButton({ action(null) }, enabled = selected != null) { Text("Dejar sin respuesta") }
}
private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
private fun activityLabel(value: CurrentActivity) = when (value) {
    CurrentActivity.FOURTH_SECONDARY -> "4° medio"; CurrentActivity.SECONDARY_GRADUATE -> "Egresado Enseñanza Media"; CurrentActivity.OTHER -> "Otra actividad"
}
private fun motivationLabel(value: Motivation) = when (value) {
    Motivation.INCOME_PROGRESS -> "Estudiar es una inversión para aumentar mis ingresos y progresar con la vida, tanto yo como mi familia."
    Motivation.VOCATION -> "Estudiar es seguir mi vocación para encontrar mi lugar en el mundo laboral. Los ingresos que recibiré no son lo central."
    Motivation.RESUME_OR_NEW_PATH -> "Estudiar es una nueva oportunidad de retomar esa carrera que dejé pendiente, postergué, o de darme la posibilidad de comenzar otra nueva carrera."
    Motivation.FINANCING_CONCERN -> "Estudiar es muy importante, sin embargo, no sé si tendré los ingresos suficientes para financiar mi carrera."
}
private fun fieldLabel(value: AdmissionField) = when (value) {
    AdmissionField.GIVEN_NAMES -> "Nombres"; AdmissionField.FAMILY_NAMES -> "Apellidos"; AdmissionField.RUT -> "RUT"
    AdmissionField.EMAIL -> "Correo"; AdmissionField.PHONE -> "Teléfono"; AdmissionField.BIRTH_DATE -> "Nacimiento"
    AdmissionField.RESIDENCE_COMMUNE -> "Comuna de residencia"; AdmissionField.ACTIVITY -> "Actividad"
    AdmissionField.ACTIVITY_OTHER -> "Otra actividad"; AdmissionField.ESTABLISHMENT -> "Establecimiento"
    AdmissionField.ESTABLISHMENT_COMMUNE -> "Comuna del establecimiento"; AdmissionField.MODALITY -> "Modalidad"
    AdmissionField.CHANNEL -> "Canal"; AdmissionField.NOTES -> "Notas"; AdmissionField.INTERESTS -> "Carreras"
    AdmissionField.SITES -> "Sedes"; AdmissionField.CONSENT -> "Consentimiento"; AdmissionField.LOCATION -> "Ubicación"
}
private fun issueLabel(value: ValidationCode) = when (value) {
    ValidationCode.REQUIRED -> "completa este campo"; ValidationCode.LENGTH -> "revisa la cantidad o extensión"
    ValidationCode.FORMAT -> "revisa el formato"; ValidationCode.UNKNOWN_OPTION -> "elige una opción válida"
    ValidationCode.DUPLICATE -> "hay una selección repetida"; ValidationCode.UNAVAILABLE -> "opción no disponible"
    ValidationCode.INVALID_EVIDENCE -> "la confirmación no es válida"
}
