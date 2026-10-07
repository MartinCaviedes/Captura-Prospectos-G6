# Seguridad y demostración reproducible

Incremento 06/10: Room v3 y contexto/catálogo sintético descritos en [DEMO_CONTEXT](DEMO_CONTEXT.md); inicialización explícita, sin autenticación real ni seed automático al migrar. Suite completa de 24 pruebas Android aprobada sobre v3; ver evidencia en ANDROID_BASELINE.

Estado local verificado al 06/10: SQLCipher 4.19.1 y clave aleatoria de 32 bytes, envuelta mediante AES-256-GCM con Android Keystore. Blob versionado en noBackupFilesDir, IV generado por Cipher y nombre de base como AAD; contraseña mantenida sólo durante la vida de la conexión y sobrescrita al cerrar. Ante clave/blob inaccesible se falla conservando archivos; sin reset ni migración destructiva. La suite de 24 instrumentadas sobre Room v3 incluye reapertura, rechazo de SQLite sin clave, pérdida de alias, permisos, duplicados, rollback y migraciones. Las 16 de v2 quedan como antecedente en ANDROID_BASELINE. Backup/transferencia efectiva, UI y recursos nativos siguen pendientes; no se declara auditoría de seguridad general.

Distribución 02/10/2026: [ENTREGAS](ENTREGAS.md). E1 conserva todos los controles locales (consentimiento, cifrado, claves, no pérdida, permisos y datos ficticios). Servidor/tokens/TLS remoto, scopes e integración se completan en E2; no se simula que esos controles ya existen en E1.

30/09/2026. Controles propuestos para el prototipo, no certificación ni auditoría realizada. El antecedente de pentesting previo fue comunicado verbalmente por el docente según D24; no hay hallazgos específicos disponibles que permitan afirmar que estos controles los corrigen.

## Datos y consentimiento

Sólo fixtures ficticias para desarrollo, tests, screenshots y presentación. No persistir imágenes de cédula, QR crudo o número de documento. RUT/DV válido no demuestra identidad; un fixture matemáticamente válido no debe presentarse como perteneciente a una persona real. Desactivar mensajes/correos/llamadas salientes para datasets de demostración.

Sin consentimiento no escribir PII en Room, disco, SavedState persistente, logs o telemetría. Borradores consentidos cifrados y sólo guardados por acción explícita. Cancelación limpia memoria de captura. Texto de privacidad de muestra rotulado; consentimiento institucional, menores, retención productiva y retiro requieren validación antes de un piloto real.

## Claves y datos locales

Base Room cifrada mediante integración verificada de SQLCipher. Generar clave aleatoria por instalación; protegerla con una clave del Android Keystore y algoritmo autenticado soportado. No construir algoritmos criptográficos propios. Verificar formato/nonces y manejo de fallos con APIs revisadas, no guardar la clave SQLCipher en texto plano ni derivarla del RUT.

Tokens en almacenamiento cifrado diseñado con Keystore; contraseñas del servidor no se retienen. No reemplazar cifrado por SharedPreferences en texto plano. Base/keys excluidas de backup/transferencia según versión de Android; testear configuración efectiva. Si la clave deja de estar disponible, informar y no destruir pendientes automáticamente. Véase [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

No borrar pendientes por edad, cerrar sesión o vencer un token. A las 24 h generar recordatorio de revisión. PT de limpieza sincronizados: al menos 7 días desde cierre del evento confirmado, ACK comprobado, sin correcciones pendientes y con resumen disponible. El supervisor decide anulaciones/borrados del lado servidor mediante reglas auditadas; purga local no equivale a borrar el registro central.

## Servidor y comunicaciones

Contraseñas hash mediante PasswordEncoder adaptativo de Spring Security; no cifrado reversible. Roles y scopes de sede comprobados en cada endpoint. JWT firmado asimétricamente en identidad; clave privada sólo allí; captación usa clave pública, valida issuer/audience/expiración. Refresh rotatorio revocable y límites de intentos de login. No hardcodear claves de firma en código.

HTTPS en integración compartida/release; validar certificados/hostname, sin trust-all. Para desarrollo local debug, excepción explícita y estrecha al host del backend de laboratorio si es indispensable; nunca relajar toda la app release. No imponer TLS 1.3 exclusivo sin validar matriz Android 8/proveedor/servidor. [Almacenamiento de contraseñas Spring](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

Permisos mínimos, solicitados en contexto: cámara/ubicación y denegación manejada; conectividad no se trata como permiso runtime peligroso. Componentes no exportados salvo necesidad declarada, como launcher. R8 en release con reglas necesarias y prueba de instalación/REST/Room; ofuscar no vuelve secretos los valores embebidos.

## Cómo entregar sin compartir claves privadas

Compilar sin archivos privados significa que el código y sus dependencias declaradas bastan para generar una variante de desarrollo. No significa que un servicio protegido funcione sin autenticarse ni que el APK release se firme sin una clave.

E1: demo local funcional con perfiles sintéticos y sin credenciales de servidor. Seleccionar un perfil no acredita autenticación segura. Aislar su configuración/datos de E2 y no enviar registros automáticamente al habilitar backend. Mantener cifrado, consentimiento y filtrado funcional por perfil. El modo asistido es la alternativa cuando no se puede garantizar bloqueo local al prestar el dispositivo.

Recorrido integrado E2:

1. Backend local real (servicios + PostgreSQL), configuración de ejemplo y script de preparación. Generar secretos locales al preparar el entorno, fuera de Git; seed sólo en perfil explícito `local-demo`.
2. Crear cuentas sintéticas captador/supervisor con contraseñas locales generadas y entregadas al operador por el setup, sin subirlas al repo. La app autentica realmente contra ese backend.
3. Dataset sintético y catálogo versionado en archivos de seed, no datos dispersos/hardcodeados dentro de composables. Reset de demo explícito sólo sobre base ficticia identificada; nunca apuntar a producción.
4. URL local configurable; no es un secreto por sí misma. Android emulador y dispositivo físico tienen instrucciones distintas de conectividad que deben probarse.
5. Prueba de extremo a extremo con red interrumpida/restaurada, duplicados, dos usuarios y supervisión. Esto sí acredita RF08; una API falsa no.
6. La demo local E1 se identifica visualmente y queda aislada por applicationId/configuración. Su persistencia y recursos son funcionales, pero no reemplazan el recorrido integrado E2 ni se habilita por fallo silencioso de login real.

No es necesario entregar claves del equipo al profesor. Para un backend remoto opcional, crear cuentas de evaluación limitadas y separadas de administración; distribuir por canal acordado y revocar al terminar. Un secreto dentro del APK puede extraerse, aunque venga de una variable de CI. Los [build variants de Android](https://developer.android.com/build/build-variants) permiten separar configuraciones, no ocultar secretos incluidos en la app.

## Custodia del equipo

Usar un gestor compartido con cuentas individuales y acceso del grupo, no un archivo de contraseñas en Git/WhatsApp. Proveedor aún por seleccionar según cuentas y coste; no contratar nada en esta etapa. GitHub Actions Secrets sólo para ejecución CI, no como sustituto de un gestor para lectura humana.

Clasificar: claves individuales GitHub (no compartir), contraseñas locales generadas (no necesitan ser idénticas entre integrantes), secretos de un entorno remoto compartido (centralizados y rotables), keystore release (una identidad de firma común con custodio y respaldo). La coherencia se logra compartiendo esquema y configuración de ejemplo; no todas las claves deben ser constantes ni las mismas en todos los computadores.

Pendiente nombrar custodio y suplente de firma. Password/keystore fuera de Git; documentar alias y procedimiento sin secreto. Para CI futura, permisos mínimos, acciones fijadas y evitar exposición de secretos a PR no confiables. Ante filtración: rotar primero, luego limpiar historia según alcance.

## Evidencia requerida

Comprobar por hito: E1 controles locales/recursos y E2 controles remotos/release; el reparto no elimina ninguna evidencia final. No declarar autorización de servidor probada por el simple filtrado de perfiles demo. Firma en E1 según aclaración docente; en E2 explícita. La solicitud de llave de la pauta P4 no autoriza publicar el `.jks`; resolver canal restringido/llave de evaluación con el docente y custodio.

Lectura directa de base cifrada rechazada sin clave; logs sin PII; permisos denegados sin crash; acceso cruzado a sede/usuario rechazado; no datos personales de borrador sin consentimiento; pendientes intactos tras logout/reinicio/24 h; release sin flags de demo inseguros; firma verificada. Hasta ejecutar estas pruebas, indicar «pendiente de verificar».
