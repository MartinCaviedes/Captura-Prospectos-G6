# Personalización local del perfil · candidato E2

22/11/2026 · `feature/e2-personalizacion-perfil`. Apodo y foto opcionales, separados de la identidad y autorización del servidor. Disponibles para CAPTADOR/SUPERVISOR en Perfil y para ADMIN mediante Mi perfil, sin exigirle evento.

## Alcance

El apodo se muestra en el perfil y en el saludo del panel. Puede quedar vacío para utilizar el nombre de cuenta; máximo 40 caracteres Unicode, sin controles. Se conserva visible el nombre de cuenta original, correo y rol. No cambia login, contraseña, rol, sedes, propietario de capturas ni datos enviados al backend.

La foto puede elegirse con el selector de imágenes de Android, tomarse como miniatura con la cámara del sistema o eliminarse. Cancelar/denegar permisos conserva la imagen anterior. No se requieren permisos amplios de galería/almacenamiento ni nuevos permisos del manifiesto. El permiso CAMERA existente se solicita únicamente al pulsar Tomar foto, con explicación de perfil separada del QR.

La personalización se guarda **por UUID de cuenta y en este dispositivo**. Persiste tras cerrar sesión/reabrir; no se sincroniza entre teléfonos ni se sube una foto al servidor. Una cuenta distinta nunca hereda apodo/foto ni un resultado pendiente del selector/cámara de otra cuenta. La operación de selección se vincula a un token efímero y al dueño de sesión; recrear Activity conserva la operación en su ViewModel, pero morir el proceso exige repetir una selección no guardada.

## Almacenamiento y tratamiento de imagen

Archivo atómico privado por cuenta en `noBackupFilesDir`, cifrado/autenticado con AES-GCM y clave Android Keystore, con UUID como dato autenticado. No modifica Room ni las migraciones existentes. Guardar apodo conserva foto y guardar/eliminar foto conserva apodo; cambios se mezclan con el último valor dentro de una exclusión mutua. Un fallo no sustituye silenciosamente el archivo ni reinicia datos de la app.

Solo se lee el URI seleccionado. Entrada máxima de 10 MiB, decodificación muestreada, recorte central cuadrado y JPEG de avatar de 256×256, máximo 128 KiB. Se recodifican los píxeles: no se copia el archivo original ni sus metadatos EXIF/GPS; no se guarda el URI. Galería mantiene sus archivos originales. La cámara usa `TakePicturePreview`, una miniatura suficiente para avatar, sin guardar una captura de tamaño completo en la galería. No se incorpora editor de recorte, reconocimiento facial ni servicio externo de imágenes.

El selector y la cámara no se lanzan automáticamente. El usuario elige la imagen o toma la foto. La importación/procesamiento y el cifrado se ejecutan fuera del hilo principal. Logout/cambio de cuenta cancela resultados de UI obsoletos; un guardado ya iniciado queda vinculado únicamente a su cuenta original. Sin acceso al archivo/clave, el perfil informa error y ofrece Reintentar, conservando identidad/autenticación/capturas.

## Fuentes y validación

- [Photo Picker: selección limitada y fallback a ACTION_OPEN_DOCUMENT](https://developer.android.com/training/data-storage/shared/photo-picker).
- [TakePicturePreview: miniatura Bitmap](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicturePreview).
- [ImageDecoder: decodificación muestreada](https://developer.android.com/reference/android/graphics/ImageDecoder).

Pruebas de estado con repositorio simulado; pruebas Android con Keystore, archivos cifrados e imágenes sintéticas; pruebas Compose de identidad inmutable, apodo, cancelación/error y aislamiento por cuenta. La elección real de galería/cámara en Redmi se registra solo cuando se observe o el usuario la reporte. No se accede a fotos personales mediante herramientas ni se publica material de perfil.

### Resultado automatizado · 22/11/2026

- `testDemoDebugUnitTest`: 260 pruebas, cero fallos (incluye nueve de personalización).
- Emulador API 34: siete pruebas de almacenamiento/procesamiento, cinco de interfaz de perfil y una de apariencia; 13 aprobadas. Se verificaron cifrado, aislamiento entre cuentas, actualización concurrente, fallos/cancelación, orientación EXIF y decodificación alternativa, imagen seleccionada mediante URI, normalización sin metadatos y acceso de ADMIN sin evento.
- `assembleDemoDebug` y `assembleDemoDebugAndroidTest`: correctos. `lintDemoDebug`: cero errores y 41 advertencias; se mantienen avisos de estilo y la recomendación de usar AndroidX ExifInterface en lugar del lector nativo utilizado en la ruta API 26/27. Esto no acredita una prueba física en esas versiones.
- La compilación para Redmi usa `-Pg6ApiBaseUrl=http://127.0.0.1:8080/` y redirección ADB al backend local temporal. No constituye un despliegue en AWS ni una APK de distribución final.

### Archivos principales

- `domain/profile/PersonalProfile.kt`: modelo y contrato del almacenamiento.
- `data/profile/AndroidPersonalProfileStore.kt`: persistencia privada cifrada por UUID.
- `data/profile/AvatarImages.kt`: lectura limitada, orientación, recorte y recodificación.
- `ui/profile/PersonalProfileViewModel.kt`: edición, guardado y vinculación de selecciones a sesión.
- `ui/profile/PersonalProfileCard.kt`: tarjeta, selector y cámara.
- `MainActivity.kt`, `ui/shell/ProfileScreen.kt` y `ui/access/UserAdminScreen.kt`: conexión con sesión, saludo, Perfil y Mi perfil.

Rutas anteriores relativas a `android/app/src/main/java/io/github/grupo6/capturaprospectos/`. Pruebas en los paquetes `ui.profile` (JVM y Android), `data.profile` y `ui.shell` (Android).

### Comprobación manual

APK debug instalada mediante actualización (`install -r`) y abierta en Redmi 9A el 22/11/2026, sin borrar datos. Artefacto local: `Docs/revision-perfil/G6-perfil-Redmi9A-debug.apk` desde la carpeta de trabajo AppMovil; SHA-256 `99d2aa595c0eee716bb55ae82764ff64db9847006436f656ecb271f7b43029d4`. Copia en Descargas del teléfono: `Captura-G6-Perfil-debug.apk`.

Martín confirmó en Redmi 9A que cambiar la imagen desde galería y cámara y modificar el apodo funcionan sin problemas. La prueba previamente pendiente queda aprobada **por reporte del usuario**; no se inspeccionaron fotos personales. Cancelación, aislamiento y borrado mantienen la evidencia automatizada descrita arriba.

La comprobación se había pospuesto al perder conexión con el backend temporal; se completó después de preparar el [backend local persistente](LOCAL_WINDOWS_SETUP.md). Martín también confirmó posteriormente la conservación de la imagen anterior y su visualización en Inicio, junto con la recuperación de sesión al reabrir la app o cambiar de pestaña; evidencia en [sesión recordada y foto en Inicio](REMEMBERED_SESSION.md).
