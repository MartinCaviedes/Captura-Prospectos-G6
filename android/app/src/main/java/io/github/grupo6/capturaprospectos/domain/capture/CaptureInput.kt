package io.github.grupo6.capturaprospectos.domain.capture

import java.time.Instant
import java.util.UUID

enum class CaptureState { DRAFT, NEEDS_CONTEXT, READY, CANCELLED }
enum class TransportState { NOT_QUEUED }
enum class AcceptanceMode { DIRECT, VERBAL }
enum class SaveIntent { SAVE_DRAFT, CONFIRM_REGISTRATION }

/** Entrada de memoria: no debe enviarse a SavedState, logs ni almacenamiento. */
data class CaptureInput(
    val fullName: String = "",
    val rut: String = "",
    val email: String = "",
    val phone: String = "",
    val educationLevel: String = "SECONDARY",
    val establishment: String = "",
    val contactChannel: String? = null,
    val notes: String? = null,
) {
    override fun toString() = "CaptureInput(redacted)"
}

data class ConsentEvidence(
    val version: String,
    val acceptedAt: Instant,
    val recordedBy: UUID,
    val mode: AcceptanceMode,
)

/** Snapshot local de una oferta; no implica catálogo institucional verificado. */
data class Offering(
    val id: UUID,
    val programId: UUID,
    val siteId: UUID,
    val catalogVersion: String,
    val programName: String,
    val enabled: Boolean,
    val modality: String? = null,
)

enum class CaptureField {
    NAME, RUT, EMAIL, PHONE, EDUCATION, ESTABLISHMENT, CHANNEL, NOTES,
    INTERESTS, CONSENT, LOCATION,
}
enum class ValidationCode { REQUIRED, LENGTH, FORMAT, UNKNOWN_OPTION, DUPLICATE, UNAVAILABLE, INVALID_EVIDENCE }
data class FieldIssue(val field: CaptureField, val code: ValidationCode)

sealed interface PreparationResult {
    data object ConsentRequired : PreparationResult
    data class Invalid(val issues: List<FieldIssue>) : PreparationResult
    /** Preparado no significa guardado: el repositorio debe autorizar y hacer commit. */
    class Prepared internal constructor(
        val input: CaptureInput,
        val consent: ConsentEvidence,
        interests: List<Offering>,
        val location: CaptureLocation?,
        val state: CaptureState,
    ) : PreparationResult {
        private val selectedInterests = interests.toList()
        val interests: List<Offering> get() = selectedInterests.toList()
        val transportState: TransportState = TransportState.NOT_QUEUED
        override fun toString() = "Prepared(state=$state, personalData=redacted)"
    }
}
