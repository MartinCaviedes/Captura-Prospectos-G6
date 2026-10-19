# Alcance y aceptación por entrega

Decisión de alcance del equipo · 19/10/2026: Martín aplaza el soporte de QR de cédula real a E2; no se incorporan dependencias ni un parser de documentos reales en E1. Se mantiene cámara real con formato sintético, confirmación y entrada manual. La distribución preliminar por GitHub no supone aceptación contractual ni cierre E1. [Guía de distribución](DISTRIBUCION_DEMO.md).

Actualización 11/10/2026: candidato local con las correcciones de las rondas preparadas y verificaciones identificadas en ANDROID_BASELINE. Se retiró la afirmación anterior de alcance E1 completamente verificado: SQLite del host no acreditaba rendimiento Android. Hay ahora un ensayo real de 400 en Room cifrado, sin que ello cierre E1/RF ni sustituya accesibilidad, permisos adversos, revisión cruzada y aceptación de contratos. Sin backend/WorkManager/ACK; transporte NOT_QUEUED. El alcance y criterios siguientes se conservan.

02/10/2026. Distribución de trabajo adoptada por instrucción de Martín: mantener el nivel del producto y progresarlo en dos entregas. No equivale a aprobación adicional del docente/cliente. El SRS define el comportamiento final; este documento determina cuándo se implementa y qué evidencia permite cerrar cada incremento. Los RF no se eliminan ni renumeran.

## Regla de planificación

- E1 = P2 código + P3 defensa, objetivo reportado 19/10/2026. App local funcional, con calidad, seguridad local y tres recursos: GPS, cámara y detección de conectividad.
- E2 = P4 código + P5 defensa, fecha calendario pendiente. Extiende E1 con autenticación remota, microservicios, bases operativas, sincronización, consolidación, API externa, cobertura y entrega firmada.
- Los criterios del SRS que aún dependen de E2 permanecen pendientes al entregar E1. Cerrar una tarea E1 no cierra automáticamente su RF completo.
- El nivel de calidad no baja entre hitos: validación, privacidad, no pérdida de datos, revisión humana y evidencia real se aplican desde el comienzo.

Decisión vigente del equipo: mantener GPS, cámara, conectividad y cifrado local en E1, sin anticipar backend. La distribución conserva los criterios finales del SRS y requiere confirmar con docente la frontera local/remota.

## Matriz de los requisitos del producto

| RF | E1: incremento local exigido por el equipo | E2: completar comportamiento del SRS |
|---|---|---|
| RF01 | Entrada explícita a demo local mediante perfil sintético, selección de evento y filtrado de datos por perfil; no se presenta como login seguro | Login real, roles/scopes en servidor, refresh, expiración offline, logout y permisos revocados sin pérdida de pendientes |
| RF02 | Formulario completo, reglas centralizadas, errores por campo y conservación ante errores recuperables | Validación equivalente y errores por campo desde servidor |
| RF03 | Catálogo local trazable, búsqueda, multiselección, resumen y referencias históricas | Consistencia con catálogo actualizado desde API |
| RF04 | GPS real, permisos y fallos, fecha/fuente/precisión; alternativa de contexto rotulada y estado NEEDS_CONTEXT | Contexto validado/persistido también en backend |
| RF05 | Consentimiento versionado antes de persistencia; cancelar limpia memoria; texto ficticio | Evidencia de consentimiento viaja y se valida en servidor; piloto real sigue fuera de alcance |
| RF06 | Room cifrado, clave protegida, borradores consentidos explícitos, guardado transaccional, recuperación y errores sin reset destructivo | Incorporar outbox durable en transacción y conservar compatibilidad mediante migraciones |
| RF07 | Consulta, filtros, edición/anulación local con confirmación/motivo y detección local de duplicado | Bloqueos por envío incierto, conflictos entre dispositivos y corrección supervisada |
| RF08 | Detección real de conectividad y estado de captura local; cero envíos o ACK simulados | Retrofit/WorkManager, cola, backoff, reconciliación, idempotencia, ACK y pruebas de caída |
| RF09 | Resumen calculado desde Room por evento/rango, distinguiendo borradores/anuladas; alcance «este dispositivo» | Resumen local/remoto sin doble conteo, continuidad tras limpieza confirmada |
| RF10 | Panel del perfil supervisor de demo sobre datos de este dispositivo; consulta y correcciones locales permitidas, sin inventar otros dispositivos | Consolidación de sedes, frescura/reportes, correcciones concurrentes y auditoría del servidor |
| RF11 | Cámara real, lector de formato sintético documentado, confirmación y fallback manual; no conservar imagen/QR crudo | Mantener funcionalidad; soporte de cédula real sólo al validar su formato, nunca afirmar que el QR sintético lo acredita |
| RF12 | Catálogo local con versión/fuente/fecha y pruebas de referencias; sin API institucional supuesta | Actualización desde backend, transacción, cache y recuperación ante descarga inválida |
| RF13 | Crear/editar eventos y asignaciones de perfiles sintéticos localmente, selección contextual y conservación del contexto histórico | Administración remota, autorización por sede y tratamiento de revocación offline |

Perfil de demo: selección explícita de identidades ficticias, sin passwords reales ni tokens fabricados. El aislamiento funcional evita mezclar registros durante el recorrido, pero no prueba autenticación ni autorización resistente a un atacante. Se conserva el formulario de login para E2, no se finge autenticación de servidor en E1.

## Inventario de pantallas y formularios E1

| Pantalla/flujo | Operación real esperada |
|---|---|
| Entrada a demo / contexto | Seleccionar perfil y evento ficticios; aviso persistente de demo local |
| Evento/asignaciones | Crear/editar datos locales, validar fechas/contexto y seleccionar captadores sintéticos |
| Inicio | Resumen local, acceso a bandeja, nueva captura y estado de conectividad |
| Captura | Datos de contacto/educación, intereses y opcionales del SRS; errores e iconos por campo |
| Cámara y ubicación | Acceso efectivo al recurso, permisos, resultados visibles y recuperación/manual |
| Consentimiento/revisión | Aceptación desmarcada, texto/versionado, guardado válido o borrador consentido explícito |
| Bandeja/detalle | Buscar/filtrar, recuperar, editar o anular dentro de reglas locales; conservar después de reiniciar |
| Resumen / supervisor local | Totales reales, alcance del dispositivo y corrección local con motivo; no reportes ficticios de teléfonos remotos |

Estos flujos pueden compartir pantallas y componentes. No imponer una pantalla por cada fila ni agregar campos no declarados. Las restricciones de RUT/evento y conservación de snapshots del SRS siguen vigentes; una corrección no se convierte en edición libre de identidad.

## GPS, cámara y conectividad en E1

Los tres se mantienen por decisión del equipo aunque P2 exija al menos dos recursos. GPS/cámara deben demostrar funcionamiento nativo por sí mismos; no depender de la valoración de conectividad para cubrir el mínimo.

| Recurso | Criterio comprobable |
|---|---|
| GPS | Permiso concedido/denegado, ubicación válida o falta de posición, resultado visible con fuente y antigüedad; sin coordenadas inventadas |
| Cámara | Lectura de código sintético válido, formato desconocido, cancelación y permiso denegado; ingreso manual sigue disponible |
| Conectividad | Observar cambios reales, actualizar UI y conservar formulario/Room al activar/desactivar red; diferenciar red detectada de internet validado y de API alcanzable |

Sin API en E1, el estado de servicio es «integración remota aún no habilitada»; no «servidor disponible» por detectar Wi-Fi. No implementar sondeos a un servicio ajeno sólo para mostrar un indicador verde. Conectividad no dispara una sincronización inexistente. En E2 será una señal para programar trabajo, nunca prueba suficiente de éxito del envío.

## Calidad y aceptación transversal

Se conservan Room cifrado, protección de claves, consentimiento antes de persistir, cero secretos en Git/APK/logs, datos ficticios, no pérdida por reinicio/logout/edad y revisión humana. Si hay incompatibilidad de dependencia o tiempo insuficiente, informar evidencia y propuesta; ningún cambio elimina controles para que compile o alcanzar una fecha.

En E1 se verifican validadores, CRUD local, cifrado, separación funcional de perfiles, reinicio, fallo de escritura, borradores, duplicados locales, métricas, permisos, conectividad y adaptación visual. Se mantienen los objetivos internos de dataset de 400 registros, medición de transacción local y recorrido de captura del SRS; registrar medición/entorno, no inventar resultados ni tratarlos como exigencias literales de P2. Animaciones deben corresponder a estado real.

En E2 se añaden autorización remota, contratos, migraciones, pérdidas de ACK, reintentos, conflictos, dos captadores/supervisor, catálogo remoto, API externa y cobertura del código lógico de al menos 80 %. Elegir métrica/herramienta de cobertura y su alcance antes de medir, conservando visible la interpretación docente pendiente. No sustituir pruebas reales de integración por mocks.

La sesión de 12 h, precisión GPS, recordatorio a las 24 h y purga propuesta siguen siendo PT con su estado anterior, no nuevas aprobaciones del cliente. Sesión y purga de sincronizados se implementan en E2; en E1 no hay datos sincronizados que purgar. No ampliar el alcance a políticas productivas o datos reales.

## Evolución sin rehacer ni mezclar datos

E1 usa dominio/repositorios/Room y servicios de plataforma separados de composables; identificadores y snapshots estables. Los contratos locales se revisan antes de dividir implementación entre compañeros. E2 agrega fuente remota/DTO/outbox después de revisar OpenAPI. Se cambia lo necesario para integrar, no toda la estructura ni los nombres por preferencia de otro colaborador.

En E1, una captura completa, consentida y con contexto válido se guarda READY con transporte NOT_QUEUED. Las incompletas consentidas guardadas explícitamente son DRAFT; las completas sin contexto válido son NEEDS_CONTEXT. La UI muestra «guardado en este dispositivo; envío no habilitado en esta entrega» después de guardar. PENDING/SENDING/SYNCED se utilizan sólo cuando exista la cola/operación correspondiente en E2. No introducir un Worker vacío ni devolver éxito desde una API ficticia.

La demo E1 queda identificada y aislada de la configuración de integración E2. Actualizar esquema no debe borrar pendientes. Sus perfiles sintéticos no son identidades de servidor: no enviar automáticamente datos E1 al habilitar red. Si se necesita importar fixtures, preparar procedimiento explícito con mapeo de usuarios/eventos, consentimiento y validación; preservar el origen, sin autoasignar capturas a quien inicia sesión.

## Infraestructura, firma y asuntos externos

E1 no necesita servicios cloud ni servidores Spring para ejecutarse. Código, documentación técnica, tareas y revisión deben acompañar el trabajo; el flujo Git se describe en CONTRIBUTING. E2 conserva propuestas de dos servicios, PostgreSQL y custodia externa de secretos, aún sin contratar ni desplegar.

La API externa es una tarea E2 distinta de nuestra API de catálogo; falta seleccionar una que aporte al caso. No se autoriza aumentar requisitos del producto para justificar una API arbitraria.

Se mantiene APK release como entregable del producto. Su exigencia ya en P2 sigue pendiente por la diferencia entre caso y rúbrica; reservar verificación de empaquetado en E1 y entregar según aclaración docente. En E2 la firma sí es explícita. La petición de `.jks` requiere acordar canal/llave exclusiva de evaluación, sin publicar claves privadas. No bloquear el trabajo local por esa consulta.

La distribución está acordada como plan del equipo; la confirmación docente de la frontera local/remota y de firma no se presume. Si exige algo adicional en E1, registrar cambio, impacto y tareas concretas. No volver al plan de «todo en P2» sin una decisión explícita.

## Cierre de una entrega

Cerrar tarea = criterios de su hito + evidencia pertinente + revisión de otro integrante + documentación del incremento. Cerrar RF = todos sus criterios finales comprobados, incluidos remotos cuando apliquen. README y tablero deben diferenciar ambos estados.

E1 se acepta cuando otro integrante puede construir/ejecutar desde las instrucciones, completar los flujos locales con los tres recursos, recuperar datos tras reinicio y explicar/modificar el código. E2 se acepta al ampliar ese recorrido con servicios reales, API externa, pruebas/cobertura y firma. Cada entrega conserva commit/tag y artefactos identificados, siguiendo las condiciones docentes aplicables.
