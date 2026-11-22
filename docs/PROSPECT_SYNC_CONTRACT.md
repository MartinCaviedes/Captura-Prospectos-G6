# Sincronización persistente de capturas · contrato candidato

22/11/2026. Incremento local sobre cuentas y sesión offline; sin publicación ni despliegue. Contrato definido antes de implementar sus consumidores.

## Transporte

`PUT /api/v1/prospects/sync/{localCaptureId}` con JWT vigente y permisos actuales. Cuerpo: `deviceId` UUID, `ownerId` UUID, `sourceRevision` entero positivo, `expectedRevision` entero >=0, `registration` con el DTO Create existente, `state` READY/NEEDS_CONTEXT/CANCELLED, `reason` opcional y `snapshotJson` JSON de F2027 (máximo 64.000 caracteres). La instantánea conserva ficha, intereses, sedes y consentimiento; los campos principales se validan mediante el DTO tipado. El snapshot no sustituye validaciones ni catálogos normalizados del servidor.

Respuesta: `localCaptureId`, `remoteId`, `sourceRevision`, `deviceId`. El servidor mantiene por captura local el dispositivo original, última revisión aceptada, hash de la petición y versión del prospecto asociado. Creación y enlace son una transacción. Repetir exactamente la última petición devuelve el mismo ACK, sin duplicar el alta ni la anulación. Cambiar el contenido de una revisión aceptada produce 409. Una revisión nueva exige `expectedRevision` igual a la última aceptada; cambios remotos posteriores mediante las rutas antiguas también producen conflicto. La primera revisión puede ser mayor que uno por ediciones previas del borrador.

Crear exige que ownerId sea el actor autenticado, con CAPTADOR o SUPERVISOR en la sede. Modificaciones exigen propietario CAPTADOR en su sede, o SUPERVISOR de esa sede. Cambiar propietario, dispositivo, RUT, evento o sede de una captura enlazada se rechaza. CAPTADOR puede anular su propia captura (igual que el dominio local); SUPERVISOR, las de su sede. No se añaden facultades de captura a ADMIN. Motivo obligatorio de 5–300 caracteres para anulaciones y edición por supervisor de otra persona. Se conservan restricciones de las rutas previas; la nueva ruta documenta explícitamente su política.

## Persistencia y recuperación Android

Room 4→5 añade enlaces y outbox sin modificar ni borrar capturas/auditoría. Guardado local y alta en outbox ocurren en la misma transacción. Borradores no se transmiten. Las operaciones confirmadas guardan snapshots inmutables, actor que realizó la operación y revisión; una edición posterior no modifica un envío anterior en curso. ACK y eliminación de la operación son otra transacción local. El vínculo remoto y la revisión confirmada sobreviven al cierre del proceso.

Solo se procesa la primera operación pendiente de cada captura. Una secuencia persistente conserva además el orden por RUT/evento entre capturas: una nueva inscripción no adelanta al alta/anulación anterior, ni durante un reintento. La cola se filtra por el actor que hizo cada operación y se consulta `/me` antes de cada envío, comprobando identidad, estado, rol y sedes sin renovar JWT ni autorización offline. Logout, cambios de cuenta y pérdida de autorización bloquean nuevos envíos. 401/403 detienen la sesión; red/5xx/429 reintentan con espera exponencial persistida de 5 segundos hasta 5 minutos. 400/404/409/422 conservan la operación bloqueada para revisión y no sobrescriben el servidor. No se borra una captura ni se cambia su revisión local por un error remoto.

El coordinador funciona mientras la aplicación está abierta; no se incorpora WorkManager ni se promete ejecución con la app cerrada. Al reabrir y autenticar, continúa la cola. No cancela retroactivamente peticiones ya recibidas por el servidor. No descarga ni fusiona automáticamente cambios de otros dispositivos.

## Datos anteriores

La versión anterior no conservaba enlaces remotos. La migración marca las capturas previas como de procedencia no conciliada: no intenta deducir propiedad por RUT ni reenvía automáticamente fichas posiblemente existentes. Editarlas conserva el trabajo local y exige revisión para el envío. Resolver esos casos requiere una conciliación explícita con el servidor; no se inventan IDs remotos. Los errores y la cantidad de pendientes se muestran en la aplicación.

El snapshot se almacena como JSON textual y no se devuelve en listados generales. La BD remota mantiene su protección por autenticación y ámbito; logs/errores no incluyen payloads ni credenciales. No se afirma cifrado adicional de PostgreSQL, despliegue AWS ni cierre de todos los requisitos E2.

La validación remota actual verifica DTO principal, RUT, propietario y sede. El servidor todavía no administra el catálogo completo ni comprueba asignaciones de eventos contra un servicio de eventos; acepta su UUID y conserva el contexto completo en la instantánea. `consentRecordedAt` de la entidad previa sigue siendo la fecha de recepción del servidor; la fecha/evidencia original del consentimiento queda conservada en el snapshot. No se declara normalización completa de F2027 ni descarga/reconstrucción de la ficha desde el servidor. Ambas limitaciones son distintas de la durabilidad e idempotencia de esta cola.

## Código del candidato

| Ruta dentro del repositorio | Responsabilidad |
|---|---|
| `android/.../data/local/CaptureSyncTables.kt` | Enlaces, outbox, migración 4→5 y snapshots transaccionales |
| `android/.../data/local/RoomCaptureSynchronization.kt` | Verificación de identidad, envío, ACK, reintentos y bloqueo |
| `android/.../data/local/RoomAdmissionRepository.kt` / `RoomRecordRepository.kt` | Guardado local y pendiente atómicos; retirado HTTP inmediato |
| `android/.../data/remote/SyncCaptureDtos.kt` / `GatewayApiClient.kt` | Contrato Retrofit; retirado mapa de IDs remotos solo en RAM |
| `android/.../ui/context/ContextSessionOwner.kt` / `MainActivity.kt` | Coordinación con el ciclo de vida y visibilidad de pendientes/revisión |
| `android/.../ui/records/RecordDetailDialog.kt` | Estado de transporte de cada ficha |
| `backend/prospects-service/.../CaptureSyncController.java` / `CaptureSyncService.java` | Validación, permisos, replay y transacción remota |
| `backend/prospects-service/.../CaptureSync.java` / `CaptureSyncRepository.java` | Enlace y snapshot persistidos con bloqueo/versión |
| `backend/prospects-service/db/migrations/V002__capture_sync.sql` | Migración aditiva, ejecutada por migrador antes de `validate` |

`android/...` abrevia `android/app/src/main/java/io/github/grupo6/capturaprospectos`; el paquete de las clases Java se encuentra bajo `backend/prospects-service/src/main/java/io/github/grupo6/prospects`.

## Verificación y seguimiento

Las pruebas de almacenamiento usan Room/SQLCipher y Keystore reales sobre bases `test-*`, con identidades y datos ficticios. Cubren persistencia tras reapertura, pérdida deliberada de ACK, snapshots inmutables al editar durante un envío, orden alta/anulación/reinscripción, aislamiento de usuarios, permisos modificados, respuestas inválidas, conflictos y rollback si falla insertar el pendiente. Las migraciones de bases cifradas 3/4→5 comparan todos los valores anteriores, claves e índices, y verifican que no se encolen fichas históricas.

La prueba HTTP desde emulador descarta deliberadamente la primera respuesta después del commit real del servidor, reabre Room, reintenta y después edita/anula. Comprueba un solo prospecto remoto y enlace/revisión local confirmados. Usa tres JAR reales sobre H2 temporal, no un doble HTTP. PostgreSQL 17.12 temporal verifica V001→V002 conservando una fila previa, perfil prod con `ddl-auto=validate`, permisos de BD, replay de alta/anulación y persistencia tras reinicio del servicio. El adaptador local de Windows cierra procesos abruptamente; esa ejecución no acredita graceful shutdown. Los scripts originales conservan su comprobación de cierre ordenado para entornos compatibles.

La regresión amplia encontró expectativas E1 que ya no correspondían al código del equipo: catálogo de cuatro programas/sedes, colegio ficticio anterior y ausencia de permiso INTERNET. Se ajustan a las diez carreras/ocho sedes y referencias actuales; el QR comprueba decodificación del modelo incluido sin atribuir ausencia de red al manifiesto. No se cambió catálogo, permisos ni motor QR para hacer pasar pruebas. La prueba asistida de pérdida/recuperación de Wi-Fi se ejecuta por separado, con orquestación explícita; no se cuenta como aprobada por omitirla en la ejecución automática.

Resultados finales del candidato en `feature/e2-sincronizacion-persistente`:

| Comprobación | Resultado |
|---|---|
| `mvnw verify` (tres servicios) | 53 pruebas, 0 fallos/errores; 7 casos nuevos de sincronización, incluido replay concurrente |
| `testDemoDebugUnitTest` | 224 pruebas JVM Android, 0 fallos/errores |
| `assembleDemoDebug` | APK debug construida correctamente |
| Suite instrumentada automática completa, API 34 | 120 pruebas, 0 fallos/errores; excluye únicamente el caso asistido de Wi-Fi |
| Prueba asistida de conectividad orquestada mediante ADB | 1 prueba aprobada: red validada → pérdida → recuperación; ajustes originales restaurados |
| QR real sin red activa por defecto, comprobada mediante `dumpsys connectivity` | 2 pruebas aprobadas; son repeticiones de casos de la suite, no dos casos nuevos |
| `lintDemoDebug` | 0 errores, 38 advertencias, sin baseline nuevo ni reglas desactivadas |
| PostgreSQL efímero con los tres JAR prod | Migración conservadora, permisos de BD, replay/reinicio y dos solicitudes simultáneas con un ACK compartido/un prospecto |
| `git diff --check` | Sin errores de whitespace |

Hay **121 casos instrumentados distintos aprobados**, distribuidos entre la suite automática y la prueba asistida. No se suman los QR repetidos ni la primera ejecución que detectó las expectativas antiguas. El primer intento PostgreSQL se detuvo en la comprobación de graceful shutdown de Windows; el adaptador posterior documenta explícitamente terminación abrupta, puertos separados y recuperación, sin atribuir cierre ordenado.

Logs locales fuera del repositorio: `Residuos ChatGPT/sync-validation.log`, `sync-backend-final.log`, `sync-postgres.log` y `sync-native-network.log`. Los servicios y el contenedor de prueba se cerraron al finalizar. El APK se encuentra en `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk`, con firma debug y versión de desarrollo.

No hay commit, merge ni publicación de APK release. Falta resolver explícitamente fichas históricas/conflictos, descarga de cambios remotos y ejecución con la app cerrada. AWS, API externa, personalización, JaCoCo y firma de distribución mantienen su seguimiento separado. Antes de usar este APK con PostgreSQL existente, aplicar V002 de prospectos mediante migrador y actualizar el backend; las pruebas no modifican una base persistente del usuario.
