# R4 · QA y preparación de E1

Corte 14/10/2026 sobre develop `0c64533`. Este incremento verifica y documenta el producto local integrado; no agrega funcionalidades ni cierra RF. Fuente técnica: [ENTREGAS](ENTREGAS.md), [SRS](SRS.md), [SECURITY](SECURITY.md) y [evidencia nueva](evidencias/r4-clon-limpio-2026-10-14.json). La defensa y coordinación del equipo se preparan en el repositorio privado.

## Resultado autónomo de este corte

Clon nuevo obtenido del remoto develop, sin local.properties ni archivos privados copiados. Build en Windows con SDK 37 y el JDK fijado por el proyecto; se usaron las dependencias y cachés disponibles del equipo. No es evidencia de un equipo nuevo sin SDK ni de otro sistema operativo.

191 pruebas JVM aprobadas, cero fallos/errores/omitidas; una corresponde al test aritmético de plantilla y las restantes a las suites del proyecto. Build debug, instrumental y release unsigned correctos; 130 tareas ejecutadas, 6 min 6 s. Lint debug: 27 avisos/0 errores; avisos por ID en la evidencia. No se actualizan dependencias ni se eliminan reglas para modificar ese resultado.

Debug e instrumental verifican firma. La release se genera sin firma y no es instalable como entrega firmada; no se creó una llave. Las tres pasan zipalign con comprobación ZIP de 16 KB. Esto no verifica la alineación ELF de cada biblioteca ni ejecución en un dispositivo de páginas de 16 KB. Release no es depurable ni contiene PreviewActivity; optimización/R8 siguen desactivados en la configuración existente y no se acredita ofuscación.

Los manifiestos efectivos mantienen cámara, ubicación y estado de red; INTERNET está ausente. allowBackup=false y reglas de exclusión de backup/transferencia configuradas. La aplicación sólo exporta su launcher; en release el receptor de ProfileInstaller exportado exige android.permission.DUMP. El análisis limitado de patrones no encontró claves/secretos literales de infraestructura ni archivos privados prohibidos en seguimiento. No es una auditoría exhaustiva; backup/transferencia efectivos, logs en recorridos reales y permisos se comprueban aparte.

Los tres scripts SQLite del host pasaron, incluido dataset estructural de 400. No prueban SQLCipher, Keystore, latencia Android ni nuevos resultados del Xiaomi. Las pruebas instrumentadas/humanas del 11/10 siguen históricas. En este corte no se detectó un teléfono autorizado al preparar la ejecución; no se ejecutó una suite Android.

## Orden de comprobaciones restantes

| Bloque | Comprobación | Estado de este corte |
|---|---|---|
| Construcción | Clon remoto limpio, JVM, debug/instrumental y Lint | Comprobado en Windows |
| Empaquetado | Release compilable, firma debug, manifiestos y alineación ZIP | Comprobado; release firmada/instalación pendientes |
| Reglas y persistencia | Consentimiento, CRUD, duplicados, permisos de perfil, cifrado, migraciones y reapertura | JVM/SQLite nuevas; suite Android final pendiente |
| Cámara/GPS | Permisos adversos, lectura segura, cancelar, candidatos y manual | Recorridos físicos pendientes en APK identificada |
| Red | Pérdida/retorno, red sin internet y portal cautivo | Pendiente de red real preparada |
| Adaptación | Texto grande, ambas orientaciones, tablet y TalkBack | Antecedentes físicos por versión; comprobación completa pendiente |
| Entrega | Fuente/artefacto/commit identificados, revisión y canal de firma | Candidato, no cierre de entrega |

La revisión de código reportada al integrar no sustituye la aceptación específica de RECORD_CONTRACTS, accesibilidad completa o ensayo individual. El requisito completo de sincronización sigue en E2.

## Pruebas manuales breves

Usar sólo cuentas/fixtures ficticios. Identificar antes la APK y registrar modelo/API, fecha, pasos, resultado esperado/observado y mensajes exactos. No borrar datos para reiniciar el recorrido. Si se cambian permisos o red, empezar sin una captura abierta y restaurar la configuración después. No alterar una red compartida para provocar fallos.

### M01 · cámara denegada y recuperación

1. Sin formulario pendiente, entrar a Ajustes del teléfono → aplicaciones → Captura de Prospectos → permisos; denegar Cámara. No usar Borrar datos.
2. Abrir una captura nueva y solicitar QR; denegar el permiso si aparece. Esperado: explicación y «Continuar manualmente», sin cierre de la app.
3. Continuar manualmente; el RUT permanece editable. Volver a solicitar: cuando el sistema no permita repetir, «Abrir ajustes» debe abrir los ajustes de esta app.
4. Conceder Cámara desde ajustes y volver. Abrir QR, cancelar y comprobar que no se aplicó ningún dato. Descartar sin guardar.

### M02 · GPS denegado, apagado y aproximado

1. Denegar Ubicación desde ajustes, sin formulario pendiente; volver y solicitar «Adquirir GPS». Esperado: permiso/explicación y alternativa manual, sin coordenadas inventadas.
2. Conceder permiso, apagar Ubicación del teléfono y solicitar de nuevo. Debe mostrar indisponibilidad, sin guardar una posición anterior.
3. Restaurar Ubicación y probar permiso aproximado si Android lo ofrece. Una candidata debe mostrar fuente/precisión y requerir confirmación; si no es válida o expira el plazo, informar y conservar el formulario.
4. Conceder precisión para la comprobación normal, si el usuario lo desea. Descartar candidata no debe aplicar posición. Restaurar ajustes y descartar el formulario sin guardar. No prometer adquisición garantizada en interiores.

### M03 · QR ficticio y desconocido

Usar el formato documentado en [ROUND2_DEVICE_RESOURCES](ROUND2_DEVICE_RESOURCES.md), nunca una cédula real. Enfocar un QR ficticio válido: revisar candidata y confirmar sólo RUT; nombres/apellidos siguen manuales. Probar un QR que contenga un texto ficticio sin el formato admitido: debe rechazarlo o informar, sin abrir sitios ni ejecutar el contenido. Cancelar no debe modificar campos. Repetir con distinta distancia/enfoque; descartar sin guardar. No afirmar soporte de cédula real.

### M04 · conectividad y formulario

Con una red propia de pruebas, abrir captura y escribir «Prueba de red» en nombres. Desactivar Wi-Fi y datos móviles manualmente; comprobar feedback de conexión y conservación del texto. Restaurar la red y comprobar actualización del estado sin mensajes de sincronización exitosa. Descartar sin guardar. Para verificar Room tras reinicio, usar exclusivamente un registro ficticio consentido y guardado explícitamente, anotar su ID y comprobarlo en bandeja después de cerrar/abrir la app; no borrar la base.

### M05 · red sin internet / portal cautivo

Requiere una red de prueba que realmente carezca de internet o un portal controlado; airplane mode sólo acredita ausencia de red. Evitar redes públicas desconocidas y nunca enviar datos reales. Conectarse sin completar el portal y sin red móvil alternativa; la app debe distinguir red detectada de internet validado, conservando captura/Room. Completar el portal sólo si es de confianza y comprobar el cambio. No se consulta un backend en E1. Registrar si la red/Android tardó en validar; no simular el portal con un doble y llamarlo prueba real.

### M06 · adaptación y accesibilidad

Probar bienvenida, acceso, evento, Inicio, Registros, Resumen, Perfil y captura con texto al 150 %, claro/oscuro y ambas orientaciones. Abrir calendarios antes de girar y comprobar diálogo/candidata/rango. Revisar desplazamiento y acceso al consentimiento y descarte. Repetir en tablet físico cuando esté disponible; un ancho simulado no acredita ese dispositivo.

TalkBack sigue pendiente hasta acordar su sesión. Al probarlo, conocer primero cómo desactivarlo: un dedo cambia foco, doble toque activa y dos dedos desplazan. Revisar nombres, estado seleccionado, errores y orden de lectura; recorrer campos, consentimiento y descarte sin guardar. Una inspección de contentDescription o un test Compose no equivale a experiencia TalkBack.

## Reproducción de controles automáticos

Desde android/, con ANDROID_HOME/SDK y JDK compatibles, sin copiar local.properties ajeno:

```powershell
.\gradlew.bat :app:testDemoDebugUnitTest :app:assembleDemoDebug :app:assembleDemoDebugAndroidTest :app:lintDemoDebug :app:assembleDemoRelease --console=plain '-Dorg.gradle.jvmargs=-Xmx4g' --max-workers=2
python tools/verify_migration_sql.py
python tools/verify_context_migration_sql.py
python tools/verify_dataset_400_sql.py
```

Los controles de firma/alineación usan apksigner y zipalign del SDK. apksigner verify debe aprobar debug; el rechazo de release-unsigned sólo acredita que no está firmada. No interpretar build exitoso como firma o instalación comprobada. Los paths y hashes de salidas están en la evidencia.

Antes de instrumentar, revisar que cada test use bases/alias test-*; no instalar con -g, revocar permisos o cambiar redes automáticamente por comodidad. La guía Android explica las restricciones de MIUI y la asistencia de apertura. Una ejecución omitida no cuenta como aprobada; el test de transición Wi-Fi necesita preparación específica.

## Registro del resultado humano

```text
Caso / APK SHA / dispositivo y API / fecha:
Estado inicial y configuración:
Pasos realizados:
Resultado observado y mensaje exacto:
PASS / FAIL / no realizado:
Datos ficticios guardados explícitamente (si aplica) y configuración restaurada:
```

Registrar evidencia nueva por APK; conservar las anteriores como antecedentes. No sumar ejecuciones separadas ni marcar todos los casos PASS por aprobar uno. Las decisiones de firma/custodia, aceptación contractual, AVA y defensa se coordinan en el marco privado.
