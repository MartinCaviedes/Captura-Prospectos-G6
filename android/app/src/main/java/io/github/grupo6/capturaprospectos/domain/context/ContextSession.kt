package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.admission.AdmissionRepository
import io.github.grupo6.capturaprospectos.domain.device.LocationProvider
import io.github.grupo6.capturaprospectos.domain.records.RecordRepository

/** Recurso local: el dueño cancela y espera a sus consumidores antes de cerrar. */
interface ContextSession : AutoCloseable {
    val repository: DemoContextRepository
    val admissionRepository: AdmissionRepository? get() = null
    val recordRepository: RecordRepository? get() = null
    val locationProvider: LocationProvider? get() = null
}
class ContextOpenFailure : Exception("Almacenamiento local no disponible")
