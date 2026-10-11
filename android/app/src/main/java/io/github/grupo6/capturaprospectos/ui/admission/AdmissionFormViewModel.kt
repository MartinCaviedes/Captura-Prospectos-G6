package io.github.grupo6.capturaprospectos.ui.admission

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.grupo6.capturaprospectos.domain.admission.*
import io.github.grupo6.capturaprospectos.domain.capture.*
import io.github.grupo6.capturaprospectos.domain.context.EstablishmentSuggestion
import io.github.grupo6.capturaprospectos.domain.device.*
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class AdmissionFormState(
    val input: AdmissionInput = AdmissionInput(),
    val mode: AcceptanceMode? = null,
    val consent: ConsentEvidence? = null,
    val pendingActivity: CurrentActivity? = null,
    val issues: List<AdmissionIssue> = emptyList(),
    val message: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val savedId: UUID? = null,
    val savedState: CaptureState? = null,
    val location: CaptureLocation? = null,
    val locationCandidate: CaptureLocation.Device? = null,
    val isAcquiringLocation: Boolean = false,
    val locationStatus: String? = null,
    val qrCandidate: QrCandidate? = null,
    val qrError: String? = null,
    val isQrScannerOpen: Boolean = false,
) { override fun toString() = "AdmissionFormState(redacted)" }

/** Mantiene el borrador de admisión en memoria y expone su validación y persistencia a la UI.
 * El borrador se descarta con el ViewModel; la persistencia corresponde al repositorio de admisiones. */
class AdmissionFormViewModel(
    val environment: AdmissionEnvironment,
    val suggestion: EstablishmentSuggestion? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val prepare: PrepareAdmission = PrepareAdmission(),
    private val repository: AdmissionRepository? = null,
    private val locationProvider: LocationProvider? = null,
    private val qrParser: QrParser = DemoQrParser(),
) : ViewModel() {
    private val state = MutableStateFlow(AdmissionFormState())
    val uiState = state.asStateFlow()
    private var active = true
    private val canEdit get() = active && !state.value.isSaving && !state.value.isSaved
    private var locationJob: Job? = null
    private var locationGeneration = 0L
    private fun change(transform: (AdmissionInput) -> AdmissionInput) {
        if (!canEdit) return
        state.value = state.value.copy(
            input = transform(state.value.input),
            issues = emptyList(),
            message = null,
            isSaved = false,
            savedId = null,
            savedState = null,
        )
    }

    @MainThread fun text(field: AdmissionField, value: String) = change { input -> when (field) {
        AdmissionField.GIVEN_NAMES -> input.copy(givenNames = value)
        AdmissionField.FAMILY_NAMES -> input.copy(familyNames = value)
        AdmissionField.RUT -> input.copy(rut = value)
        AdmissionField.EMAIL -> input.copy(email = value)
        AdmissionField.PHONE -> input.copy(phone = value)
        AdmissionField.BIRTH_DATE -> input.copy(birthDate = value)
        AdmissionField.RESIDENCE_COMMUNE -> input.copy(residenceCommune = value)
        AdmissionField.ACTIVITY_OTHER -> if (input.currentActivity == CurrentActivity.OTHER) input.copy(currentActivityOther = value) else input
        AdmissionField.ESTABLISHMENT -> input.copy(establishment = value)
        AdmissionField.ESTABLISHMENT_COMMUNE -> input.copy(establishmentCommune = value)
        AdmissionField.NOTES -> input.copy(notes = value)
        else -> input // Sin catálogo de modalidad/canal: no inventar opciones.
    } }

    @MainThread fun activity(value: CurrentActivity) {
        if (!canEdit) return
        val input = state.value.input
        if (input.currentActivity == CurrentActivity.OTHER && value != CurrentActivity.OTHER && !input.currentActivityOther.isNullOrBlank()) {
            state.value = state.value.copy(pendingActivity = value); return
        }
        change { it.copy(currentActivity = value, currentActivityOther = if (value == CurrentActivity.OTHER) it.currentActivityOther else null) }
    }

    @MainThread fun confirmActivityDiscard(confirmed: Boolean) {
        if (!canEdit) return
        val pending = state.value.pendingActivity ?: return
        if (confirmed) change { it.copy(currentActivity = pending, currentActivityOther = null) }
        state.value = state.value.copy(pendingActivity = null)
    }

    @MainThread fun acceptEstablishmentSuggestion() {
        val value = suggestion ?: return
        change { it.copy(establishment = value.name, establishmentCommune = value.commune) }
    }

    @MainThread fun personal(value: PersonalOption?) = change { it.copy(formPersonalOption = value) }
    @MainThread fun schedule(value: PreferredSchedule?) = change { it.copy(preferredSchedule = value) }
    @MainThread fun interestLevel(value: InterestLevel?) = change { it.copy(interestLevel = value) }
    @MainThread fun motivation(value: Motivation?) = change { it.copy(motivation = value) }

    @MainThread fun program(id: UUID) {
        if (environment.programs.none { it.id == id && it.enabled }) return
        change { input ->
            val current = input.interests
            val next = if (current.any { it.programId == id }) current.filterNot { it.programId == id }
                else if (current.size < 3) current + RankedInterest(id, current.size + 1) else current
            input.copy(interests = next.mapIndexed { index, item -> item.copy(priority = index + 1) })
        }
    }

    @MainThread fun moveProgram(id: UUID, direction: Int) {
        if (direction != -1 && direction != 1) return
        change { input ->
            val items = input.interests.toMutableList(); val from = items.indexOfFirst { it.programId == id }
            val to = from + direction
            if (from >= 0 && to in items.indices) { val item = items.removeAt(from); items.add(to, item) }
            input.copy(interests = items.mapIndexed { index, item -> item.copy(priority = index + 1) })
        }
    }

    @MainThread fun mention(programId: UUID, mentionId: UUID?) {
        val option = environment.programs.singleOrNull { it.id == programId } ?: return
        if (mentionId != null && mentionId !in option.mentions) return
        change { input -> input.copy(interests = input.interests.map { if (it.programId == programId) it.copy(mentionId = mentionId) else it }) }
    }

    @MainThread fun site(id: UUID) {
        if (environment.sites.none { it.id == id && it.enabled }) return
        change { input -> input.copy(preferredSiteIds = if (id in input.preferredSiteIds) input.preferredSiteIds - id else input.preferredSiteIds + id) }
    }

    @MainThread fun mode(value: AcceptanceMode) {
        if (!canEdit) return
        state.value = state.value.copy(mode = value, consent = null, issues = emptyList(), message = null)
    }

    @MainThread fun consent(accepted: Boolean) {
        if (!canEdit) return
        val current = state.value
        val evidence = if (accepted && current.mode != null) ConsentEvidence(environment.context.consentVersion,
            clock.instant(), environment.context.actor.id, current.mode) else null
        state.value = current.copy(consent = evidence, issues = emptyList(), message = null)
    }

    // --- Ubicación GPS y contextual (RF04) ---

    @MainThread
    fun requestLocation(customProvider: LocationProvider? = null): Job? {
        if (!canEdit || state.value.isQrScannerOpen) return null
        val current = state.value
        if (current.isAcquiringLocation) return null
        val provider = customProvider ?: locationProvider ?: run {
            state.value = current.copy(locationStatus = "Proveedor de ubicación no disponible.")
            return null
        }
        val generation = ++locationGeneration
        state.value = current.copy(isAcquiringLocation = true, locationCandidate = null, locationStatus = "Adquiriendo ubicación…")
        return viewModelScope.launch {
            val result = provider.request(LocationRequestDefaults.timeout)
            currentCoroutineContext().ensureActive()
            if (generation != locationGeneration || !canEdit) return@launch
            val latest = state.value
            val context = environment.context
            val now = clock.instant()
            state.value = when (result) {
                is LocationResult.Candidate -> {
                    val policy = LocationPolicy()
                    val isValid = policy.isValid(result.position, now, context.actor.id, context.event.location)
                    if (isValid) {
                        latest.copy(
                            isAcquiringLocation = false,
                            locationCandidate = result.position,
                            locationStatus = "Ubicación candidata (precisión ${result.position.accuracyMeters.toInt()} m). Confirma antes de usarla.",
                        )
                    } else {
                        latest.copy(
                            isAcquiringLocation = false,
                            locationStatus = "Ubicación GPS fuera de los umbrales de política local (5 min / 100 m).",
                        )
                    }
                }
                LocationResult.PermissionDenied -> latest.copy(
                    isAcquiringLocation = false,
                    locationStatus = "Permiso de ubicación denegado en el dispositivo.",
                )
                LocationResult.ServicesDisabled -> latest.copy(
                    isAcquiringLocation = false,
                    locationStatus = "Servicios de ubicación (GPS) desactivados.",
                )
                LocationResult.TimedOut -> latest.copy(
                    isAcquiringLocation = false,
                    locationStatus = "Tiempo de espera agotado al adquirir ubicación (10s).",
                )
                LocationResult.Unavailable -> latest.copy(
                    isAcquiringLocation = false,
                    locationStatus = "Ubicación GPS no disponible en este momento.",
                )
            }
        }.also { locationJob = it }
    }

    @MainThread fun confirmDeviceLocation() {
        if (!canEdit) return
        val candidate = state.value.locationCandidate ?: return
        if (!LocationPolicy().isValid(candidate, clock.instant(), environment.context.actor.id, environment.context.event.location)) {
            state.value = state.value.copy(locationCandidate = null, locationStatus = "La ubicación candidata ya no es válida; adquiere otra.")
            return
        }
        state.value = state.value.copy(location = candidate, locationCandidate = null, locationStatus = "Ubicación GPS confirmada por el usuario.")
    }
    @MainThread fun dismissLocationCandidate() {
        if (!canEdit) return
        cancelLocation()
        state.value = state.value.copy(locationCandidate = null, isAcquiringLocation = false, locationStatus = null)
    }

    @MainThread
    fun confirmEventLocation() {
        if (!canEdit || state.value.isAcquiringLocation) return
        val eventLoc = environment.context.event.location
        if (eventLoc != null) {
            val confirmed = CaptureLocation.EventConfirmed(
                coordinates = eventLoc,
                confirmedAt = clock.instant(),
                confirmedBy = environment.context.actor.id,
            )
            state.value = state.value.copy(
                location = confirmed, locationCandidate = null,
                locationStatus = "Ubicación confirmada según sede del evento.",
            )
        } else {
            state.value = state.value.copy(
                locationStatus = "El evento no tiene coordenadas declaradas.",
            )
        }
    }

    @MainThread
    fun clearLocation() {
        if (!canEdit) return
        cancelLocation()
        state.value = state.value.copy(location = null, locationCandidate = null, isAcquiringLocation = false, locationStatus = null)
    }

    // --- Asistencia por Cámara / QR sintético (RF11) ---

    @MainThread
    fun openQrScanner() {
        if (!canEdit || state.value.isAcquiringLocation) return
        state.value = state.value.copy(isQrScannerOpen = true, qrCandidate = null, qrError = null)
    }

    @MainThread
    fun closeQrScanner() {
        if (!canEdit) return
        state.value = state.value.copy(isQrScannerOpen = false, qrCandidate = null, qrError = null)
    }

    @MainThread
    fun processQrPayload(rawPayload: String) {
        if (!canEdit || !state.value.isQrScannerOpen) return
        when (val result = qrParser.parse(rawPayload)) {
            is QrResult.Candidate -> {
                state.value = state.value.copy(qrCandidate = result.value, qrError = null)
            }
            QrResult.Unsupported -> {
                state.value = state.value.copy(
                    qrCandidate = null,
                    qrError = "Formato de código no compatible. Debe ser formato sintético de demo (G6DEMO:1).",
                )
            }
            QrResult.Invalid -> {
                state.value = state.value.copy(
                    qrCandidate = null,
                    qrError = "Código inválido o datos mal formados.",
                )
            }
        }
    }

    @MainThread
    fun applyQrCandidate(replaceExistingRut: Boolean = false) {
        if (!canEdit || !state.value.isQrScannerOpen) return
        val candidate = state.value.qrCandidate ?: return
        val previous = state.value.input.rut
        if (previous.isNotBlank() && RutValidator.normalize(previous) != candidate.rut && !replaceExistingRut) {
            state.value = state.value.copy(qrError = "Confirma explícitamente el reemplazo del RUT escrito o conserva la entrada manual.")
            return
        }
        change { input ->
            input.copy(
                rut = candidate.rut,
            )
        }
        state.value = state.value.copy(isQrScannerOpen = false, qrCandidate = null, qrError = null)
    }

    @MainThread
    fun dismissQrError() {
        if (!canEdit) return
        state.value = state.value.copy(qrError = null)
    }

    @MainThread fun cameraUnavailable() {
        if (canEdit && state.value.isQrScannerOpen) state.value = state.value.copy(qrError = "Cámara no disponible. Puedes continuar con ingreso manual.")
    }

    // --- Validación y persistencia (FORM-01-B) ---

    @MainThread fun validate(intent: SaveIntent) {
        if (!canEdit) return
        val current = state.value; val context = environment.context
        val result = prepare.prepare(current.input, intent, current.consent, context.consentVersion,
            context.actor.id, environment.programs, environment.sites, current.location, context.event.location,
            clock.instant(), context.event.timeZone)
        state.value = when (result) {
            AdmissionPreparation.ConsentRequired -> current.copy(issues = listOf(AdmissionIssue(AdmissionField.CONSENT, ValidationCode.REQUIRED)), message = "Registra el consentimiento para validar.")
            is AdmissionPreparation.Invalid -> current.copy(issues = result.issues, message = "Revisa los campos indicados.")
            is AdmissionPreparation.Prepared -> current.copy(issues = emptyList(), message = "Validación local correcta. No se ha guardado ni enviado la inscripción.")
        }
    }

    @MainThread
    fun save(intent: SaveIntent): Job? {
        val current = state.value
        if (!canEdit) return null
        if (current.isAcquiringLocation || current.isQrScannerOpen || current.locationCandidate != null) return null
        if (current.consent == null) {
            state.value = current.copy(
                issues = listOf(AdmissionIssue(AdmissionField.CONSENT, ValidationCode.REQUIRED)),
                message = "Registra el consentimiento para guardar.",
            )
            return null
        }
        val repo = repository ?: run {
            state.value = current.copy(message = "Repositorio local no disponible para guardar.")
            return null
        }
        state.value = current.copy(isSaving = true, message = null)
        return viewModelScope.launch {
            val request = NewAdmission(
                input = current.input,
                intent = intent,
                consent = current.consent,
                eventId = environment.context.event.id,
                location = current.location,
            )
            val result = repo.create(request)
            val latest = state.value
            state.value = when (result) {
                is CreateAdmissionResult.Saved -> latest.copy(
                    isSaving = false,
                    isSaved = true,
                    savedId = result.id,
                    savedState = result.state,
                    message = "Inscripción guardada localmente (${result.state.name}). Envío remoto no habilitado en esta entrega."
                )
                is CreateAdmissionResult.DuplicateOwn -> latest.copy(
                    isSaving = false,
                    message = "Ya existe una inscripción registrada para este RUT en este evento."
                )
                CreateAdmissionResult.DuplicateRestricted -> latest.copy(
                    isSaving = false,
                    message = "Este RUT ya se encuentra registrado en este evento."
                )
                CreateAdmissionResult.ConsentRequired -> latest.copy(
                    isSaving = false,
                    issues = listOf(AdmissionIssue(AdmissionField.CONSENT, ValidationCode.REQUIRED)),
                    message = "Registra el consentimiento para guardar."
                )
                is CreateAdmissionResult.Invalid -> latest.copy(
                    isSaving = false,
                    issues = result.issues,
                    message = "Revisa los campos indicados para guardar."
                )
                CreateAdmissionResult.AccessDenied -> latest.copy(
                    isSaving = false,
                    message = "Acceso no autorizado para este perfil o evento."
                )
                CreateAdmissionResult.StorageUnavailable -> latest.copy(
                    isSaving = false,
                    message = "Almacenamiento local no disponible. Los datos se conservan; puedes reintentar."
                )
            }
        }
    }
    private fun cancelLocation() { locationGeneration++; locationJob?.cancel(); locationJob = null }
    @MainThread fun reset() { if (!state.value.isSaving) { cancelLocation(); state.value = AdmissionFormState() } }
    @MainThread fun discard() { if (!state.value.isSaving) { cancelLocation(); state.value = AdmissionFormState() } }
    override fun onCleared() { active = false; cancelLocation(); state.value = AdmissionFormState() }
}
