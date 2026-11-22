# Sesión local sin conexión · E2

Actualización posterior del 22/11: [sesión online recordada hasta cuatro horas](REMEMBERED_SESSION.md). El archivo offline descrito aquí continúa sin JWT y mantiene su ventana de doce horas. Las referencias inferiores a JWT de 900 segundos y restauración solo offline describen el corte original, no el acceso online posterior.

22/11/2026 · candidato local en `feature/e2-sesion-offline`, sobre el incremento de cuentas. No publicado, sin commit ni merge. No modifica el backend ni la estructura de Room.

## Comportamiento y alcance

El primer acceso exige login remoto y consulta `/me`. Si ambos confirman una cuenta ACTIVE de CAPTADOR o SUPERVISOR y la importación del perfil a Room tiene éxito, se guarda una autorización local cifrada. ADMIN no dispone de acceso offline. Registro y gestión de usuarios requieren servidor.

La ventana dura **hasta 12 horas desde la verificación del login**, no desde que se pierde la red. Se mide con `SystemClock.elapsedRealtime()`, incluyendo el tiempo en suspensión, y se vincula a `Settings.Global.BOOT_COUNT`. Cambiar la fecha/hora no extiende la ventana. **Reiniciar el dispositivo exige un nuevo acceso con conexión**; cerrar/reabrir la aplicación durante el mismo arranque sí permite restaurar la autorización. Si el dispositivo no informa el identificador de arranque, no se concede restauración offline. Consultar `/me` no renueva automáticamente ni las 12 horas ni el JWT.

Al perder la red durante una sesión válida se permite continuar localmente y se muestra un aviso. Al reabrir la app, con ausencia de red confirmada, el acceso presenta **Continuar sin conexión** y requiere pulsación explícita. No se compara la contraseña ingresada contra credenciales locales. Una red desconocida o sin validar no basta para ofrecer restauración: tampoco se confunde un backend caído con ausencia de red.

La autorización local contiene identidad, rol, sedes y referencia de tiempo. Se cifra/autentica con AES-GCM y una clave no exportable de Android Keystore, en `noBackupFilesDir`. No contiene contraseña, hash ni JWT. Corrupción o alteración del archivo impide recuperarlo. Logout elimina archivo y clave; una escritura de la misma instancia iniciada antes de invalidarlo se rechaza mediante revisión. Se protege contra lectura/modificación corriente del archivo, no contra un dispositivo rooteado o un proceso de aplicación comprometido con acceso a Keystore. No es un token offline emitido o firmado por el servidor.

## Separación de autorización local y transmisión

El JWT online permanece en memoria, con máximo de 900 segundos. El permiso offline no genera ni prolonga JWT. `BackendSession.bearerHeader()` no entrega autorización para enviar durante operación offline ni mientras se revalida el acceso; `/me` usa una cabecera separada exclusivamente para esa comprobación.

Al recuperar red, una sesión que conserva su JWT vigente consulta `/me` antes de volver a habilitar transmisión. Si se restauró tras cerrar el proceso, no hay JWT: se solicita login. Un fallo transitorio de revalidación mantiene los envíos bloqueados; un rechazo 401/403 de esa consulta, desactivación o cambio de rol/sedes invalida la autorización local. Una solicitud de login nueva descarta primero la anterior autorización: su rechazo no habilita un fallback local.

La desactivación del servidor no puede conocerse mientras el equipo permanezca sin red. El alcance de permisos de otros servicios y la tolerancia de sus JWT siguen siendo los documentados en el incremento de cuentas. Este cambio **no añade una cola durable, conciliación ni revalidación antes de cada petición de prospectos**: eso corresponde al siguiente incremento de sincronización. Los envíos ya iniciados antes de un cambio de conectividad no se cancelan de forma retroactiva.

Vencimiento, logout o rechazo cierran el acceso de la UI y bloquean transmisión. No borran Room, capturas, auditoría ni asignaciones. La selección de evento se realiza de nuevo después de restaurar el perfil. No se fuerza una migración ni se resetean datos.

## Código

Rutas dentro de `android/app/src/main/java/io/github/grupo6/capturaprospectos/`:

| Archivo | Responsabilidad |
|---|---|
| `data/session/OfflineSession.kt` | Vigencia, arranque, roles permitidos y contrato del almacenamiento |
| `data/session/AndroidOfflineSessionStore.kt` | Reloj Android, AES-GCM, Keystore y archivo atómico sin backup |
| `data/remote/GatewayApiClient.kt` | JWT en memoria y bloqueo de cabeceras de transmisión |
| `data/remote/RemoteAccounts.kt` | Consulta de revalidación separada de las operaciones protegidas |
| `ui/access/DemoAccessViewModel.kt` | Emisión, restauración, expiración y transiciones red/sesión |
| `ui/access/DemoAccessScreen.kt` | Acción explícita para continuar offline |
| `MainActivity.kt` | Inyección de almacenamiento/reloj, conectividad y aviso de sesión |

## Verificación

Las pruebas JVM usan reloj, almacenamiento y servidor de cuentas simulados explícitamente. Las instrumentadas usan Android Keystore y Room/SQLCipher reales, archivos/bases exclusivos de prueba y cuentas ficticias. La integración HTTP previa de cuentas se repite contra gateway/usuarios reales con H2 temporal; no se atribuye esta evidencia a AWS ni a PostgreSQL.

Los casos nuevos cubren límite exacto de 12 horas, retroceso del reloj monotónico y cambio de arranque, exclusión de ADMIN y cuentas no activas, restauración explícita sin JWT, bloqueo hasta revalidación, fallo de servidor, rechazo, cambio de permisos y logout durante una consulta en curso. En Android se comprueban archivo cifrado, recreación del almacén, manipulación del ciphertext, invalidación de escrituras antiguas y restauración sobre Room sin crear credenciales locales.

La conservación de 60 capturas, auditoría y asignaciones se comprueba además en la regresión instrumentada existente. No se realizó una espera física de 12 horas ni un reinicio físico del dispositivo: esos límites se verifican con el reloj inyectado. La prueba de Room combina ViewModel, cifrado y repositorio; no sustituye un recorrido manual completo de MainActivity.

Resultados ejecutados sobre este candidato:

| Control | Resultado |
|---|---|
| `testDemoDebugUnitTest` | 224 pruebas JVM, 0 fallos/errores; 15 casos nuevos de autorización offline |
| `assembleDemoDebug` | APK debug construida correctamente |
| Paquete instrumentado `ui.access`, G6_Pixel6_API34 / Android 14 | 21 pruebas, 0 fallos/errores; 5 casos nuevos de almacenamiento, restauración sobre Room y pantalla offline |
| `lintDemoDebug` | 0 errores, 38 advertencias; sin reglas desactivadas ni baseline nuevo |
| `git diff --check` | Sin errores de whitespace |

Logs locales, fuera del repositorio: `Residuos ChatGPT/offline-build.log`, `offline-device.log` y `offline-lint.log`. Los servicios temporales se cierran al finalizar. La APK continúa en `android/app/build/outputs/apk/demo/debug/app-demo-debug.apk`, con firma debug; no es una release final.

## Siguiente incremento

El incremento posterior añade [sincronización persistente](PROSPECT_SYNC_CONTRACT.md): cola, enlaces durables, comprobación previa y reintentos idempotentes. Mantiene límites explícitos de conciliación de fichas anteriores, conflictos y ejecución en segundo plano. Los límites de transmisión descritos arriba corresponden al corte original de sesión offline. AWS, geocodificación inversa, personalización, cobertura JaCoCo y firma release/distribución final mantienen seguimiento separado.
