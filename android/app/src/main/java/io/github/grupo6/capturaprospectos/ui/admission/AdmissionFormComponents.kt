package io.github.grupo6.capturaprospectos.ui.admission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import java.time.LocalDate
import androidx.compose.ui.Alignment
import io.github.grupo6.capturaprospectos.R
import io.github.grupo6.capturaprospectos.ui.components.*
import io.github.grupo6.capturaprospectos.ui.theme.CapturaProspectosG6Theme

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
    val applyQrCandidate: ((Boolean) -> Unit)? = null,
    val cameraUnavailable: (() -> Unit)? = null,
    val confirmDeviceLocation: (() -> Unit)? = null,
    val dismissLocationCandidate: (() -> Unit)? = null,
    val dismissQrError: (() -> Unit)? = null,
)

/** Compone el formulario a partir del estado y las acciones del ViewModel.
 * La navegación y la confirmación de guardado pertenecen al consumidor. */
@Composable fun AdmissionFormRoute(
    vm: AdmissionFormViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onSaveEdit: ((SaveIntent) -> Unit)? = null,
    canSaveEdit: Boolean = true,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val requestLocation = rememberDevicePermissionAction(camera = false) { vm.requestLocation() }
    val openCamera = rememberDevicePermissionAction(camera = true) { vm.openQrScanner() }
    AdmissionFormComponents(state, vm.environment, vm.suggestion, remember(vm, onNavigateBack, requestLocation, openCamera) {
        FormActions(
            vm::text, vm::activity, vm::confirmActivityDiscard, vm::acceptEstablishmentSuggestion,
            vm::personal, vm::schedule, vm::interestLevel, vm::motivation, vm::program, vm::moveProgram,
            vm::mention, vm::site, vm::mode, vm::consent, vm::validate, vm::discard,
            save = { intent -> if (onSaveEdit != null) onSaveEdit(intent) else { vm.save(intent); Unit } }, reset = vm::reset, onBack = onNavigateBack,
            requestLocation = requestLocation,
            confirmEventLocation = vm::confirmEventLocation,
            clearLocation = vm::clearLocation,
            openQrScanner = openCamera,
            closeQrScanner = vm::closeQrScanner,
            processQrPayload = vm::processQrPayload,
            applyQrCandidate = vm::applyQrCandidate,
            cameraUnavailable = vm::cameraUnavailable,
            confirmDeviceLocation = vm::confirmDeviceLocation,
            dismissLocationCandidate = vm::dismissLocationCandidate,
            dismissQrError = vm::dismissQrError,
        )
    }, modifier, editing = vm.initialRecord != null,
        allowDraft = vm.initialRecord == null || vm.initialRecord.state == CaptureState.DRAFT, saveEnabled = canSaveEdit)
}

@Composable fun AdmissionFormComponents(
    state: AdmissionFormState, environment: AdmissionEnvironment,
    suggestion: EstablishmentSuggestion?, actions: FormActions, modifier: Modifier = Modifier,
    editing: Boolean = false, allowDraft: Boolean = true, saveEnabled: Boolean = true,
) {
    CapturaProspectosG6Theme(darkTheme = false) {
        Surface(modifier, color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface) {
            ConexionesBackground(Modifier.fillMaxSize(), light = true) {
                FormContent(state, environment, suggestion, actions, Modifier.fillMaxSize(), editing, allowDraft, saveEnabled)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun FormContent(
    state: AdmissionFormState, environment: AdmissionEnvironment,
    suggestion: EstablishmentSuggestion?, actions: FormActions, modifier: Modifier,
    editing: Boolean, allowDraft: Boolean, saveEnabled: Boolean,
) {
    val listState = rememberLazyListState()
    val fieldPositions = remember { mutableMapOf<AdmissionField, Int>() }
    val requesters = remember { AdmissionField.entries.associateWith { FocusRequester() } }
    LaunchedEffect(state.issues) {
        val field = state.issues.firstOrNull()?.field ?: return@LaunchedEffect
        val position = fieldPositions[field] ?: return@LaunchedEffect
        listState.scrollToItem(position)
        withFrameNanos { }
        if (field in textFields && !(editing && !allowDraft && field == AdmissionField.RUT)) requesters.getValue(field).requestFocus()
    }
    // La búsqueda se conserva sólo durante esta composición para evitar restaurar datos personales.
    var search by remember { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showConsentText by remember { mutableStateOf(false) }
    var showBirthPicker by remember { mutableStateOf(false) }
    BackHandler(enabled = actions.onBack != null) {
        if (!state.isSaving) {
            if (state.isSaved) actions.onBack?.invoke() else confirmDiscard = true
        }
    }
    val input = state.input

    if (showBirthPicker) {
        VolatileDatePickerDialog(input.birthDate, "Fecha de nacimiento",
            yearRange = 1900..LocalDate.now().year,
            onDismiss = { showBirthPicker = false }, onConfirm = {
                actions.text(AdmissionField.BIRTH_DATE, it); showBirthPicker = false
            })
    }
    if (showConsentText) AlertDialog(onDismissRequest = { showConsentText = false },
        title = { Text("Texto de consentimiento") },
        text = { androidx.compose.foundation.layout.Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(environment.context.consentText)
            Text("Leer este texto no registra aceptación. Confírmala en la sección de consentimiento antes de guardar.")
        } }, confirmButton = { TextButton({ showConsentText = false }) { Text("Cerrar consentimiento") } })

    if (state.pendingActivity != null) AlertDialog(onDismissRequest = { actions.discardActivity(false) },
        title = { Text("¿Cambiar actividad?") }, text = { Text("Se quitará el detalle escrito para Otra actividad.") },
        confirmButton = { TextButton(onClick = { actions.discardActivity(true) }) { Text("Cambiar y quitar detalle") } },
        dismissButton = { TextButton(onClick = { actions.discardActivity(false) }) { Text("Conservar") } })

    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text("¿Descartar formulario?") }, text = { Text("Se quitarán los datos que aún están sólo en memoria.") },
        confirmButton = { TextButton(enabled = !state.isSaving, onClick = { actions.discard(); search = ""; confirmDiscard = false; actions.onBack?.invoke() }) { Text("Descartar datos") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } })

    if (state.isQrScannerOpen) QrCameraDialog(state,
        onPayload = { actions.processQrPayload?.invoke(it) },
        onUnavailable = { actions.cameraUnavailable?.invoke() },
        onApply = { actions.applyQrCandidate?.invoke(it) },
        onRetry = { actions.dismissQrError?.invoke() },
        onClose = { actions.closeQrScanner?.invoke() })

    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        fieldPositions.clear()
        var index = 0
        fun LazyListScope.entry(field: AdmissionField? = null, content: @Composable LazyItemScope.() -> Unit) {
            if (field != null) fieldPositions[field] = index
            index++
            item(content = content)
        }
        entry {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (actions.onBack != null) IconButton({
                    if (state.isSaved) actions.onBack.invoke() else confirmDiscard = true
                }, enabled = !state.isSaving) {
                    DesignIcon(R.drawable.g6_capture_frame, description = "Volver al panel")
                }
                Text(if (editing) "Edición de inscripción" else "Nueva captura", style = MaterialTheme.typography.headlineMedium)
            }
            Text(if (editing) "Los cambios permanecen en memoria hasta guardar. El consentimiento original se conserva." else if (state.isSaved) "Registro confirmado en este dispositivo." else "Los datos permanecen sin guardar. Validar no guarda ni envía la inscripción.")
        }

        entry { OutlinedButton({ showConsentText = true }) { Text("Leer consentimiento") } }

        // Asistente de escaneo QR (RF11)
        if (!editing || allowDraft) entry {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesignIcon(R.drawable.g6_capture_frame1)
                        Text("Completa más rápido con QR", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Escanea un QR compatible y confirma el RUT. Completa los nombres manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    PrimaryActionButton(
                        onClick = { actions.openQrScanner?.invoke() },
                        enabled = !state.isSaving && !state.isSaved && !state.isAcquiringLocation && state.locationCandidate == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Escanear código QR asistido")
                    }
                }
            }
        }

        entry { Text("Datos personales y contacto", style = MaterialTheme.typography.titleLarge) }
        fieldPositions[AdmissionField.FAMILY_NAMES] = index
        entry(AdmissionField.GIVEN_NAMES) {
            AdaptiveFields(
                first = { FormText("Nombres", AdmissionField.GIVEN_NAMES, input.givenNames, state, actions, requester = requesters.getValue(AdmissionField.GIVEN_NAMES)) },
                second = { FormText("Apellidos", AdmissionField.FAMILY_NAMES, input.familyNames, state, actions, requester = requesters.getValue(AdmissionField.FAMILY_NAMES)) })
        }
        entry(AdmissionField.RUT) { FormText("RUT", AdmissionField.RUT, input.rut, state, actions, locked = editing && !allowDraft, requester = requesters.getValue(AdmissionField.RUT)) }
        fieldPositions[AdmissionField.PHONE] = index
        entry(AdmissionField.EMAIL) {
            AdaptiveFields(
                first = { FormText("Correo electrónico", AdmissionField.EMAIL, input.email, state, actions, KeyboardType.Email, requester = requesters.getValue(AdmissionField.EMAIL)) },
                second = { FormText("Teléfono", AdmissionField.PHONE, input.phone, state, actions, KeyboardType.Phone, requester = requesters.getValue(AdmissionField.PHONE)) })
        }
        entry(AdmissionField.BIRTH_DATE) { FormText("Nacimiento (aaaa-mm-dd)", AdmissionField.BIRTH_DATE, input.birthDate, state, actions, requester = requesters.getValue(AdmissionField.BIRTH_DATE),
            trailing = { IconButton({ showBirthPicker = true }, enabled = !state.isSaving && !state.isSaved) {
                DesignIcon(R.drawable.g6_capture_frame2, description = "Elegir fecha de nacimiento")
            } }) }
        entry(AdmissionField.RESIDENCE_COMMUNE) { FormText("Comuna de residencia", AdmissionField.RESIDENCE_COMMUNE, input.residenceCommune, state, actions, requester = requesters.getValue(AdmissionField.RESIDENCE_COMMUNE)) }
        entry(AdmissionField.ACTIVITY) { Choices("Actividad actual", CurrentActivity.entries, input.currentActivity, ::activityLabel, actions.activity) }
        if (input.currentActivity == CurrentActivity.OTHER) entry(AdmissionField.ACTIVITY_OTHER) { FormText("Detalle de otra actividad", AdmissionField.ACTIVITY_OTHER, input.currentActivityOther.orEmpty(), state, actions, requester = requesters.getValue(AdmissionField.ACTIVITY_OTHER)) }
        if (suggestion != null) entry {
            Text("Establecimiento indicado en el evento: ${suggestion.name}. Confirma si corresponde.")
            OutlinedButton(onClick = actions.suggestion) { Text("Usar establecimiento del evento") }
        }
        entry(AdmissionField.ESTABLISHMENT) { FormText("Establecimiento", AdmissionField.ESTABLISHMENT, input.establishment.orEmpty(), state, actions, requester = requesters.getValue(AdmissionField.ESTABLISHMENT)) }
        entry(AdmissionField.ESTABLISHMENT_COMMUNE) { FormText("Comuna del establecimiento (opcional)", AdmissionField.ESTABLISHMENT_COMMUNE, input.establishmentCommune.orEmpty(), state, actions, requester = requesters.getValue(AdmissionField.ESTABLISHMENT_COMMUNE)) }
        entry { Text("Preferencias de admisión", style = MaterialTheme.typography.titleLarge) }
        entry { OptionalChoices("Opción personal (opcional)", PersonalOption.entries, input.formPersonalOption,
            { when (it) { PersonalOption.FEMALE -> "Femenino"; PersonalOption.MALE -> "Masculino"; PersonalOption.NOT_STATED -> "No indica" } }, actions.personal) }
        entry { OptionalChoices("Jornada (opcional)", PreferredSchedule.entries, input.preferredSchedule,
            { if (it == PreferredSchedule.DAY) "Diurna" else "Vespertina" }, actions.schedule) }
        entry { OptionalChoices("Nivel de interés (opcional)", InterestLevel.entries, input.interestLevel,
            { when (it) { InterestLevel.HIGH -> "Alto"; InterestLevel.MEDIUM -> "Medio"; InterestLevel.LOW -> "Bajo" } }, actions.level) }
        entry { OptionalChoices("Motivación (opcional)", Motivation.entries, input.motivation, ::motivationLabel, actions.motivation) }
        entry(AdmissionField.INTERESTS) { Text("Carreras de interés · máximo 3, en orden de prioridad", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(search, { search = it }, label = { Text("Buscar carrera") }, trailingIcon = { DesignIcon(R.drawable.g6_capture_frame4) }, modifier = Modifier.fillMaxWidth()) }
        entry {
            input.interests.forEach { selected ->
                val program = environment.programs.firstOrNull { it.id == selected.programId }
                Text("${selected.priority}. ${program?.name ?: "Carrera no disponible"}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        entry(AdmissionField.SITES) { Text("Sedes de interés (opcional, independientes del evento)", style = MaterialTheme.typography.titleMedium)
            environment.sites.filter { it.enabled }.forEach { site -> FilterChip(site.id in input.preferredSiteIds,
                { actions.site(site.id) }, label = { Text(site.name) }, modifier = Modifier.fillMaxWidth()) } }
        entry { Text("Modalidad y canal de contacto se habilitarán cuando exista un catálogo definido.") }
        entry(AdmissionField.NOTES) { FormText("Notas (opcional, máximo 500; no amplían la motivación)", AdmissionField.NOTES, input.notes.orEmpty(), state, actions, requester = requesters.getValue(AdmissionField.NOTES)) }

        // Ubicación de captura
        entry(AdmissionField.LOCATION) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesignIcon(R.drawable.g6_capture_frame5)
                        Text("Ubicación de captura", style = MaterialTheme.typography.titleMedium)
                    }
                    val loc = state.location
                    when (loc) {
                        is CaptureLocation.Device -> {
                            Text(
                                "Ubicación GPS confirmada (${loc.coordinates.latitude}, ${loc.coordinates.longitude}) · Precisión: ${loc.accuracyMeters.toInt()}m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedButton(enabled = !state.isSaving && !state.isSaved, onClick = { actions.clearLocation?.invoke() }) {
                                Text(if (editing) "Restaurar ubicación original" else "Quitar ubicación")
                            }
                        }
                        is CaptureLocation.EventConfirmed -> {
                            Text(
                                "Ubicación confirmada según sede del evento (${loc.coordinates.latitude}, ${loc.coordinates.longitude})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedButton(enabled = !state.isSaving && !state.isSaved, onClick = { actions.clearLocation?.invoke() }) {
                                Text(if (editing) "Restaurar ubicación original" else "Quitar ubicación")
                            }
                        }
                        null -> {
                            Text(
                                "Sin ubicación asociada. Puedes guardar y completar el contexto después.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    state.locationCandidate?.let { candidate ->
                        Text("Ubicación candidata: precisión ${candidate.accuracyMeters.toInt()} m. Confirma para utilizarla.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryActionButton(onClick = { actions.confirmDeviceLocation?.invoke() }) { Text("Confirmar GPS candidato") }
                            OutlinedButton(onClick = { actions.dismissLocationCandidate?.invoke() }) { Text("Descartar candidata") }
                        }
                    }
                    if (state.isAcquiringLocation) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Adquiriendo señal GPS (máximo 10s)…", style = MaterialTheme.typography.bodySmall)
                    }

                    state.locationStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryActionButton(
                            onClick = { actions.requestLocation?.invoke() },
                            enabled = !state.isAcquiringLocation && !state.isSaving && !state.isSaved && state.locationCandidate == null,
                        ) {
                            Text("Adquirir GPS")
                        }
                        OutlinedButton(
                            onClick = { actions.confirmEventLocation?.invoke() },
                            enabled = !state.isAcquiringLocation && !state.isSaving && !state.isSaved && environment.context.event.location != null,
                        ) {
                            Text("Confirmar evento")
                        }
                    }
                }
            }
        }

        // Evidencia histórica inmutable durante edición.
        if (editing) entry {
            Text("Consentimiento original conservado", style = MaterialTheme.typography.titleMedium)
            Text("Versión ${state.consent?.version} · ${state.consent?.acceptedAt} · ${state.consent?.mode}")
        } else {
        // Consentimiento
        entry(AdmissionField.CONSENT) { Text("Consentimiento", style = MaterialTheme.typography.titleMedium)
            Text(environment.context.consentText)
            Choices("Forma de aceptación", AcceptanceMode.entries, state.mode,
                { if (it == AcceptanceMode.DIRECT) "Directa" else "Verbal registrada" }, actions.mode)
            Row(Modifier.fillMaxWidth().toggleable(value = state.consent != null, enabled = state.mode != null,
                role = Role.Checkbox, onValueChange = actions.consent)) {
                Checkbox(state.consent != null, null, enabled = state.mode != null); Text("Consentimiento aceptado")
            }
        }
        }
        entry {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Guardando en base de datos local…", style = MaterialTheme.typography.bodyMedium)
            }
            if (state.isSaved) {
                GlassCard(
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryActionButton(onClick = { actions.reset?.invoke() }) {
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
                    PrimaryActionButton(
                        onClick = { actions.save.invoke(SaveIntent.CONFIRM_REGISTRATION) },
                        enabled = saveEnabled && !state.isSaving && !state.isAcquiringLocation && state.locationCandidate == null && !state.isQrScannerOpen,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (editing) "Guardar cambios y confirmar inscripción" else "Guardar inscripción")
                    }
                    if (allowDraft) OutlinedButton(
                        onClick = { actions.save.invoke(SaveIntent.SAVE_DRAFT) },
                        enabled = saveEnabled && !state.isSaving && !state.isAcquiringLocation && state.locationCandidate == null && !state.isQrScannerOpen,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Guardar borrador")
                    }
                }
                PrimaryActionButton({ actions.validate(SaveIntent.CONFIRM_REGISTRATION) }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Validar inscripción") }
                if (allowDraft) OutlinedButton({ actions.validate(SaveIntent.SAVE_DRAFT) }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Validar borrador") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ confirmDiscard = true }, enabled = !state.isSaving) { Text(if (editing) "Descartar edición" else "Descartar formulario") }
                    if (actions.onBack != null) {
                        TextButton(enabled = !state.isSaving, onClick = { confirmDiscard = true }) { Text("Volver al panel") }
                    }
                }
            }
        }
    }
}
@Composable private fun FormText(label: String, field: AdmissionField, value: String, state: AdmissionFormState,
    actions: FormActions, keyboard: KeyboardType = KeyboardType.Text, locked: Boolean = false, requester: FocusRequester? = null, trailing: (@Composable () -> Unit)? = null) {
    val isPhone = field == AdmissionField.PHONE
    val displayedValue = if (isPhone) value.removePrefix("+56") else value
    val issue = state.issues.firstOrNull { it.field == field }
    OutlinedTextField(displayedValue, { edited ->
        val input = if (isPhone && edited.isNotBlank() && !edited.startsWith("+56")) "+56$edited" else edited
        actions.text(field, input)
    }, label = { Text(label) }, prefix = if (isPhone) ({ Text("+56") }) else null,
        modifier = Modifier.fillMaxWidth().then(if (requester != null) Modifier.focusRequester(requester) else Modifier),
        enabled = !state.isSaving && !state.isSaved && !locked,
        supportingText = issue?.let { { Text(issueLabel(it.code)) } },
        trailingIcon = trailing ?: issue?.let { { Text("!", color = MaterialTheme.colorScheme.error) } },
        isError = state.issues.any { it.field == field }, keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = if (keyboard == KeyboardType.Email || keyboard == KeyboardType.Phone) false else null),
        singleLine = field != AdmissionField.NOTES)
}
@Composable private fun <T> Choices(title: String, options: List<T>, selected: T?, label: (T) -> String, action: (T) -> Unit) {
    Column { Text(title); options.forEach { option -> FilterChip(option == selected, { action(option) }, label = { Text(label(option)) }, modifier = Modifier.fillMaxWidth()) } }
}
@Composable private fun <T> OptionalChoices(title: String, options: List<T>, selected: T?, label: (T) -> String, action: (T?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton({ expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let(label) ?: "Sin respuesta", Modifier.weight(1f))
                DesignIcon(R.drawable.g6_capture_frame3)
            }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(text = { Text("Dejar sin respuesta") }, onClick = { action(null); expanded = false })
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(label(option)) }, onClick = { action(option); expanded = false })
                }
            }
        }
    }
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

private val textFields = setOf(AdmissionField.GIVEN_NAMES, AdmissionField.FAMILY_NAMES, AdmissionField.RUT,
    AdmissionField.EMAIL, AdmissionField.PHONE, AdmissionField.BIRTH_DATE, AdmissionField.RESIDENCE_COMMUNE,
    AdmissionField.ACTIVITY_OTHER, AdmissionField.ESTABLISHMENT, AdmissionField.ESTABLISHMENT_COMMUNE, AdmissionField.NOTES)

@Composable private fun AdaptiveFields(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { first(); second() }
    }
}
