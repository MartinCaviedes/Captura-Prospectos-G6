# Encabezado adaptable de eventos

El título «2. Evento para supervisar» y el botón «Nuevo evento» compartían una Row. En vertical, el título ocupaba el ancho y la acción terminaba con una letra por línea. FlowRow conserva ambos en una fila cuando caben y pasa el botón completo a la siguiente cuando falta espacio. Se conservan tema, semántica de encabezado, acción y bloqueo mientras se procesa una operación.

RF13/E1: sólo presentación. APK demo **1.0.1 (2)**, mismo identificador y certificado para actualización; sin dependencia, permiso, contrato, Room o regla nuevos. [Evidencia](evidencias/evento-boton-responsive-2026-10-19.json).

## Verificación

- Regresión anterior: la comprobación de una línea falla en la APK publicada 22d3e987.
- 191 JVM nuevas, cero fallos/errores/omitidas; compilación y Lint correctos, 30 avisos/0 errores.
- Siete Android dirigidas en Xiaomi 11T/API 34: cinco de disposición, lectura, clic y estado ocupado en anchos simulados de 320/360/800 dp, escalas 1/1.5/2 y claro/oscuro; dos de creación/edición con Room cifrado de prueba. Ejecución con apertura asistida MIUI. Base demo conservada por hashes durante instalación/suite; sin permisos o redes cambiados por el runner.
- La primera comprobación adicional hasVisualOverflow era inadecuada: el párrafo reservaba 792 px mientras la línea completa ocupaba 264,64 de los 265 px del Text. Se conservan los registros fallidos y se comprueban la línea visible, caracteres, elipsis y límites con tolerancia de redondeo, además de clic/no superposición. No se elimina una prueba de lectura para aprobar el arreglo.
- Comprobación física del operador: **NOT_REPORTED**. Sin respuesta atribuida aún; las medidas de ancho no se presentan como giro físico ni tablet real.

## Distribución

[APK y archivos de la versión](https://github.com/MartinCaviedes/Captura-Prospectos-G6/releases/tag/e1-demo-1.0.1). SHA-256 `fc9dadc967d5adfbc7025f828130ef79fb59e005f3e7b728a28ec06194371612`. La versión anterior conserva su APK, etiqueta y checksum; los enlaces de descarga del README apuntan a esta actualización. El cambio de versión evita confundir instaladores al evaluar la actualización.

No se repiten ni se atribuyen a esta APK las pruebas históricas de identidad/TalkBack/tablet/portal. Los pendientes generales siguen en [QA](ROUND4_QA.md). No se declara ausencia de todos los bugs ni cierre E1.
