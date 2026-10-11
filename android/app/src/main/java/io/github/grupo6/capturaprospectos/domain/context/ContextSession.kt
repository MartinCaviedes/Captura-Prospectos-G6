package io.github.grupo6.capturaprospectos.domain.context

import io.github.grupo6.capturaprospectos.domain.admission.AdmissionRepository

/** Recurso local: el dueño cancela y espera a sus consumidores antes de cerrar. */
interface ContextSession : AutoCloseable {
    val repository: DemoContextRepository
    val admissionRepository: AdmissionRepository? get() = null
}
class ContextOpenFailure : Exception("Almacenamiento local no disponible")
