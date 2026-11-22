# Sesión recordada e imagen en Inicio

22/11/2026. Incremento solicitado después de la prueba física de personalización. Martín confirmó que galería, cámara y apodo funcionan en el Redmi 9A; pidió mantener el acceso al reabrir la app y utilizar la foto junto al saludo de Inicio.

## Comportamiento

El servidor emite JWT de **14400 segundos (4 horas)** para las nuevas sesiones. Al abrir la app durante esa ventana, con red disponible y en el mismo arranque del teléfono, se recupera el token cifrado y se consulta `/me`. Solo después de confirmar cuenta ACTIVE, misma identidad, rol y sedes e importar su perfil local se habilita la navegación y la transmisión. ADMIN puede recuperar sesión online, pero continúa sin acceso offline.

No se conserva contraseña, hash de contraseña ni un refresh token. Cerrar el proceso no elimina el acceso recordado; **Cerrar sesión** sí lo elimina. Vencimiento, reinicio del teléfono, cuenta desactivada, cambio de rol/sedes o rechazo 401/403 exigen nuevo login. Abrir la app o consultar `/me` no renueva las cuatro horas.

La recuperación requiere el mismo URL de backend que emitió el token. Un cambio de servidor no envía automáticamente el JWT anterior al nuevo destino. Se usa tiempo monotónico incluyendo suspensión y BOOT_COUNT; cambiar la fecha/hora no amplía la ventana. Si no puede verificarse el arranque, se mantiene el login normal.

Un fallo de red/servidor no autoriza la sesión recuperada ni habilita envíos. Conserva el token aún vigente para reintentar al volver a primer plano, sin impedir el login manual. La autorización offline operacional de hasta 12 horas conserva sus reglas separadas y la acción explícita Continuar sin conexión; no se renueva al restaurar online y no permite transmitir un JWT vencido.

El JWT dura más que antes: los servicios que confían exclusivamente en sus claims pueden aceptar un token emitido hasta cuatro horas, más su tolerancia de reloj. No se incorporó revocación inmediata ni refresh. Android comprueba `/me` al restaurar/volver a primer plano y antes de sincronizar; esas comprobaciones no equivalen a revocación en todos los clientes/servicios. La integración institucional de identidad sigue aplazada.

En la actualización inicial debe iniciarse sesión una vez, porque las APK anteriores no guardaban el JWT y los tokens ya emitidos conservan su duración original de 15 minutos.

## Persistencia

`RememberedSession` contiene la respuesta de login, URL del backend, referencia monotónica, identificador de arranque y versión de esquema. El token se guarda en un archivo privado atómico sin backup, cifrado/autenticado AES-GCM con clave Android Keystore no exportable. No se guarda en SharedPreferences, Room ni SavedState. La sesión offline conserva su archivo separado, que continúa sin contener JWT.

Logout invalida primero la revisión del almacén y elimina archivo/clave, impidiendo que una escritura antigua o un resultado de `/me` pendiente resuciten la sesión. Las instancias del almacén comparten exclusión/revisión por archivo durante el mismo proceso. Alteración del ciphertext impide recuperarlo. Los errores no imprimen tokens ni credenciales. No se borra Room, personalización ni cola de sincronización al cerrar sesión.

## Imagen en Inicio

La tarjeta junto al saludo utiliza el avatar de la misma cuenta activa que Perfil. Guarda el vínculo al UUID y actualiza la foto al volver de Perfil; al eliminarla muestra las iniciales. El apodo sigue aplicándose al saludo. No se añade almacenamiento ni carga de fotos al servidor. Un cambio de cuenta no conserva la imagen de la anterior.

`ProfileAvatar` se comparte entre Perfil e Inicio y decodifica la miniatura fuera del hilo principal. El componente de Inicio ocupa 56 dp y el de Perfil mantiene su tamaño habitual. Resumen no incorpora un avatar adicional.

## Código

Rutas relativas a `android/app/src/main/java/io/github/grupo6/capturaprospectos/`:

- `data/session/RememberedSession.kt`: duración, arranque y servidor vinculados al registro.
- `data/session/AndroidRememberedSessionStore.kt`: cifrado, archivo atómico y revisión compartida.
- `ui/access/DemoAccessViewModel.kt`: login, restauración, revalidación, expiración y logout.
- `data/remote/GatewayApiClient.kt`: admite JWT hasta cuatro horas, conservando la compuerta de transmisión.
- `MainActivity.kt`: restaura cuando Room/conectividad están preparados; conecta la foto de la cuenta activa.
- `ui/profile/PersonalProfileCard.kt`, `ui/hub/MainHubRoute.kt`, `ui/hub/MainHubScreen.kt`: avatar compartido.

Backend: `users-service/.../controller/AuthController.java`, constante `SESSION_SECONDS`. El contrato OpenAPI y su unificado se actualizaron a la duración vigente.

## Verificación

- Android JVM: **268 pruebas aprobadas**, ocho nuevas de restauración/expiración, cambios de permisos, servidor, logout durante petición y ADMIN online.
- Android API 34: **18 pruebas aprobadas** de almacenes recordado/offline, contexto Room, Perfil e Inicio. Incluyen cifrado real, recreación del almacén, manipulación del archivo, logout entre instancias y visualización/eliminación del avatar en Inicio. No se utilizaron fotos personales.
- `assembleDemoDebug`, `assembleDemoDebugAndroidTest` y `lintDemoDebug`: correctos; Lint sin errores, 42 advertencias.
- Backend: **60 pruebas aprobadas**, incluido vencimiento emitido y diferencia exacta exp/iat de cuatro horas. Comprobación JaCoCo por servicio aprobada. Los porcentajes Android del corte anterior no se atribuyen a este incremento; no se repitió aquí la suite completa de cobertura instrumental.
- Backend local persistente reiniciado conservando sus bases; login de `captor-a` confirmó `expiresIn=14400`, sin imprimir JWT.
- APK actualizada en Redmi mediante `install -r` y abierta; redirección USB al gateway 8080 comprobada. No se borraron datos de la aplicación.
- Martín confirmó en Redmi 9A que la foto de la prueba anterior se conservó y aparece en Inicio, y que la sesión se recupera correctamente después de cerrar la app o cambiar de pestaña, permitiendo volver a ingresar rápidamente. Ambas mejoras quedan aprobadas por reporte del usuario, junto con las pruebas previas de cámara/galería/apodo. No se inspeccionaron fotos ni credenciales personales.
- La espera física de cuatro horas no se realizó; los límites de vencimiento continúan respaldados por las pruebas con reloj inyectado, no por esta comprobación manual.

APK debug local: `Docs/revision-sesion/G6-sesion-inicio-Redmi9A-debug.apk` desde AppMovil. SHA-256: `404dc009e5b9fceed9c2b95ebe0d9506f4503917d5413a15b6d04fa71c50bcf1`. Compilada para `http://127.0.0.1:8080/` y conexión USB al gateway del PC. No es una release final ni elimina la dependencia del backend local.
