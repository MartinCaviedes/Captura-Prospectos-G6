package io.github.grupo6.capturaprospectos.testing

/** Backend aislado o cambios de red orquestados: ejecutar explícitamente con ese entorno. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class RequiresExternalFixture
