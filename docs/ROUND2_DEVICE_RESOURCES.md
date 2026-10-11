# Ronda 2 · recursos nativos corregidos

Candidato en `feature/e1-ronda2-device-resources`, sobre 925810f con las correcciones de alta/hub y registros. La revisión encontró un escáner que sólo pegaba texto/fixtures y GPS aplicado automáticamente; este candidato corrige ambos. No acredita cierre E1 ni revisión humana.

## Cámara y QR

CameraX enlaza Preview e ImageAnalysis al ciclo de vida. ML Kit decodifica sólo QR con modelo incluido en la APK; las imágenes se procesan en memoria, cada ImageProxy se cierra y el analizador/observador/scanner se liberan al salir. Los callbacks tardíos se ignoran. El permiso CAMERA se solicita al pulsar «Escanear código QR asistido», con denegación, explicación, reintento/ajustes e ingreso manual. No hay cuadro para pegar texto presentado como cámara.

El payload se pasa transitoriamente a DemoQrParser: sólo G6DEMO:1 con fullName y rut, límites vigentes y sin URLs/cédula real/nacimiento. El candidato se muestra y el usuario confirma el RUT compatible; si ya escribió otro RUT, debe aceptar explícitamente su reemplazo. fullName no se divide ni se copia a givenNames; nombres/apellidos se completan manualmente. Cerrar conserva los campos escritos y elimina el candidato. No se guardan imágenes ni payloads crudos, ni se ejecutan URLs.

Dependencias nuevas y fijas: CameraX camera2/lifecycle/view 1.5.3 y com.google.mlkit:barcode-scanning 17.3.0 (modelo incluido). Son necesarias para cámara real y lectura offline, compatibles con minSdk 26/compileSdk 37; no se actualizan las versiones existentes. Referencias primarias: [CameraX](https://developer.android.com/jetpack/androidx/releases/camera), [análisis de imágenes](https://developer.android.com/media/camera/camerax/analyze), [ML Kit Android](https://developers.google.com/ml-kit/vision/barcode-scanning/android). El manifiesto E1 retira INTERNET añadido por bibliotecas; la decodificación comprobada funciona sin ese permiso.

## Ubicación y estado

Al pulsar «Adquirir GPS» se solicitan FINE y COARSE en contexto. Sin permisos se conserva la entrada y se explica la alternativa manual o confirmación del evento. LocationManager usa GPS sólo con permiso preciso; con permiso aproximado utiliza el proveedor de red, sin fingir precisión. ServicesDisabled, PermissionDenied, TimedOut y Unavailable se muestran como resultados distintos. Solicitud acotada a 10 s, listener retirado al cancelar y protección de la carrera cancelación/registro.

LocationPolicy valida coordenadas, precisión <=100 m y edad <=5 min. Un candidato válido sigue sin aplicarse hasta confirmación explícita; al confirmar se revalida. Rechazarlo preserva ubicación anterior; mala precisión/antigüedad no la sustituye. Confirmar coordenadas del evento es otra acción explícita, sin deducir comuna/establecimiento ni inventar GPS. Guardar se bloquea durante adquisición, escaneo o candidato pendiente de confirmación. Descartar/reset/liberar el formulario cancela solicitudes y bloquea resultados/callbacks tardíos. Guardado en curso y alta ya confirmada no aceptan cambios nativos. El dueño retenido conserva memoria por perfil/evento, sin SavedState con datos personales.

## Controles realizados y pendientes

147 JVM aprobadas, ambas APK y Lint 24 avisos/0 errores. En Xiaomi Android 14/API 34: tres pruebas nativas/decoder y ocho UI por separado, con hashes APK coincidentes. Decoder real sobre dos bitmaps QR ficticios en androidTest/assets: candidato válido y URL rechazada. El test de proveedor nativo comprueba PermissionDenied con FINE/COARSE sin conceder; no se presenta ese resultado como GPS obtenido. UI: dos nuevos casos de confirmación/reemplazo y seis regresiones, con dobles y apertura asistida ADB. Las pruebas originales se conservan; las expectativas de aplicación automática/nombre se corrigen contra ROUND1_INTERFACES.

Comprobación física pendiente en el Xiaomi (sólo datos ficticios): enfocar el QR de prueba con cámara, aceptar/rechazar CAMERA y salir/girar sin pérdida ni cámara activa; conceder ubicación aproximada/precisa y comprobar precisión/edad antes de confirmar; negar permanentemente y recuperar desde ajustes; servicios apagados, timeout y alternativa explícita del evento. No se ha realizado esa matriz ni se ha concedido ningún permiso durante estos tests. Giro real de MainActivity, recorrido UI+Room conjunto, TalkBack y revisión visual completa también pendientes.

[Resultados separados](ANDROID_BASELINE.md), [fuentes/APK/suites](evidencias/device-correction-2026-10-10.json). La prueba cifrada de Room v4 es la del bloque previo, no una prueba nueva de captura nativa persistida. Sin backend, ACK, integración en develop ni aceptación humana/cierre de E1.
