# Refinamiento visual E1 y comprobación manual

Antecedente del primer refinamiento de 11/10/2026. La V3 aprobada posteriormente actualiza tema, vistas y accesos de muestra; leer [UI_CONEXIONES_E1](UI_CONEXIONES_E1.md) para su comportamiento y evidencia. Las etiquetas/capturas y resultados siguientes corresponden al incremento anterior.

Incremento candidato en `feature/e1-ui-refinement`, basado en `161f635` (rondas corregidas), dependiente del PR #1 de producto. No supone integración en develop, aceptación contractual ni cierre de E1.

## Decisión y alcance

El encargo del 11/10 permite mejorar el flujo visual, navegación, perfil, formularios, resumen y adaptación tablet. El usuario confirmó acceso con credenciales ficticias locales; Google/Microsoft sólo anunciados para una entrega futura. Se conserva Room v4, consentimiento, permisos, contratos de dominio y separación funcional de captador/supervisor. Sin backend, tokens, OAuth, registro remoto, sincronización ni nuevas reglas de admisión.

El [diseño editable](https://www.figma.com/design/vVcMyy72PsACIh4stMKMKG?node-id=7-220) contiene ocho vistas de teléfono y dos de tablet. Sus cifras son ilustrativas; la APK utiliza exclusivamente métricas del repositorio. Los campos resumidos del prototipo no sustituyen F2027.

Bienvenida → preparar contexto si falta → acceso demo → elegir evento → inicio. Menú: Inicio / Registros / Resumen / Perfil. Se preservan confirmación de descarte y bloqueo de salidas durante escrituras. Un proceso nuevo exige nuevamente acceso; la selección anterior de Room no autentica al operador. El formulario continúa sólo en memoria durante recreación, sin PII en SavedState.

## Credenciales ficticias de evaluación

| Perfil | Correo inventado | Contraseña inventada |
|---|---|---|
| Captador A | `captador.a@g6.example` | `DemoG6!2026` |
| Captador B | `captador.b@g6.example` | `DemoG6!2026` |
| Supervisor | `supervisor@g6.example` | `DemoG6!2026` |

Son fixtures públicos, no secretos ni autenticación de producción. Los botones de cuenta completan sólo el correo; se escribe la contraseña. Google/Microsoft están deshabilitados y rotulados «Próximamente». El registro remoto se anuncia sin simular una cuenta creada. No se guardan contraseñas ni tokens.

## Prospectos de evaluación

En Perfil del captador y en uno de sus eventos originales demo, «Preparar datos ficticios» pide confirmación antes de crear 24 altas a través de AdmissionRepository. Guarda RUT con dígito verificador, nombres explícitamente ficticios, contacto inventado, carreras del catálogo y evidencia del consentimiento sintético. No inventa GPS: las altas quedan NEEDS_CONTEXT. Repetir la carga omite duplicados propios activos. Un error detiene la carga e informa el avance parcial; no borra ni reemplaza registros. Los registros anulados no se reactivan: repetir puede crear una nueva inscripción activa conforme al contrato de duplicados. No es un generador del benchmark de 400.

El supervisor consulta las altas autorizadas creadas por los captadores; no se le concede permiso de alta ni se cambia de perfil por debajo de la interfaz para cargar datos. Captador B no ve los registros de A. La carga no debe usarse con PII ni en entornos E2.

## Revisión visual y accesibilidad

Paleta: amarillo Duoc, azul de apoyo propio del proyecto y superficies opacas; tipografía Android nativa, tamaños sp, campos Material y tarjetas redondeadas. No se afirma tema móvil oficial ni aprobación institucional. Menú inferior compacto y lateral desde 840 dp de ancho disponible; formularios limitan ancho y siguen desplazables. La simulación de ancho con Density en tests no acredita ejecución en tablet física o emulador.

Gráficos: barras sobre cifras reales por carrera, nombres y cantidades accesibles. Una persona puede tener varios intereses; no sumar carreras como personas. Un error mantiene el aviso y el último cálculo; desconocido no equivale a cero. Correo solicita KeyboardType.Email y desactiva autocorrección; la distribución concreta depende del teclado instalado. Ver [opciones de teclado Compose](https://developer.android.com/develop/ui/compose/text/user-input).

## Comprobación en Xiaomi

Usar sólo datos inventados. No desinstalar, borrar almacenamiento ni guardar un formulario personal para probar. La rotación manual aprobada sobre `161f635` es evidencia anterior a esta interfaz; repetir tras instalar el candidato.

1. **Acceso y giro:** probar contraseña incorrecta (sin entrar), luego correcta. Elegir evento. Abrir formulario, escribir «Prueba de giro», girar vertical/horizontal con teclado abierto, desplazarse y volver. Esperar conservación del texto y confirmación de descarte. Descartar sin guardar. Pulsar correo y comprobar acceso a @; la app puede solicitar un teclado, no imponer sus teclas.
2. **QR y cámara:** usar el QR sintético seguro `G6DEMO:1` entregado, que contiene Persona Ficticia Alfa / 12345678-5, sin URL. Variar distancia e iluminación, acercar/alejar, girar con el lector abierto y cerrar con Atrás. Esperar candidata, confirmación sólo del RUT y nombre manual. Cerrar no aplica datos ni deja cámara activa. Reabrir para comprobar que recupera el preview.
3. **TalkBack:** en Ajustes buscar Accesibilidad → TalkBack y revisar primero cómo desactivarlo. Deslizar a derecha/izquierda con un dedo mueve el foco; doble toque activa y dos dedos desplazan. Recorrer acceso, menú, formulario, consentimiento, errores, detalle y resumen. Comprobar etiquetas de campos, estado deshabilitado de proveedores, destinos seleccionados, lectura de números/carreras y acceso a confirmaciones. Usar Atrás o «Volver al panel» y descartar. No confundir tests de semántica con prueba de experiencia hablada. [Ayuda oficial](https://support.google.com/accessibility/android/answer/6283677?hl=es).
4. **Permisos:** desde Ajustes de la app, negar cámara y ubicación; comprobar mensaje y alternativa manual. Con ubicación aproximada, la app debe manejar permiso sin exigir siempre precisa; puede agotar 10 s si no encuentra candidata que cumpla <=100 m/<=5 min. Con permiso preciso y GPS apagado comprobar fallo controlado; reactivar GPS y reintentar. Revocar permiso en Ajustes mientras la app está en segundo plano y volver: ningún cierre inesperado ni ubicación aplicada sin confirmar. Restaurar permisos preferidos al terminar. No usar ADB para concederlos en esta prueba.
5. **Redes:** modo avión → sin red; WiFi con salida válida → validada; WiFi con portal cautivo antes del acceso → conectada sin validar (puede variar por detección del sistema). Mantener un nombre ficticio en formulario mientras cambia la red: no se borra ni aparece envío o sincronización. Un hotspot con internet apagado prueba red sin salida, pero no sustituye un portal cautivo real. No introducir credenciales de redes en evidencias.
6. **Tablet/visual:** pendiente equipo físico o emulador. Revisar vertical, horizontal, ventana dividida, teclado, tema claro/oscuro y fuente 150–200 %. Menú y controles sin cortes, sin scroll horizontal para leer formularios. Figma y un test de ancho simulado son preparación, no aprobación Android tablet.

Registrar versión exacta, tema/tamaño de fuente, escenario, resultado y mensaje. Una revisión humana independiente del código y aceptación de contratos sigue pendiente; el ensayo P3 requiere que el equipo explique decisiones, demuestre recorridos y reconozca límites E1/E2.

## Comprobación manual del candidato — 11/10/2026

En la APK con SHA-256 `c78e6dd1dffe26c427267e0ec9d71b0fb0ab179388b0c2cb7ada22518c1279ff`, el usuario respondió al recorrido solicitado de acceso, teclado de correo, giro, desplazamiento y descarte: todo funciona en vertical y horizontal, sin botones cortados ni problemas con el texto ingresado. Es evidencia manual de este recorrido en Xiaomi; no acredita TalkBack, tablet, redes especiales ni revisión independiente.
