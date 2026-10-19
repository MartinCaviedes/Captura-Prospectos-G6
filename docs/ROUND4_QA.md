# R4 · QA y preparación de E1

## R4 · acceso y giro corregido y comprobado · 19/10/2026

El operador confirma bienvenida/acceso/evento legibles y alcanzables con XXL, completando el recorrido visual del teléfono en APK 733104d7. Detecta un fallo funcional independiente: el giro borra correo/contraseña rellenados por cuenta de muestra. DemoAccessScreen los mantenía con remember, perdido al recrear Activity. DemoAccessRoute ahora observa DemoAccessViewModel; éste retiene el borrador sólo en memoria, consume la contraseña al enviar y limpia credenciales al logout/onCleared. No SavedState, disco, Room, nuevo proveedor ni cambio de autenticación E1/E2. La contraseña vuelve a estar oculta tras recreación.

Regresión antes: falla al comprobar correo tras recreación, APK de control 185dc7f8 (ruta añadida, comportamiento remember aún intacto). Después: **191 JVM, 14 Android dirigidas aprobadas y Lint 27 avisos/0 errores**; apertura asistida ADB en MIUI, bases demo conservadas por hashes y Room sólo test-*. Las tres nuevas cubren cuenta de muestra sin ingreso automático, edición manual/recreación/consumo de contraseña y logout/nueva Activity vacía. Incluye UI de acceso, perfiles/fixtures Room, calendarios y regresión de conectividad. ActivityScenario.recreate no es giro físico ni muerte de proceso.

APK debug candidata `8a02596d…` instalada; debug/instrumental firmadas y ZIP comprobados. Release compilada unsigned, sin instalar. Bibliotecas nativas debug idénticas a 733104d7 por hash; no ejecución 16 KB. [Evidencia, hashes y límites](evidencias/r4-acceso-giro-2026-10-19.json), [fallo anterior](evidencias/android-acceso-before-2026-10-19.txt), [corrida corregida](evidencias/android-acceso-after-2026-10-19.txt). **Giro físico comprobado por reporte del operador en 8a02596d:** los campos se conservan sin problemas. Es conservación en memoria, no guardado en disco; no se reporta por separado cada paso de ingreso/evento. Arreglo preparado para publicación normal en ramas R4/PR #10, sin merge ni aprobación de terceros atribuida. TalkBack aplazado, tablet, M05 sin entorno y aceptación/entrega siguen pendientes; no cierre R4.

**Visual M06 · 19/10/2026:** el operador completa el recorrido solicitado en Xiaomi con fuente **XXL**, sin botones/textos cortados, ilegibles ni inaccesibles: Inicio, Registros, Resumen, Perfil y captura, incluyendo las variaciones de tema/orientación y calendario indicadas en la guía. Resultado global reportado, sin desglose por control; XXL no equivale a 150 % medido. APK `733104d7…`; [reporte y límites](evidencias/r4-nativos-manual-2026-10-19.json). Quedan bienvenida/acceso/evento con XXL, TalkBack aplazado y tablet físico. Sin nueva APK ni suites; no cierre M06/R4.

**Seguimiento M01 · 19/10/2026:** el operador confirma que «Abrir ajustes», tras denegar Cámara y solicitar QR, abre la ficha correcta de la app. Concede Cámara y vuelve sin reinicio ni descarte en ese intento, APK `733104d7…`. Queda comprobada la acción pendiente de M01 en su recorrido guiado; no es garantía de conservar memoria si Android termina el proceso. [Evidencia por evento](evidencias/r4-nativos-manual-2026-10-19.json). Sin nueva APK ni suites. M05 sigue sin entorno; visual final, TalkBack aplazado y tablet conservan su estado. Los apartados anteriores conservan su corte histórico.

## Corrección de conectividad comprobada · 19/10/2026

En APK anterior `b0294e11…`, el operador observó Inicio aún «Conexión a internet verificada» tras apagar redes; sólo al cambiar de pestaña aparecía offline. Al recuperar conexión se actualizaba automáticamente y el formulario conservaba sus campos. M04 se registra con fallo en pérdida de red, sin atribuir PASS por la conservación del formulario. M03 sí fue reportado: QR físico válido confirma sólo RUT; desconocido se rechaza y permite manual conservando datos.

Se detectó que MainHubRoute creaba un Flow nuevo por recomposición, reiniciando la escucha. Una regresión Compose falla antes (`expected 1 / actual 2` suscripciones) y pasa después de retener el Flow con `remember(connectivityObserver)`. También comprueba cambio de proveedor, cancelación de la escucha anterior y descarte de sus emisiones tardías. No se modifica AndroidConnectivityObserver, permisos, manifiesto, datos, Gradle ni reglas de negocio. No hay sondeo ni permiso INTERNET añadido.

Evidencia nueva: 191 JVM aprobadas; Lint 27 avisos/0 errores; ocho Android dirigidas aprobadas en una ejecución (dos nuevas de conectividad con proveedor controlado, tres resumen/Room, dos calendarios tras recreación y una de liberación del callback real). Xiaomi/API 34, apertura asistida ADB en MIUI, archivos demo intactos por hashes. [Resultado y hashes](evidencias/r4-conectividad-fix-2026-10-19.json), [fallo esperado anterior](evidencias/android-red-before-2026-10-19.txt), [ejecución corregida](evidencias/android-red-after-2026-10-19.txt). Las redes/permisos no fueron modificados por el runner. Debug e instrumental construidas; release unsigned actualizada, sin firma ni instalación release; ZIP comprobado y las 20 bibliotecas nativas por variante tienen los mismos hashes del corte ELF anterior.

APK debug `733104d7…`, instalada conservando datos. **Repetición física del aviso M04 aprobada por reporte humano:** pérdida y recuperación se reflejan sin cambiar pestañas; el operador estima hasta un segundo en ambos sentidos. Es una observación informal, sin cronómetro ni muestra para un benchmark. La conservación de campos fue comprobada antes en b0294e11; los reportes cámara/GPS/QR también mantienen esa APK. No se suman como nuevos en 733104d7. Corrección preparada para revisión en rama R4/PR #10, sin merge ni cierre E1.

M05 no ejecutado: el operador informa que no dispone de Wi-Fi de pruebas sin internet ni portal controlado. La transición offline/validada comprobada no sustituye este caso. Seguimiento posterior: M01 Abrir ajustes confirmado en 733104d7; continúan visual final, TalkBack aplazado/tablet y decisiones de entrega.

## Seguimiento humano de cámara y ubicación · 19/10/2026

Denegación/manual y recuperación/apertura/cierre de cámara conservan campos según el operador. M02: permiso denegado conserva texto sin coordenadas; servicio apagado informa sin aplicar posición; aproximada finaliza por timeout sin perder texto ni aplicar ubicación; precisa ofrece candidata, descartar no la aplica y una nueva adquisición sólo se aplica al confirmar. No se reportaron precisión numérica ni textos literales completos. [Evidencia humana y límites](evidencias/r4-nativos-manual-2026-10-19.json).

Los cuatro escenarios GPS guiados quedan comprobados por reporte humano en esta APK, sin atribuir una adquisición exitosa al modo aproximado ni lectura exclusiva de satélites. Seguimiento posterior: M03 fue reportado con QR físico válido/desconocido. Seguimiento posterior: M04 corregido ya fue comprobado físicamente en 733104d7. Queda Abrir ajustes desde denegación permanente y demás controles pendientes. No cambió código ni se ejecutaron suites/build/instalación nuevos; los resultados automáticos del 18/10 se conservan separados. La limitación de memoria ante muerte del proceso sigue vigente.

## Verificación Android · 18/10/2026

Se retoma R4 sobre el mismo código funcional integrado `0c64533`, candidato documental `855d6ef`. APK debug `b0294e11…`, Xiaomi 11T / Android 14 / API 34, páginas de 4096 bytes. Instalación conservadora con `-r -t`, sin borrar datos ni conceder permisos por ADB. Los hashes de los archivos de la base demo y clave envuelta permanecieron iguales durante la instalación y ambas suites.

**Evidencia nueva:** 52 pruebas de datos/proveedores y 31 de UI aprobadas en dos ejecuciones, sin fallos ni omisiones dentro de la selección. [Resultados, clases y límites](evidencias/r4-android-2026-10-18.json); [salida de datos](evidencias/android-r4-data-2026-10-18.txt) y [salida UI](evidencias/android-r4-ui-2026-10-18.txt). Room/SQLCipher/Keystore, migraciones, no consentimiento, duplicados, rollback, aislamiento de perfiles, edición/anulación y reapertura se ejercitan con bases `test-*`. Las actividades UI requirieron apertura asistida ADB en MIUI; incluyen calendarios tras recreación, acceso, eventos, bandeja/detalle, resumen y texto al 150 % con ancho simulado.

Dataset cifrado: 400 capturas, 600 intereses, 320 claves activas y 440 auditorías, sin FK rotas; tras reabrir conserva filas y alcance. Alta p95 **42,98 ms** (400 muestras); escrituras totales p95 **42,44 ms** (440 muestras), bajo meta interna PT de 150 ms. Medición debug de llamada completa al repositorio; excluye inicialización, cambio de contexto, GPS y red, no es tiempo total de captura ni garantía de rendimiento en otros equipos.

Inspección acotada de logs del UID de la app durante estas pruebas: sin coincidencias de los patrones de correo/RUT/nombres fixture/contraseña o etiquetas de clave revisados, ni FATAL EXCEPTION. No es auditoría exhaustiva ni prueba de logs de todos los recorridos humanos. No se inspeccionaron logs globales del teléfono.

**Empaquetado:** comprobador portátil de cabeceras ELF con ocho pruebas host. Debug y release unsigned pasan LOAD/RELRO para las diez bibliotecas de 64 bits de cada APK. Se aplica la excepción documentada de RELRO en LOAD separado; el módulo por sí solo produciría falsos rechazos. `barhopper` de los ABI de 32 bits conserva LOAD de 4 KB y se informa aparte: el criterio Android de páginas de 16 KB usado aquí corresponde a 64 bits. [Referencia Android](https://developer.android.com/guide/practices/page-sizes). Es comprobación estática, complementa ZIP/firmas del 14/10 y no acredita ejecución 16 KB. No hay AVD/imagen 16 KB configurado y el Xiaomi usa 4 KB. Release sigue sin firma y no instalada.

Los 191 JVM, Lint 27 avisos/0 errores y build desde clon remoto limpio pertenecen al 14/10, [evidencia histórica](evidencias/r4-clon-limpio-2026-10-14.json). No se repitieron porque el código de la app no cambió. El comprobador Python no modifica la APK.

## Qué queda de R4

| Bloque | Comprobado | Pendiente concreto |
|---|---|---|
| Reglas/persistencia | JVM históricas + regresión Android nueva y 400 cifradas | Aceptación humana específica de RECORD_CONTRACTS y recorrido final de operador |
| Nativos/red | Cámara manual/recuperación y M02/M03 en b0294e11; M01 Abrir ajustes y aviso M04 sin navegar en 733104d7 comprobados por operador | M05 sin red controlada disponible |
| Adaptación/accesibilidad | Recorrido visual del teléfono en XXL reportado sin recortes/inaccesibles, incluida bienvenida/acceso/evento (733104d7); giro de login corregido, recreación automática y conservación física de campos aprobadas | TalkBack aplazado; tablet físico |
| Seguridad/empaquetado | Cifrado/clave inaccesible sin reset, logs acotados, manifiestos/ZIP/ELF | Backup/transferencia efectivos en entorno controlado; runtime 16 KB; aclaración/firma release |
| Entrega | Candidato fuente y APK identificados, guías y evidencia | Revisión de PR, corte/tag final, AVA y explicación individual |

El test opt-in de transición Wi-Fi no se invocó: necesita un cambio de red preparado con el usuario, sin red móvil alternativa. El proveedor GPS sólo garantiza resultado tipado; la decodificación de bitmap no acredita enfoque de cámara. Los tests UI emplean ComponentActivity de prueba, no un recorrido humano completo de MainActivity. No sumar anchos simulados como tablet físico ni semántica como TalkBack. La revisión externa reportada al integrar R0–R3 no sustituye aceptación contractual. La sincronización real continúa en E2.

## Pruebas manuales breves

Usar sólo cuentas/fixtures ficticios. Identificar antes la APK y registrar modelo/API, fecha, pasos, resultado esperado/observado y mensajes exactos. No borrar datos para reiniciar el recorrido. Si se cambian permisos o red, empezar sin una captura abierta y restaurar la configuración después. No alterar una red compartida para provocar fallos.

### Resultado parcial M01 · 19/10/2026

El operador reporta que denegar Cámara y elegir «Continuar manualmente» conserva nombres/apellidos. Al ir a Ajustes, cambiar el permiso y volver, aparece bienvenida y se pierde el formulario sin guardar. Consulta ADB de sólo lectura: cuatro salidas recientes del paquete con `reason=8 (PERMISSION CHANGE)`; APK instalada coincide con `b0294e11…`. El historial confirma muertes por cambio de permisos, sin atribuir cada salida a una pulsación concreta de conceder/revocar. [Evidencia y límites](evidencias/r4-permisos-camara-2026-10-19.json).

El formulario y acceso demo sólo viven en memoria; sobreviven a recreación de Activity con su dueño, pero no a muerte del proceso. No es una recuperación garantizada al volver de Ajustes. No se cambia a SavedState ni guardado automático sin consentimiento. Para estas pruebas, ajustar permisos sin una captura pendiente y luego abrir una nueva; si ya existe consentimiento válido, un borrador sólo se conserva tras guardado explícito correcto. Se corrige esta omisión de la guía anterior, sin cambiar código de la app. Seguimiento posterior: el operador confirma apertura/cierre de cámara con campos intactos tras recuperar permiso. Queda por confirmar explícitamente la acción «Abrir ajustes» desde denegación permanente; M01 completo no se cierra por inferencia. [Consolidación M01/M02](evidencias/r4-nativos-manual-2026-10-19.json).

### M01 · cámara denegada y recuperación

1. Sin formulario pendiente, entrar a Ajustes del teléfono → aplicaciones → Captura de Prospectos → permisos; denegar Cámara. No usar Borrar datos.
2. Abrir una captura nueva y solicitar QR; denegar el permiso si aparece. Esperado: explicación y «Continuar manualmente», sin cierre de la app.
3. Continuar manualmente; el RUT permanece editable. Volver a solicitar: cuando el sistema no permita repetir, «Abrir ajustes» debe abrir los ajustes de esta app.
4. Terminar o descartar la captura de prueba antes de cambiar el permiso en Ajustes. Conceder Cámara y volver; si Android terminó el proceso, reingresar y abrir una captura nueva. Abrir QR, cancelar y comprobar que no se aplicó ningún dato. Descartar sin guardar. La recuperación de un formulario no guardado tras muerte del proceso no forma parte del contrato volátil.

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

## Reproducir la comprobación ELF

Desde `android/`, con Python 3 (biblioteca estándar):

```powershell
python tools/test_verify_apk_elf.py
python tools/verify_apk_elf.py app/build/outputs/apk/demo/debug/app-demo-debug.apk
python tools/verify_apk_elf.py app/build/outputs/apk/demo/release/app-demo-release-unsigned.apk
```

Salida JSON con hash de APK y cabeceras por biblioteca; código 0 si las bibliotecas de 64 bits pasan, 1 si alguna no pasa y 2 ante un archivo inválido. Una APK sin bibliotecas nativas devuelve lista vacía: no hay ELF que comprobar, no acredita instalación. Usar además `zipalign -c -P 16 4 APK` y el dispositivo adecuado para ejecución. No modificar `.so` de terceros ni excluir ABI para forzar un resultado.
