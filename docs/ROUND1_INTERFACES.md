# Interfaces por tarea · ronda 1

09/10/2026. Incremento local sobre ronda 0 (`ec9b93b`), sin publicación ni integración en develop. La revisión humana de contratos sigue pendiente. No cierra RF completos ni adelanta backend.

| Entregable | Alcance preparado | Siguiente consumidor |
|---|---|---|
| RECORDS-01-A | Propuesta Kotlin y [contrato de consulta/edición/anulación](RECORD_CONTRACTS.md), revisión, errores y fixtures | Revisar una vez; repositorio y bandeja desarrollan por separado contra esa firma |
| FORM-01-A | AdmissionFormViewModel, Route y componentes volátiles contra AdmissionInput/PrepareAdmission | FORM-01-B conecta contexto, navegación y alta con AdmissionRepository |
| DEVICE-01-A | DemoQrParser puro, QrParser/QrResult y LocationProvider/LocationResult | DEVICE-01-B implementa cámara, permisos y GPS Android con confirmación |

## Formulario

Sin repositorio, SavedStateHandle, rememberSaveable ni almacenamiento de respuestas. El ViewModel mantiene estado durante recreación mientras conserve su dueño; descartar o liberar el dueño vacía el estado. Una muerte de proceso pierde el formulario no guardado deliberadamente. Ronda 2 debe asignar un dueño por contexto/formulario y confirmar descarte antes de cambiar contexto; no retener respuestas bajo un perfil/evento diferente.

Campos F2027, actividad con confirmación antes de quitar detalle de Otra, establecimiento sugerido sólo por acción explícita y editable, respuestas opcionales sin valor predeterminado, tres carreras máximas ordenadas con mención opcional, búsqueda sin acentos/mayúsculas y sedes múltiples independientes. Motivación conserva las cuatro alternativas del cupón; sin texto libre. Modalidad y canal no ofrecen opciones inventadas mientras falte catálogo.

Consentimiento sin marcar, modalidad DIRECT/VERBAL elegida explícitamente, evidencia con versión/actor/instante. Cambiar modalidad elimina la evidencia anterior. Validar delega a PrepareAdmission: devuelve errores legibles o confirmación de validación **sin guardado**. Preparar no autoriza contexto ni acredita escritura. No hay GPS conectado en esta ronda: la validación puede preparar NEEDS_CONTEXT conforme al contrato. R2 usará la ubicación explícitamente aceptada; jamás inferirá residencia/establecimiento desde coordenadas.

Los componentes aún están fuera de MainActivity. No presentar este avance como recorrido de alta operativo. Revisión visual en claro/oscuro, letra ampliada, teclado y TalkBack sigue pendiente aunque compile y existan pruebas Compose.

## Dispositivos

DemoQrParser acepta sólo `G6DEMO:1:{"fullName":"Persona Ficticia Alfa","rut":"12345678-5"}`: máximo 512 caracteres, objeto JSON de exactamente dos cadenas, nombre 2–120 y RUT con DV válido. Rechaza campos extra/duplicados, tipos diferentes, escapes inválidos, Unicode mal formado, JSON truncado, URLs y otras versiones. Devuelve candidato sin efectos ni diagnóstico con PII. El lector JSON es un subconjunto acotado para este contrato, no un parser JSON general ni de cédulas institucionales.

No separar fullName por espacios para inventar nombres/apellidos F2027. Mostrar el candidato, pedir confirmación del usuario para los campos compatibles y completar manualmente lo faltante. No almacenar payload crudo ni añadir nacimiento a G6DEMO:1. La investigación del nacimiento por cédula continúa condicionada a un formato verificable.

LocationProvider.request(timeout) produce Candidate(Device), PermissionDenied, ServicesDisabled, TimedOut o Unavailable; implementación Android pendiente. Debe exigir timeout positivo y detener la adquisición al cancelar. Valor PT de trabajo 10 segundos. El consumidor aplica LocationPolicy (5 min/100 m) y confirma; la confirmación de coordenadas del evento es otra acción, nunca fallback implícito. No guardar posiciones de prueba personales.

## Verificación

JVM: pruebas de parser estricto, límites de consultas, estado F2027, prioridades/menciones, consentimiento, descarte y delegación de validaciones. El contrato de registros no tiene implementación: fixtures no son pruebas de transacciones. Evidencia numérica final y ejecuciones Android en [ANDROID_BASELINE](ANDROID_BASELINE.md).

Prueba especial de red `ConnectivityTransitionTest`: se activa sólo con argumento instrumental `-e g6WifiTransitions true` y orquestación autorizada para apagar/restaurar Wi-Fi según `network_phase`. Sin ese argumento se omite mediante Assume para no colgar suites normales. Requiere Wi-Fi validado y ausencia de conectividad alternativa; restaurar el estado inicial en finally del controlador. No confundir esa omisión deliberada con cobertura de pérdida/recuperación.
