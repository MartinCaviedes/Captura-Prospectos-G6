# Identidad, apariencia y muestra mixta · E1

## Bienvenida con contenido de captura · 19/10/2026

Por observación del operador se retira la ilustración de órbitas de WelcomeScreen. Se reemplaza por texto nativo adaptable: **Datos de contacto**, **Intereses académicos** y **Consentimiento**, con numeración informativa sin acciones o datos simulados. Conserva el estilo, tema, logos, título y botón Comenzar. Martín confirma además el icono aplicado en la APK previa 1f5c8b2b; esta confirmación se registra separada del nuevo texto.

APK debug 22d3e9876628f4eb076bee009845737b32f808fdbad670353219c98bb368ae49: assembleDemoDebug y lintDemoDebug aprobados, firma/ZIP correctos, instalación -r -t conservando archivos de la base demo. Inspección del agente en screenshot real del Xiaomi oscuro/vertical: etiquetas completas y Comenzar visible. Lint 30 avisos/0 errores; se conserva el recurso gráfico anterior como antecedente, sin suprimir avisos. [Evidencia](evidencias/bienvenida-captura-2026-10-19.json). No suites automáticas nuevas ni release reconstruida por este cambio de presentación; 191 JVM/21 Android mantienen su APK 1f5c8b2b. No nueva validación humana del bloque, claro/horizontal/XXL/TalkBack/tablet ni cierre R4. Se publica en la misma rama/PR #11; sin merge ni cambios de estado, datos o reglas.

19/10/2026. Martín solicita esta iteración e informa recomendación docente de usar logos oficiales para dar realismo. Es autorización de implementación académica, no aprobación institucional de la aplicación ni cambio de alcance remoto. Base pública b73bb00, rama feature/e1-identidad-tema-demo dependiente de [R4 #10](https://github.com/MartinCaviedes/Captura-Prospectos-G6/pull/10). Revisión del nuevo incremento desde esa base; no repetir como nuevo el código ya revisado de rondas anteriores. Sin merge ni cierre de RF/E1.

## Identidad

Recurso completo desde [sitio oficial Duoc UC](https://www.duoc.cl/wp-content/themes/wordpress-duoc-cl/images/logo-duoc.svg), consultado junto al [manual 2024](https://www.duoc.cl/wp-content/uploads/2024/11/MANUAL-DE-IDENTIDAD-CORPORATIVA-DUOC-UC-2024_70111139.pdf). Original SVG preservado, rasterización integral a 4× en drawable-nodpi, sin cambiar trazados, colores o proporciones. Logo blanco sobre reserva azul; conserva su área propia en ambos temas. Se muestra en bienvenida/acceso, Inicio y menú amplio. No se aplica el tinte de controles al logo.

El símbolo propio es una persona enmarcada por cuatro esquinas de captura: vector azul/dorado/blanco, independiente del logotipo institucional. ProductIdentity lo usa junto al nombre; launcher usa capas adaptativas y monocroma, más respaldos por densidad. Trazos dentro de la zona segura de 66/108, sin texto diminuto o sello institucional reducido en el icono. [Procedencia/hashes](evidencias/identidad-assets-2026-10-19.json); fuente propia y original en android/app/src/main/assets/branding. La marca Duoc UC conserva titularidad y normas de su institución; el símbolo de captura es elaboración del proyecto.

## Apariencia

Perfil → Apariencia → Modo oscuro. AppearanceViewModel conserva StateFlow y una única preferencia booleana dark_mode en SharedPreferences appearance. No almacena credenciales, capturas ni consentimiento; Room continúa como almacenamiento de inscripciones. Predeterminado oscuro para conservar V3. La preferencia se lee al abrir la app, sobrevive al giro y se aplica también a bienvenida/acceso tras cerrar sesión. MainActivity conecta el dueño de estado al tema; applySystemBarAppearance adapta iconos de barras del sistema. SharedPreferences no reemplaza el repositorio cifrado.

LocalG6DarkTheme coordina fondos e imágenes; MaterialTheme gobierna textos, tarjetas e iconos de controles. El formulario ahora sigue ambos temas y mantiene Surface con onSurface explícito; sustituye su anterior variante siempre luminosa sin recuperar el fallo de blanco sobre blanco. Logos e ilustraciones de marca conservan sus colores.

## Datos ficticios

Perfil → Preparar datos ficticios → confirmación explícita. El captador sólo puede cargar en eventos sintéticos originales autorizados. Se mantienen Captador A/B y Supervisor; no roles nuevos ni cuentas reales.

| Estado | Muestra completa por evento/captador | Construcción |
|---|---:|---|
| Falta contexto | 24 | Fixtures antiguas preservadas; alta confirmada sin ubicación |
| Listas (READY local) | 18 | Alta validada con coordenadas de fixture (0,0), precisión sintética 5 m y consentimiento ficticio |
| Borradores | 12 | SAVE_DRAFT con consentimiento ficticio |
| Anuladas | 6 | Alta seguida de cancelación real con revisión esperada y motivo de muestra |

READY es completitud local; transporte continúa NOT_QUEUED, sin ACK ni envío simulado. Las ubicaciones simuladas se informan en la confirmación: no son una adquisición GPS, presencia en un evento o consentimiento de personas reales. En una muestra sin otros registros, métricas del captador: 42 activas (18 listas +24 falta contexto), 12 borradores y 6 anuladas; 60 filas. El supervisor conserva exclusión de borradores ajenos: 42 activas y 6 anuladas, sin convertir esa exclusión en pérdida. Los intereses se distribuyen entre los tres programas sintéticos elegibles. Registros previos o ediciones pueden hacer variar los totales observados.

DemoDataViewModel llama AdmissionRepository.create y RecordRepository.cancel. Consulta RUT/evento para reconocer todos los estados, incluidos borradores/anuladas. Repetir omite lo existente y conserva revisiones manuales. Tras una interrupción puede terminar una anulación de fixture propia sin editar (revisión 1 y marcadores coincidentes); no resetea la base ni modifica capturas ordinarias para hacerlas coincidir. Un fallo se informa y permite reintentar conservando lo creado. Sin nueva tabla, migración, seed destructivo, oferta institucional ni cambios de reglas de estado.

## Verificación y límites

191 JVM exitosas; 21 Android dirigidas aprobadas en Xiaomi 11T/API 34, con asistencia de apertura MIUI registrada. Datos/proveedores, acceso/giro, ambos formularios y contraste con texto 150 % controlado, Perfil/preferencia/barras, calendarios y red. Room usa bases test-* y preferencias test-appearance-*; archivos de demo intactos por hashes. La prueba mixta verifica estados, auditoría, repetición y conservación de edición manual; otra conserva las 24 previas y recupera una interrupción entre alta y anulación. Prueba de apariencia cambia el switch y comprueba paleta, flags nativos y lectura de preferencia por un nuevo dueño; no atribuirle reapertura humana del proceso.

Build debug/instrumental y release unsigned; firmas debug/instrumental y ZIP comprobados. APK debug 1f5c8b2bf4165c667aabebd5284511217c8dc13460a125424f2e03968cd77a98. Lint 29 avisos/0 errores; sin desactivar controles para ocultar avisos. Bibliotecas nativas debug idénticas al corte previo; no ejecución 16 KB. Primera corrida anterior al ajuste de barras/nodpi también pasó 21, conservada aparte: no se suma a la ejecución final. [Resultado y hashes](evidencias/identidad-tema-demo-2026-10-19.json), [salida Android](evidencias/android-identidad-2026-10-19.txt). No dependencias/Gradle/manifest nuevos ni modificación de dominio, Room o E2.

Confirmación del operador sobre esta APK: cambiar tema funciona en todas las pestañas sin textos invisibles y la preparación/visualización de la muestra funciona correctamente. Es un reporte global del recorrido guiado; no se reportan conteos numéricos ni detalle independiente de cada logo/icono/reapertura/giro/XXL. No elevarlo a auditoría visual exhaustiva. La carga manual escribe fixtures por acción del operador; los hashes de conservación pertenecen a las pruebas automáticas previas. La visual XXL y otros recorridos de R4 conservan su APK histórica. TalkBack permanece aplazado, tablet físico/portal sin entorno y aceptación/entrega siguen pendientes. No congelación, integración automática ni cierre R4.
