# Ronda 2 · FORM-01-B y Hub de conectividad

Incremento local sobre la base de ronda 1 (`525f109`), desarrollado en la rama `feature/e1-ronda2-form-hub`. Conecta el contexto activo con la persistencia en Room cifrado y presenta el estado de conectividad en la interfaz de usuario, avanzando el alcance exigido para la entrega local E1 (P2/P3).

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **FORM-01-B** | Conexión de `AdmissionFormViewModel` con `AdmissionRepository.create`. Guarda capturas completas consentidas (`READY`), borradores explícitos (`DRAFT`) y maneja duplicados propios y restringidos en Room cifrado. Retroalimentación accesible y botón de nueva captura / retorno al panel. |
| **HUB-01-A** | `MainHubScreen` y `MainHubRoute`. Pantalla principal tras seleccionar perfil y evento. Muestra resumen del contexto activo, acciones operativas y banner de red nativa. |
| **NET-01-B** | Presentación visual reactiva del estado de red (`AndroidConnectivityObserver`) en el Hub: `VALIDATED`, `CONNECTED_UNVALIDATED`, `OFFLINE`, `UNKNOWN`. Rótulo explícito indicando que el envío remoto no está habilitado en E1. |
| **NAV-01-A** | Navegación declarativa en `MainActivity` coordinada por estado: Selector de contexto → Hub principal → Formulario de admisión. Descarte y limpieza segura de datos en memoria al salir del formulario sin guardar. |

## Reglas y contratos preservados

1. **Privacidad y memoria volátil:** Los datos del prospecto se mantienen en memoria durante la edición. Descartar el formulario o salir al panel limpia completamente el estado sin dejar rastros en `SavedState` ni logs. El guardado en Room exige consentimiento previo.
2. **Cero transmisiones remotas en E1:** Al guardar con éxito se informa explícitamente: *«Inscripción guardada localmente (READY/DRAFT). Envío remoto no habilitado en esta entrega»*. La observación de conectividad es informativa y no dispara sincronizaciones ni simula ACK de backend.
3. **Control de duplicados sin fuga de datos:** `CreateAdmissionResult.DuplicateOwn` notifica la coincidencia propia; `CreateAdmissionResult.DuplicateRestricted` informa duplicado en el evento sin revelar identificadores ni datos de otros captadores.
4. **Tolerancia a fallos:** Los fallos de almacenamiento (`StorageUnavailable`) se comunican sin reiniciar la base de datos ni provocar crashes, preservando los datos digitados para reintentar.

## Corrección y verificación — 10/10/2026

La revisión encontró un argumento inexistente en CaptureEnvironment y un formulario creado con remember sin ViewModelStore. Se corrige el consumidor (`offerings`); ContextSessionOwner retiene el formulario por perfil/evento y libera su estado al salir. Primero cancela y espera sus consumidores, luego cierra Room. Sólo se retienen respuestas en memoria; una muerte de proceso pierde el formulario no guardado.

Atrás del sistema y el botón de salida piden confirmación para descartar. Durante el guardado no se permiten edición, cambio de consentimiento, descarte, reset ni segundo envío: se espera el resultado transaccional y se conserva la entrada si falla. Tras guardar se requiere «Nueva inscripción» para otra alta. Sin consentimiento no se llama al repositorio desde el formulario; el repositorio mantiene la validación autoritativa. Mensajes por campo y filas adaptables mejoran el feedback; no acreditan accesibilidad completa.

Controles nuevos: **109 JVM sin fallos/errores/omitidas**, build principal/instrumental correcto y **Lint 20 avisos/0 errores**, sin actualizar dependencias. En Xiaomi 11T Android 14/API 34: **27 pruebas de persistencia cifrada** (29.645 s) y, por separado, **4 de formulario/Atrás** (40.641 s). Las cuatro UI necesitaron apertura asistida por ADB en MIUI, sin conceder permisos ni omitir aserciones. La duración incluye espera y no mide rendimiento de la app. Los SHA256 de ambas APK instaladas coinciden con el build registrado.

[Evidencia por fuentes/APK y suites](evidencias/form-correction-2026-10-10.json), [salida UI](evidencias/android-form-correction-2026-10-10.txt). Los tests de retención/liberación y cancelación antes de cerrar la conexión usan ViewModelStore en JVM; todavía queda comprobar giro real del recorrido de MainActivity, TalkBack/visual completa, cámara/GPS y revisión humana. La UI usa un doble demorado; la persistencia real se prueba en otro grupo, sin afirmar un recorrido conjunto nuevo ni cierre de E1.

## Pruebas originales del incremento (antecedente)

- `AdmissionFormSaveTest`: 9 pruebas unitarias en JVM que verifican:
  - Guardado sin repositorio informa error sin crash.
  - Guardado válido consentido confirma estado `READY` y mensaje contractual.
  - Guardado de borrador consentido confirma estado `DRAFT`.
  - Manejo de duplicado propio y duplicado restringido (sin PII).
  - Fallo de almacenamiento preserva datos en memoria.
  - El caso original recibía ConsentRequired del doble; la corrección posterior comprueba que sin consentimiento no se llama al repositorio.
  - Reseteo completo tras guardar para capturar nuevo prospecto.
  - La corrección posterior requiere reset explícito antes de otra captura y bloquea cambios durante guardado.
