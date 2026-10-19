# Sistema de diseño y flujos

Bienvenida · 19/10: la ilustración abstracta se sustituye por tres conceptos reales de captura con texto nativo (contacto, intereses y consentimiento), adaptable al tema/letra. No son controles ni campos simulados. [Estado y evidencia](UI_IDENTIDAD_TEMA_DEMO.md).

Iteración autorizada 19/10/2026 · identidad, apariencia y muestra mixta E1. Logo oficial Duoc UC desde su sitio, separado del símbolo propio de captura e icono adaptativo. Perfil permite claro/oscuro, preferencia booleana sin PII; fondos, controles, formulario y barras siguen el modo elegido. Carga voluntaria de 60 ficticios: conserva 24 sin contexto y añade 18 listas/12 borradores/6 anuladas, con ubicaciones simuladas declaradas y sin duplicar/reemplazar trabajo manual. 191 JVM/21 Android dirigidas aprobadas; Lint 29 avisos/0 errores. APK 1f5c8b2b: el operador confirma cambio de tema en todas las pestañas sin texto invisible, carga ficticia y visualización correctas. Reporte global, sin conteos numéricos ni validación independiente detallada de cada paso de la guía. Publicación en ramas propias para revisión, sin merge. Rama feature/e1-identidad-tema-demo depende del PR R4 #10; sin integración, nueva autenticación ni cambio de esquema/reglas. [Detalle y límites](UI_IDENTIDAD_TEMA_DEMO.md).

Ajuste V3 tras prueba humana, 11/10: el formulario claro establece su color de contenido en Surface; no hereda blanco del shell oscuro. Nacimiento y filtros Desde/Hasta comparten calendario en memoria; seleccionar no guarda ni aplica el rango automáticamente. [Resultado y evidencia](UI_CONEXIONES_E1.md).

**Tema vigente del candidato V3 · 11/10/2026:** [Conexiones](UI_CONEXIONES_E1.md) sustituye la adaptación inicial de tema/fuentes descrita abajo, conservando las reglas del formulario y E1. Fondo azul #0B1525, superficie #172B46, acción dorada #FFB800, formulario claro #EDF3FB y texto #132B45/#52657A. Manrope local en títulos y Roboto nativa en cuerpo. En el formulario, acciones secundarias en tinta y principales doradas con texto azul oscuro. Las tablas de paleta anteriores son antecedentes; no alternar ambos sistemas entre pantallas nuevas.

UI F2027 del 06/10: diferenciar campos obligatorios/condicionales/opcionales; ninguna preferencia preseleccionada. Sugerencia de establecimiento desde evento identificado necesita confirmación y permite editar; no sobrescribe datos al cambiar contexto. Mostrar jornada, nivel y motivación como opcionales, sin campo libre para motivación. Conservar ingreso manual de nacimiento, sin prometer que el lector lo extrae.

Acuerdo F01: las opciones pueden ordenarse alfabéticamente; el resumen de hasta tres seleccionadas se ordena por prioridad e incluye controles accesibles para reordenar. F03: mostrar por separado contexto del captador y preferencia de sede; múltiples sedes opcionales sin máximo adicional, sin preseleccionar la del evento. F02 aclarado el 06/10.

Revisión 05/10: antes de implementar captura/preferencias, revisar F01–F03 en FORMULARIO_ADMISION_2027. Nombre/apellidos, actividad, nuevas variables, ranking y sede de interés tienen reglas acordadas en la revisión del 06/10; no inventar obligatoriedad ni confundir jornada, modalidad y sede. La base visual y recursos E1 no cambian.

Actualizado 08/10/2026 con referencias del kit digital Duoc UC. Guía común para que cada integrante amplíe la app sin crear estilos distintos. La adaptación inicial del tema y los componentes de contexto está implementada en la rama E1-BASE-03-A1; conexión a datos y revisión visual/humana pendientes. Ver [CONTEXT_UI](CONTEXT_UI.md). No existe un mockup aprobado por el cliente.

## Principios

Material Design 3, textos en español, jerarquía clara y contraste comprobado. Utilizar tokens semánticos de tema para color, tipografía, espaciado y formas; no hexadecimales repetidos en cada pantalla. El kit público respalda las referencias visuales siguientes; no acredita aprobación institucional de esta app ni autoriza por sí solo incorporar cualquier logo o recurso.

Escala inicial de espaciado: 4/8/12/16/24/32 dp. Controles táctiles >=48 dp, etiqueta siempre visible, soporte de texto ampliado y TalkBack. Ningún estado se comunica sólo mediante color. La guía de [accesibilidad de Compose](https://developer.android.com/codelabs/basic-android-kotlin-compose-test-accessibility) respalda el tamaño de objetivos táctiles; estos tokens de espaciado son propuesta del equipo técnico.

## Referencia Duoc UC y adaptación Android

Consulta del 08/10/2026. El [kit web](https://www.duoc.cl/kit-digital/web/) utiliza Bootstrap; en esta app se adapta a Compose/Material 3. No incorporar HTML, CSS, JavaScript o WebView para reproducirlo, ni convertir mecánicamente sus medidas web a dp/sp.

La fuente de [tipografía y colores](https://www.duoc.cl/kit-digital/web/tipografia-colores/) publica negro `#1A1A1A`, blanco `#FFFFFF` y amarillo `#FFB800` como paleta primaria; gris `#666666`, negro `#000000` y gris claro `#EEEEEE` como secundaria, y las familias Merriweather y Lato. Las asignaciones Android siguientes son decisiones de adaptación del proyecto, no un tema móvil oficial.

| Rol inicial del tema | Claro | Oscuro |
|---|---|---|
| primary / onPrimary | amarillo / negro #1A1A1A | amarillo / negro #1A1A1A |
| background / onBackground | blanco / negro #1A1A1A | negro #1A1A1A / blanco |
| surface / onSurface | blanco / negro #1A1A1A | negro #1A1A1A / blanco |
| Texto secundario sobre el fondo anterior | gris #666666 | gris claro #EEEEEE |

Esta tabla fija roles principales, no todos los roles de ColorScheme. Completar contenedores, bordes, selección, foco y estados en el tema compartido al implementar la primera pantalla, comprobando sus pares de contraste. Evitar el reemplazo automático por colores dinámicos del dispositivo en el tema de referencia. Los colores semánticos de error/advertencia/éxito se definen por legibilidad y significado; no atribuirlos al kit sin fuente ni usar amarillo como prueba de guardado o sincronización.

Comprobación numérica sRGB, colores opacos: negro #1A1A1A sobre amarillo ≈10,04:1; blanco sobre amarillo ≈1,73:1; gris #666666 sobre blanco ≈5,74:1 y sobre negro #1A1A1A ≈3,03:1. Por tanto, no usar blanco sobre amarillo ni ese gris sobre negro para texto normal. Revisar también el límite visible de controles amarillos sobre blanco; añadir borde oscuro cuando sea necesario. Son cálculos de pares de colores, no una auditoría de accesibilidad de pantallas todavía inexistentes.

Tipografía propuesta: Lato para cuerpo/controles y Merriweather para títulos destacados puntuales. Hasta incorporar archivos y licencias verificadas, conservar las fuentes nativas del tema; no descargar fuentes en tiempo de ejecución ni prometer fidelidad ya implementada. Mantener tamaños escalables en sp y roles Material 3; verificar tildes, ñ y ampliación de texto. No inferir colores de carreras o escuelas a partir del catálogo sintético.

| Fuente visual consultada | Aplicación al proyecto |
|---|---|
| [Cajas y bloques](https://www.duoc.cl/kit-digital/web/cajas-bloques/) | Jerarquía de tarjetas para evento, contexto y resumen; controles nativos y texto legible |
| [Tablas y listas](https://www.duoc.cl/kit-digital/web/tablas-listas/) | Filas con etiquetas claras; preferir listas adaptables en teléfono frente a tablas anchas |
| [Acordeones](https://www.duoc.cl/kit-digital/web/acordeones/) | Secciones desplegables sólo donde ayuden; anunciar expansión y abrir la sección que contiene un error |
| [Mensajes y notificaciones](https://www.duoc.cl/kit-digital/web/mensajes-notificaciones/) | Mensaje, icono y acción adecuados; conservar las distinciones de estado de esta guía |
| [Botones e iconos](https://www.duoc.cl/kit-digital/web/botones-iconos/) | Acción principal, secundaria y destructiva diferenciadas; el kit referencia Feather, pero se usarán vectores/iconos nativos sin importar su runtime web |
| [Widgets de contenidos](https://www.duoc.cl/kit-digital/web/widget-contenidos/) | Referencia de jerarquía de contenido; sus tarjetas de noticias no agregan un feed ni nuevos RF |

No ocultar consentimiento o información imprescindible dentro de un acordeón sin acceso claro. No copiar contenido comercial, catálogo, imágenes o enlaces de los ejemplos como datos funcionales. La primera revisión visual debe incluir tema claro/oscuro, texto ampliado, TalkBack y controles reales; cualquier desviación se resuelve en el componente/tema compartido.

## Recorridos

Aplicación por hito (02/10/2026): [ENTREGAS](ENTREGAS.md) contiene el inventario E1. Se implementan captura, eventos, bandeja, resumen y supervisión local con datos reales de Room y perfiles sintéticos rotulados. Los recorridos de login/servidor/consolidación siguientes describen el producto E2. No añadir botones de envío ficticio ni paneles de otros dispositivos con datos que parezcan actuales.

Captador: acceso → evento asignado → confirmar contexto de ubicación → inicio con pendientes/resumen → nueva captura → ingreso manual o escaneo → preferencias → consentimiento y revisión → guardar → confirmación local → bandeja/detalle con estado de envío.

Supervisor: acceso → sede/evento → panel con frescura de reportes → capturas/conflictos → corrección o anulación → confirmación de cierre. Creación/asignación de eventos desde una sección administrativa del mismo Android; no se exige portal web adicional.

El formulario puede consultar el consentimiento desde el inicio. El orden de pantallas no autoriza persistencia previa a su aceptación. Si se pasa el dispositivo al prospecto, activar modo formulario sin navegación al historial; salir exige desbloqueo local del operador. Si no puede garantizarse ese bloqueo, utilizar modo asistido sin entregar el equipo.

## Componentes compartidos

- `AppTextField`: etiqueta, ayuda, mensaje de error, contador cuando sea útil y teclado apropiado.
- `ProgramSelector`: búsqueda, orden alfabético, selección múltiple y chips removibles; nada de desplazarse por un dropdown enorme sin búsqueda.
- `SyncStatusChip`: texto/icono de estado, sin usar el color como única señal.
- `ConnectivityStatus`: red detectada/sin red/estado desconocido según observación real; en E1 no atribuye salud al servidor ni envío. Su diseño no depende sólo del color.
- `LocationSummary`: origen dispositivo/evento, antigüedad y precisión disponible; nunca ocultar el fallback.
- `ConsentSection`: desmarcada por defecto, versión/texto accesible y modo de aceptación.
- `EmptyState`, `LoadingState`, `ErrorState`: acción de recuperación y mensajes específicos.
- `ConfirmDestructiveAction`: qué se anula/descarta, consecuencia y acción de cancelar.

Nombres son propuestas de componentes, no archivos existentes. Los ViewModels producen estados; los componentes no validan RUT ni llaman al backend.

## Estados y mensajes

«Guardado en este dispositivo» se muestra después de transacción local. «Sincronizado» sólo después del ACK. «Sin conexión» describe red; «Servidor no disponible» describe una falla diferente. «No hay datos recientes de este dispositivo» no equivale a cero capturas.

E1 muestra «Demo local» y «Envío no habilitado en esta entrega» donde afecte la interpretación. Cambiar de conectividad no borra el formulario, no marca sincronizado y no inicia un loader de transmisión inexistente. GPS/cámara/conectividad son los tres recursos previstos en E1.

Errores de campo al salir del campo o intentar guardar; evitar marcar todo en rojo antes de interacción. Conservar entradas ante fallas recuperables, mover foco al primer error y no deshabilitar una acción sin explicar qué falta. El formulario con consentimiento rechazado se limpia.

## Adaptación y animaciones

Teléfonos primero; pantallas amplias pueden mostrar lista/detalle en dos paneles. No bloquear orientación; probar teclado abierto, landscape y letra ampliada. No diseñar sólo para una resolución fija.

Animaciones discretas: transición de bandeja a detalle, aparición de errores y confirmación de guardado. La animación no es evidencia de que una operación terminó; la gobierna el estado real. Respetar configuración de reducción/desactivación de animaciones del sistema. Sin loaders infinitos: cancelación o explicación de espera cuando corresponda.

## Revisión visual por RF

Evidencia de estado normal, vacío, error y carga; captura con datos ficticios; revisión por compañero. Mantener guía actualizada si se agrega un patrón, evitando variantes locales del mismo componente. Que el código compile no demuestra que teclado, permisos, cámara o texto ampliado sean utilizables.


## Refinamiento E1 candidato · 11/10

Por encargo explícito se prepara bienvenida, acceso con fixtures locales, menú, perfil y resumen con gráficos. Ver [alcance y pruebas manuales](UI_REFINEMENT_E1.md). La paleta anterior describe la adaptación inicial: el candidato mantiene amarillo #FFB800/negro #1A1A1A y añade azul #112B46 como apoyo propio, fondo claro #F3F5F8 y fondo oscuro #101B28 / superficie #1B2A3B. No usar amarillo como texto normal sobre blanco. Se conservan fuentes nativas y controles Material 3; tarjetas de radios 12/20/28 dp y elevaciones discretas. Se priorizan superficies opacas para texto y formularios.

Google/Microsoft se anuncian deshabilitados. Registro remoto/perfil editable institucional siguen en E2; el perfil E1 muestra identidad y contexto ficticios. La carga opcional de prospectos inventados conserva validación, consentimiento sintético, permisos de alta y duplicados de los repositorios. No implica aceptación del cliente ni del contrato de registros. Menú lateral desde 840 dp disponibles; los datos siguen separados por actor y ámbito. El prototipo editable propone una composición tablet; la APK debe validarse por separado con teclado, giro y letra ampliada.
