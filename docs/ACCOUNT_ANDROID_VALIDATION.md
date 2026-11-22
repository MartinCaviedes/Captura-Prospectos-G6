# Cuentas E2 · integración Android

21/11/2026 · candidato local en feature/e2-cuentas-android. Conserva el backend y contratos previos no publicados. Sin commit, PR, merge ni APK release publicada en este incremento.

## Comportamiento

Se conserva el formulario del equipo y se conecta a POST /auth/register. Solo solicita nombre, correo y contraseña: no asigna rol/sedes ni crea una cuenta local operativa. PENDING muestra «Solicitud pendiente» y permite volver al acceso. Los mensajes se eligen por códigos de error, sin reproducir mensajes arbitrarios del servidor.

Login consulta al servidor y GET /me antes de habilitar navegación. Credenciales inválidas, cuenta pendiente/desactivada y fallo de conexión no se convierten en login local. El fallback de fixtures ya no participa en el acceso del producto; sus datos/utilidades de prueba se conservan. No se simula Microsoft/Duoc ni recuperación de contraseña.

ADMIN tiene navegación propia, sin evento y sin convertirse en SUPERVISOR. Lista cuentas por estado con paginación, revisa identidad, asigna rol/sedes y habilita/desactiva con confirmación. ADMIN está protegido tanto en pantalla como servidor. Un conflicto de versión refresca el detalle sin repetir la mutación. La edición se conserva al recrear la composición para el mismo administrador y se limpia al cambiar de cuenta.

CAPTADOR/SUPERVISOR importan la identidad confirmada a Room, sin contraseña/hash del servidor, sin sede por defecto y sin fusionar cuentas por coincidencia de correo. Upsert actualiza el perfil sin el borrado implícito de REPLACE. Se actualizan sus sedes, conservando capturas/auditoría/relaciones de eventos. El correo remoto se mantiene en memoria y se obtiene nuevamente al iniciar sesión.

## Sesión y límites

- JWT solo en memoria con límite monotónico de 15 minutos. Consultar /me no renueva ese plazo. Logout limpia sesión sin borrar Room.
- Al volver a primer plano se revalida identidad; desactivación o cambio de permisos requiere nuevo acceso. 401/403 administrativos cierran la sección. Los tokens anteriores no se revocan inmediatamente en otros servicios.
- En este corte de cuentas la autorización offline seguía pendiente. El incremento posterior la implementa con límites y evidencia propios: [sesión offline](OFFLINE_SESSION_VALIDATION.md). No se recupera el fallback de contraseña.
- Revalidación antes de cada envío, cola durable y reconciliación de prospectos corresponden al siguiente incremento. No se afirma que esta integración los resuelva.
- Room no se migró ni reseteó. Los métodos antiguos de autenticación/registro local permanecen para pruebas, pero la UI de producto no los invoca.
- Personalización, eventos remotos y un rol administrador con facultades de captador no forman parte de este incremento.

## Código principal

Rutas relativas a android/app/src/main/java/io/github/grupo6/capturaprospectos:

| Archivo | Responsabilidad |
|---|---|
| data/remote/GatewayApiClient.kt y RemoteAccounts.kt | DTOs/rutas Retrofit, transporte, errores y sesión |
| ui/access/DemoAccessViewModel.kt y DemoAccessScreen.kt | Registro remoto, login y pantalla pendiente |
| ui/access/UserAdminViewModel.kt y UserAdminScreen.kt | Listado, permisos, versiones y confirmación |
| MainActivity.kt | Navegación ADMIN independiente y revalidación al reanudar |
| data/local/RoomDemoContextRepository.kt y ContextTables.kt | Importación de identidad sin credenciales y Upsert conservador |

## Validación ejecutada

| Control | Resultado |
|---|---|
| testDemoDebugUnitTest | 209 pruebas JVM, 0 fallos/errores |
| assembleDemoDebug | APK de prueba generada |
| Paquete instrumentado ui.access, G6_Pixel6_API34 / Android 14 | 16 pruebas, 0 fallos/errores |
| Retrofit real desde emulador | Registro pendiente, rechazo de login pendiente, login ADMIN, catálogo/listado, habilitación, conflicto, login CAPTADOR, denegación administrativa y desactivación |
| Room instrumentado | 60 capturas/asignaciones conservadas tras dos importaciones del perfil; no crea credenciales locales; sede desconocida rechazada sin perder registros |
| Recreación/pantallas | Retiene entradas durante recreación, consume contraseña, no ofrece privilegios en registro y exige confirmación de permisos |
| lintDemoDebug --rerun-tasks | 0 errores, 38 advertencias, sin reglas desactivadas ni baseline nuevo |
| Visual | Capturas Compose de registro, pendiente, listado y confirmación revisadas en tema oscuro |

Los ViewModel JVM usan dobles explícitos y las pruebas Retrofit JVM un servidor HTTP controlado. RemoteAccountJourneyTest sí consumió JAR reales gateway/usuarios desde el emulador, sobre H2 temporal y con claves/cuentas ficticias. No equivale a repetir PostgreSQL: su evidencia está en [validación backend](ACCOUNT_BACKEND_VALIDATION.md). Las capturas de UI usan estados de prueba; no se atribuye aprobación humana ni un recorrido completo de MainActivity mediante toques manuales.

Gradle desinstala los APK al finalizar el runner. Se repitieron solo las cinco pruebas visuales mediante AndroidJUnitRunner para recuperar las capturas; no se suman como casos nuevos. Capturas locales en Docs/revision-cuentas-android del workspace, fuera del repositorio. Logs en Residuos ChatGPT/android-accounts-final.log y android-accounts-lint.log. Los servicios temporales de integración se cerraron al terminar.

Se actualizó una aserción histórica de tres carreras: el catálogo F2027 produce nueve en esa muestra. Ahora compara IDs de las métricas contra los intereses efectivamente almacenados, manteniendo los conteos de personas/registros. No se cambió el cálculo de métricas para hacer pasar el test.

Lint sufrió un fallo interno FIR; al repetir con fuentes estables señaló PropertyEscape en la ruta SDK de local.properties. Se corrigió el separador de unidad en ese archivo ignorado y se recalculó sin caché. No se cambiaron SDK/dependencias ni se ocultaron comprobaciones.

## Ejecución manual y pendientes

APK: android/app/build/outputs/apk/demo/debug/app-demo-debug.apk. Sigue siendo demoDebug con firma debug y versión de desarrollo. Firma release/distribución final quedan para otra etapa.

En emulador, iniciar el backend actualizado de [backend/README](../backend/README.md) en host:8080. Android usa http://10.0.2.2:8080/. Se necesita una cuenta administradora aprovisionada; no hay contraseña ADMIN embebida en la APK. Registro pendiente solo habilita acceso después de revisión administrativa.

RemoteAccountJourneyTest es optativo: requiere servicios aislados en host:18080, seed local con credenciales ficticias explícitas del test y BD temporal. No ejecutarlo contra servicios reales/compartidos. Un dispositivo físico o futuro AWS requiere URL accesible y HTTPS; 10.0.2.2 solo apunta al host del emulador.

Pendientes: teléfono físico, tema claro específico de administración, TalkBack, autorización offline extendida y sincronización completa. No hubo despliegue AWS ni consumo de API externa.
