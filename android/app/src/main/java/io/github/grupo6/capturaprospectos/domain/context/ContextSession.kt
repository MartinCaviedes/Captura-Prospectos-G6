package io.github.grupo6.capturaprospectos.domain.context

/** Recurso local: el dueño cancela y espera a sus consumidores antes de cerrar. */
interface ContextSession : AutoCloseable { val repository: DemoContextRepository }
class ContextOpenFailure : Exception("Almacenamiento local no disponible")
