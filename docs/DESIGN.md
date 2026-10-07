# Sistema de diseño y flujos

UI F2027 del 06/10: diferenciar campos obligatorios/condicionales/opcionales; ninguna preferencia preseleccionada. Sugerencia de establecimiento desde evento identificado necesita confirmación y permite editar; no sobrescribe datos al cambiar contexto. Mostrar jornada, nivel y motivación como opcionales, sin campo libre para motivación. Conservar ingreso manual de nacimiento, sin prometer que el lector lo extrae.

Acuerdo F01: las opciones pueden ordenarse alfabéticamente; el resumen de hasta tres seleccionadas se ordena por prioridad e incluye controles accesibles para reordenar. F03: mostrar por separado contexto del captador y preferencia de sede; múltiples sedes opcionales sin máximo adicional, sin preseleccionar la del evento. F02 aclarado el 06/10.

Revisión 05/10: antes de implementar captura/preferencias, revisar F01–F03 en FORMULARIO_ADMISION_2027. Nombre/apellidos, actividad, nuevas variables, ranking y sede de interés tienen reglas acordadas en la revisión del 06/10; no inventar obligatoriedad ni confundir jornada, modalidad y sede. La base visual y recursos E1 no cambian.

Versión 0.1, 30/09/2026. Propuesta común para que cada integrante amplíe la app sin crear estilos distintos. No existe mockup aprobado ni marca comercial definitiva.

## Principios

Material Design 3, textos en español, jerarquía clara y contraste comprobado. Utilizar tokens semánticos de tema para color, tipografía, espaciado y formas; no hexadecimales repetidos en cada pantalla. No copiar logos ni presentar colores como identidad oficial sin material autorizado.

Escala inicial de espaciado: 4/8/12/16/24/32 dp. Controles táctiles >=48 dp, etiqueta siempre visible, soporte de texto ampliado y TalkBack. Ningún estado se comunica sólo mediante color. La guía de [accesibilidad de Compose](https://developer.android.com/codelabs/basic-android-kotlin-compose-test-accessibility) respalda el tamaño de objetivos táctiles; estos tokens de espaciado son propuesta del equipo técnico.

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
