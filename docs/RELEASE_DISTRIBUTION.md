# Distribución firmada · E2

La distribución académica utiliza una APK release firmada con una clave propia del Grupo 6. Se instala directamente en Android y puede adjuntarse a una release de GitHub; no requiere publicar en Google Play. Preparar el archivo local no equivale a haberlo publicado ni a integrar los cambios en main.

## Custodia y evidencia JKS

22/11/2026: se conserva la clave principal `g6-release.p12` y una copia `g6-release.jks` de la misma clave, alias y certificado, para identificar el formato mencionado en las pautas. Ambas están en `C:\Workspace\Projects\Academic\AppMovil\Docs\firma-release\acceso-privado\`, bajo custodia de Martín Caviedes; la contraseña permanece en esa carpeta privada. La conversión no genera otra identidad de firma ni modifica la APK existente.

[Resultado de verificación](evidencias/e2-1.1.0-firma-jks.json) · [Imagen de evidencia](evidencias/e2-1.1.0-firma-jks.png). Se comparó el certificado de ambos almacenes con el firmante de la APK usando keytool y apksigner. El certificado público permite verificar firma sin revelar la clave. Una solicitud académica de la llave se atiende únicamente por canal privado, nunca como adjunto público. El respaldo en un segundo almacenamiento conserva su seguimiento separado.

## Construcción y firma en Windows

Requisitos: PowerShell 7, JDK de Android Studio, SDK Android y Build Tools 36.0.0. Desde `android/`, con `JAVA_HOME` configurado:

```powershell
.\tools\build-release.ps1 `
  -ApiBaseUrl 'https://IP_ELASTICA/' `
  -SdkDirectory 'D:/DevelopmentData/AndroidSDK' `
  -SigningDirectory 'C:/Workspace/Projects/Academic/AppMovil/Docs/firma-release/acceso-privado' `
  -OutputDirectory 'C:/Workspace/Projects/Academic/AppMovil/Docs/entrega-final' `
  -InitializeSigningKey
```

Adaptar las rutas al equipo y usar la IP del despliegue vigente. `-InitializeSigningKey` autoriza la creación inicial si no existe la clave; las compilaciones siguientes reutilizan la misma clave, sin necesitar esa opción. El script no regenera una clave existente ni sustituye una contraseña ausente.

La clave PKCS#12 RSA de 3072 bits utiliza el alias `g6-release`. La clave y su contraseña quedan fuera del repositorio, en una carpeta con permisos limitados al usuario de Windows y SYSTEM. La contraseña se proporciona a keytool/apksigner mediante archivo, sin colocarla en los argumentos ni en el log. Conservar una copia privada y protegida de ambos archivos en otro almacenamiento: son necesarios para firmar futuras actualizaciones de esta aplicación. No adjuntarlos a GitHub ni al paquete de entrega.

El script compila `demoRelease`, comprueba HTTPS, alinea el ZIP antes de firmar y verifica después la firma y alineación. Genera la APK, `SHA256SUMS.txt`, metadatos y el certificado público del firmante. El certificado público identifica la firma y no contiene la clave privada.

## Identidad e instalación

Versión de este incremento: `1.1.0`, código `3`. La release utiliza `io.github.grupo6.capturaprospectos.demo`; la debug utiliza `io.github.grupo6.capturaprospectos.demo.debug`. Pueden coexistir. Son instalaciones independientes: la release no importa la base cifrada, las fotos ni la sesión de la debug. Los datos de la debug permanecen intactos; el acceso de la release utiliza las cuentas existentes en AWS. Las capturas almacenadas en el servidor no se descargan automáticamente a una instalación nueva.

Para actualizar una release instalada, conservar applicationId y clave de firma e incrementar versionCode. No desinstalar ni borrar datos como parte de una actualización normal. Una APK con la misma identidad y una clave distinta no constituye una actualización compatible.

## Comprobaciones de la entrega

1. Verificar SHA-256, firma, identidad, versión y ausencia de `debuggable=true` en el manifiesto final.
2. Comprobar cabeceras ELF y alineación ZIP de bibliotecas nativas. Esa inspección estática no acredita una prueba en un dispositivo de páginas de 16 KiB.
3. Instalar y abrir la APK firmada; completar una comprobación manual de acceso y captura contra AWS. Las pruebas de instrumentación y JaCoCo usan la variante debug en un emulador aislado: no se presentan como instrumentación de la release.
4. Adjuntar a la publicación únicamente la APK y archivos públicos de verificación/instrucciones. Las credenciales de administración/presentación permanecen en la carpeta privada del responsable.

La publicación en GitHub y la revisión del equipo son pasos separados de la firma local. La disponibilidad de la API depende de mantener EC2 encendida y su certificado renovado.

## Candidato generado · 22/11/2026

Archivo local fuera del repositorio: `Docs/entrega-final/Captura-Prospectos-G6-1.1.0-release.apk`, con URL HTTPS del despliegue AWS. SHA-256: `d24c92fc061fab9b9c9c21fa9ec1b895f422f4e7acb751f1f6b770b862114f1e`. Huella SHA-256 del certificado: `512b316af7b60701d4b22a2e1ab52b988afe1c453c3f6011d9a8ced2cb06106c`.

Compilación y `lintDemoRelease` aprobados: cero errores y 42 advertencias. Firma v2/v3 verificada por apksigner; ZIP alineado para páginas de 16 KiB. El manifiesto final confirma applicationId `io.github.grupo6.capturaprospectos.demo`, versión `1.1.0`/`3` y ausencia de la marca depurable. La URL de AWS se comprobó en el DEX del archivo final. Se inspeccionaron 20 bibliotecas nativas, sin fallos del criterio estático de cabeceras ELF de 64 bits; no es una ejecución en un teléfono de páginas de 16 KiB.

Instalación y arranque de la release comprobados en Redmi 9A el 22/11/2026 mediante ADB. Después de la cancelación inicial de MIUI, Martín dejó el teléfono desbloqueado y confirmó instalación vía USB activa; el segundo intento terminó con `Success`. La actividad inició con `Status: ok`; Android confirmó versión `1.1.0`/`3`, sin flag DEBUGGABLE. La release y la debug permanecen instaladas con identidades distintas, sin desinstalar ni borrar la debug.

Martín confirmó el 22/11/2026 que la comprobación guiada de esta release funciona sin problemas observados: acceso con cuenta de presentación, captura ficticia sincronizada y conservación de sesión/datos tras cerrar y reabrir. Es una confirmación manual global del recorrido solicitado, sin nuevos tests automáticos ni una repetición independiente del recorrido offline completo. Este último conserva su evidencia anterior en la APK debug de AWS. Canal final de distribución: [E2 1.1.0 final](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e2-1.1.0); el código fue integrado a main por PR #23 y los documentos por PR #25, bajo la autorización de cierre de Martín.

Referencias: [compilación por línea de comandos](https://developer.android.com/build/building-cmdline), [apksigner](https://developer.android.com/tools/apksigner), [zipalign](https://developer.android.com/tools/zipalign), [keytool](https://docs.oracle.com/en/java/javase/21/docs/specs/man/keytool.html).

## Respaldo temporal y publicación final

22/11/2026: por indicación de Martín se guardó una copia privada en `C:\Respaldo\Captura-Prospectos-G6-firma\`, con PKCS12, JKS y archivo de contraseña idénticos a los originales y acceso restringido. Es una copia en el mismo PC; el traslado al pendrive queda a cargo del custodio y no se considera todavía un respaldo externo completado.

La release final `e2-1.1.0` utiliza la misma APK que la candidata, sin recompilar. Incluye SRS 1.5 Word/Markdown y evidencia pública, sin claves privadas. La preparación de la demo comprobó Swagger/OpenAPI por HTTPS, readiness UP y versión 1.1.0 (3) no depurable instalada en Redmi 9A. No se repitió el recorrido funcional ni se cambiaron permisos del teléfono durante esta comprobación.
