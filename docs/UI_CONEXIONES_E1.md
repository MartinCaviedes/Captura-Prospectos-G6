# V3 · Conexiones · Android E1

Implementación candidata `feature/e1-ui-conexiones`, base `dad8a5b` de `feature/e1-ui-refinement`. Encargo visual aprobado por Martín el 11/10/2026. Los PR de las rondas y el refinamiento previo siguen siendo dependencias; este documento no acredita integración en develop ni aceptación de contratos.

[Diseño editable en Figma](https://www.figma.com/design/vVcMyy72PsACIh4stMKMKG?node-id=48-2595). Página `48:2594`, vistas principales `48:2599`–`48:2887`, tablet `48:4169`/`48:4223`. Se mantienen las iteraciones anteriores. Los números del prototipo son ilustrativos; en Android se consultan los repositorios existentes.

## Implementación y límites

| Vista | Adaptación nativa |
|---|---|
| Bienvenida y acceso | Identidad Conexiones, ilustración exportada, campos reales, contraseña visible/oculta y accesos de muestra desplegables que rellenan correo y contraseña sin iniciar sesión |
| Proveedores | Recursos Google/Microsoft exportados de Figma; botones deshabilitados con semántica de disponibilidad futura. Sin OAuth, recuperación o creación remota de cuentas |
| Evento | Tarjetas con nombre, horario/zona y existencia de referencia declarada del evento real; selección explícita con permisos y bloqueo de operaciones conservados |
| Inicio | Evento, estado real de conectividad, inscripciones activas/borradores y avance calculado por Room; sin ACK ni simulación de envío |
| Captura | Variante luminosa, iconos QR/GPS/búsqueda, calendario nativo y preferencias opcionales en menús; validación, prioridades, sedes y consentimiento originales |
| Registros y ficha | Búsqueda/filtros, consulta, edición y anulación existentes. Menú común deshabilitado durante detalle, lectura, edición, mutación o confirmación para impedir salidas que descarten datos |
| Resumen | Barras por estado e intereses por carrera con conteos reales y texto accesible. Hoy, todo el evento y rango explícito conservan las consultas existentes; Desde/Hasta incluyen calendario rápido sin aplicar automáticamente |
| Perfil | Identidad/contexto, avatar con iniciales, hoja de foto ilustrativa sin captura/subida y carga voluntaria confirmada de 24 ficticios por los repositorios existentes |
| Ventanas amplias | Menú lateral desde 840 dp, más ancho con letra aumentada; nombres/apellidos y correo/teléfono en columnas desde 600 dp disponibles. Desplazamiento y alturas adaptables |

Adaptaciones deliberadas: las siete barras diarias ficticias de Figma se sustituyen por estados reales porque DeviceMetrics no expone series diarias. No se inventan cifras ni se cambia ese contrato para dibujar una gráfica. La región y el catálogo enlazado de comunas, el GPS previo al evento y la foto persistente continúan como propuestas del prototipo, sin migración ni campos de entrada que aparenten guardarse. La comuna sigue siendo manual; el GPS funcional por captura mantiene confirmación/descarte y no infiere residencia, establecimiento o presencia en un evento.

## Tema y recursos

Tokens centrales: fondo `#0B1525`, superficie `#172B46`, dorado `#FFB800`, azul `#3B63F5`, papel `#EDF3FB` e información legible `#132B45`/`#52657A`. Texto secundario oscuro `#AABBD0`. Formulario claro: vínculos y acciones secundarias en tinta; acción principal dorada con texto azul oscuro, evitando amarillo sobre blanco.

Manrope 600/700/800 incorporada localmente desde [Google Fonts](https://github.com/google/fonts/tree/main/ofl/manrope); licencia OFL en `android/app/src/main/assets/licenses/Manrope-OFL.txt`. Roboto nativa para cuerpo/controles, tamaños en sp, sin descarga de fuentes en ejecución. Márgenes 24 dp, separaciones 8/12/16 dp y radios 12/20/28 dp.

El material se adapta con tarjetas Compose, gradiente translúcido, borde y elevación. No incorpora el componente iOS del kit ni aplica desenfoque al texto; conserva minSdk 26 y las dependencias fijadas. Los SVG exportados se rasterizaron completos a 4× sin modificar sus trazados; PNG original de Google conservado. Se deduplican únicamente archivos con SHA256 idéntico. [Procedencia por nodo/slot, geometría y consumidores](evidencias/conexiones-assets-2026-10-11.json).

No cambian dominio, Room v4, migraciones, SQLCipher/Keystore, catálogos, proveedor nativo, parser QR, Gradle, manifest ni transporte. Calendario y credenciales usan memoria volátil; no guardan PII en SavedState. Confirmar una fecha sólo rellena ISO y la validación de negocio existente sigue siendo obligatoria. Cancelar conserva el texto manual.

## Ajustes de contraste y calendario · 11/10

La prueba humana de la primera APK V3 confirmó acceso, evento, captura y giro sin recortes; señaló texto blanco heredado en la captura clara. Ahora una Surface local establece onSurface y el recurso de fondo claro. Cambiar MaterialTheme por sí solo no sustituía el color de contenido heredado de la pantalla oscura; [referencia Material 3 de Surface](https://android.googlesource.com/platform/frameworks/support/+/dcaa116fbfda77e64a319e1668056ce3b032469f/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Surface.kt).

Desde/Hasta comparten el calendario en memoria con Nacimiento. El contenido es desplazable para ventanas bajas; el mes y el modo de ingreso siguen fuera de SavedState. Su icono usa el color de contenido del tema oscuro del resumen. Abrir/cancelar no cambia el valor; confirmar rellena sólo el extremo elegido. Ninguna selección consulta Room hasta Aplicar rango; entrada manual y rechazo de rangos inválidos se conservan. Años del calendario: nacimiento 1900–año actual; rango 1900–2100, sin limitar la validación de las fechas ingresadas manualmente.

## Verificación

Resultados identificados por versión/APK en [ANDROID_BASELINE](ANDROID_BASELINE.md) y su evidencia de Conexiones. Las pruebas Compose usan bases `test-*` aisladas; la instalación `-r -t` conserva los datos de la app y no concede permisos mediante ADB. La simulación de ancho por Density acredita composición, no un tablet físico.

Prueba manual de esta APK:

1. Comenzar → Cuentas de muestra → Captador A. Debe rellenar ambos campos, sin entrar; borrar o cambiar contraseña debe producir rechazo. Credenciales ficticias: `captador.a@g6.example` / `DemoG6!2026`.
2. Seleccionar evento → Inicio → Registros → Resumen → Perfil. Comprobar etiquetas, cifras y texto sin recortes. La carga de datos ficticios es opcional y requiere su confirmación.
3. Nueva captura: escribir sólo datos ficticios, usar el calendario y cancelar otra selección; comprobar correo con teclado apropiado, giro, desplazamiento y descarte sin guardar. Menús opcionales empiezan sin respuesta.
4. QR: usar únicamente el fixture G6DEMO:1 documentado; confirma sólo el RUT y exige autorización para reemplazar uno ya escrito. GPS: solicita permiso en contexto, muestra precisión candidata y permite confirmar/descartar, sin rellenar residencia.

5. Resumen → Elegir período: abrir calendario Desde/Hasta, elegir una fecha y confirmar; cancelar otra selección no cambia el campo. Aplicar rango realiza el cálculo y conserva los controles de orden/formato.

TalkBack completo continúa pendiente a petición del usuario. Cámara/enfoque y GPS físicos de versiones anteriores son antecedentes; permisos adversos, portal cautivo, tablet real, revisión cruzada y ensayo P3 requieren sus comprobaciones específicas. No se cierran RF, rondas ni tarjetas por esta adaptación visual.
