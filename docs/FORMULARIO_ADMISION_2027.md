# Contrato de captura F2027

Reglas acordadas para el proyecto al 06/10/2026, incorporadas en SRS 1.4. Fuente: cupón de admisión recibido, dos páginas; el año 2027 procede del nombre aportado. SHA-256 de la fuente: c702b52ad2bc125a7493038540e4f273087ac85080191e3c9b00cc56cb26cd25. La fuente impresa no se redistribuye aquí. La tabla siguiente distingue observación impresa de regla adoptada; los acuerdos del equipo no equivalen a aprobación institucional.

F01: hasta tres carreras priorizadas. F02: campos y opcionales definidos en la tabla del contrato. F03: sede del evento y preferencias independientes, sedes múltiples opcionales. F04: nacimiento por QR sólo con contenido/formato verificable; no ampliar G6DEMO:1 por inferencia.

Dominio/persistencia implementados; suite Android v3 aprobada; UI y revisión humana pendientes. La transcripción del catálogo no es oferta habilitada. Sólo datos ficticios; consentimiento institucional, menores y formato real de cédula pendientes para cualquier piloto real.

## Qué muestra y qué cambia

| Elemento impreso | Evidencia | Relación con la app |
|---|---|---|
| Autorización y referencia al consentimiento informado | Página 1, casilla y texto sobre reserva/uso para contacto | Respalda aceptación explícita; no demuestra autorización verbal ni política para menores |
| RUT, Nombre, Apellidos | Página 1, casillas separadas | RUT ya previsto; nombre/apellidos separados difieren de fullName actual |
| Femenino / Masculino / No indica | Página 1; opciones sin título de variable explícito | Campo nuevo; no inferir identidad ni imponerlo como requisito |
| Fecha de nacimiento | Página 1, día/mes/año | Campo nuevo; no derivar política de menores del formulario |
| Comuna donde vives | Página 1 | Nueva; distinta de sede y de ubicación GPS |
| Celular, prefijo 569 | Página 1 | Compatible con regla móvil chilena vigente; no prueba números extranjeros |
| E-mail | Página 1 | Ya previsto |
| Actividad actual: 4° Medio, Egresado Enseñanza Media, Otra + texto | Página 1 | Más específico que SECONDARY; no mapear automáticamente el valor genérico a 4° Medio |
| Establecimiento y comuna de tu establecimiento | Página 1 | Nombre ya previsto; comuna nueva y distinta de residencia |
| Jornada Diurna / Vespertina | Página 1 | No equivale a modalidad presencial/online ni acredita oferta carrera/jornada |
| Sede de interés | Página 1, 19 opciones agrupadas en 5 regiones | Preferencia del prospecto; no es el ámbito de permisos ni necesariamente sede del evento |
| Nivel de interés Alto / Medio / Bajo | Página 1 | Nuevo; no es prioridad de una carrera |
| Afirmación con que se identifica | Página 1, cuatro alternativas | Nueva motivación; no almacenar como diagnóstico ni inferir capacidad económica |
| Hasta 3 carreras, con prioridades del 1 al 3 | Página 2, encabezado | Diferencia explícita con multiselección sin ranking del SRS; decisión F01 |
| Escuelas, nivel profesional/técnico y Duoc Online | Página 2 | Catálogo de referencia; falta mapa de oferta por sede/jornada |

El formulario no marca inequívocamente todos los campos obligatorios, ni define exclusividad de cada grupo de casillas. No convertir automáticamente toda casilla en obligatoria ni el número de cuadros impresos en máximo de caracteres digital. Tampoco demuestra que falten datos digitales como evento, captador, consentimiento versionado o auditoría: éstos siguen siendo necesarios.

## Catálogo transcrito

[catalogo-cupon-2027.json](referencias/catalogo-cupon-2027.json) conserva **84 entradas de programas**, 10 secciones y 19 sedes. Las cuatro menciones de Administración en Turismo y Hospitalidad se guardan bajo el programa padre, no como cuatro carreras adicionales. Duoc Online es sección diferenciada; no se convierte en sede física. Se conserva cada aparición de nombres repetidos entre secciones, sin decidir equivalencia curricular.

Cada fila tiene identificador documental propio, página y nombre impreso; no son códigos oficiales ni UUID del dominio. `siteIds` y `schedules` son null y `enabledForCapture` es false: **no es un seed de ofertas válidas**. Nueve escuelas más Duoc Online; verificación cruzada humana pendiente. No filtrar a «carreras de Viña» a partir de este PDF: esa relación no está impresa.

Los horarios impresos son informativos: diurna L–V 08:30–19:00; vespertina L–V 19:00–22:45 y sábado 08:30–16:00. No son validaciones de disponibilidad ni horarios de cada carrera. La nota del reverso pide informarse de las jornadas por carrera.

## Contrato objetivo acordado — 06/10/2026

Obligatoriedad aplicable a confirmar nuevas inscripciones; borradores explícitos consentidos pueden estar incompletos. Límites de longitud son parámetros técnicos de trabajo revisables, no exigencias atribuidas al cliente. Sin preselecciones de respuestas personales.

| Campo | Tipo / opciones | Regla vigente |
|---|---|---|
| givenNames, familyNames | Texto separado | Ambos obligatorios; trim 2–120 cada uno, nombres compuestos y un solo apellido permitidos |
| fullNameSnapshot | Texto derivado | Componer para registros nuevos; no separar fullName histórico por espacios |
| rut, email, phone | Reglas SRS | Sin cambio; no verifican identidad ni titularidad |
| formPersonalOption | FEMALE / MALE / NOT_STATED / null | Opcional y sin preselección; null distinto de «No indica» explícito. Etiqueta final no informada, no afirmar significado institucional |
| birthDate | LocalDate | Obligatoria; fecha real no futura. Sin edad mínima/máxima inventada; ingreso manual disponible |
| residenceCommune | Texto | Obligatoria, trim 2–120; entrada manual E1, nunca inferida del GPS del captador |
| currentActivity | FOURTH_SECONDARY / SECONDARY_GRADUATE / OTHER | Obligatoria, sin preselección; sustituye enseñanza media genérica para altas nuevas |
| currentActivityOther | Texto / null | Requerido sólo con OTHER, 2–160. No inventar descripción; al cambiar actividad confirmar descarte de texto si corresponde |
| establishment | Texto / null | Obligatorio sólo en 4° Medio; opcional para egresados/otra actividad, permitiendo indicar último establecimiento; 2–160 si se informa |
| establishmentCommune | Texto / null | Opcional cuando se informa establecimiento, 2–120; manual e independiente de residencia |
| preferredSchedule | DAY / EVENING / null | Una preferencia opcional, sin preselección; no deducir disponibilidad. Exclusivamente online no exige jornada |
| interestLevel | HIGH / MEDIUM / LOW / null | Una opción, opcional y sin preselección |
| motivation | INCOME_PROGRESS / VOCATION / RESUME_OR_NEW_PATH / FINANCING_CONCERN / null | Una de las cuatro alternativas del cupón, opcional, sin campo libre adicional ni inferencia |
| preferredSiteIds | Lista de referencias | Cero o más sedes únicas, sin máximo adicional al catálogo; opcional incluso con interés presencial; nunca sede del evento por defecto |
| interests | Programa + prioridad | 1–3 programas únicos al confirmar, orden contiguo 1..n; preferencias generales independientes de sede/jornada, no oferta confirmada |
| tourismMention | Referencia / null | Una mención opcional de Administración en Turismo y Hospitalidad; no consume otro cupo de carrera |
| modality / contactChannel / notes | Opcionales existentes | No eliminados por el cupón; modalidad distinta de jornada. Notes no se usa para eludir ausencia de texto libre en motivación |
| consent y metadatos | Contrato SRS | Consentimiento antes de persistir, contexto/captador/hora/fuente preservados; sólo demo ficticia |

Las cuatro motivaciones representan inversión/progreso económico, vocación, retomar/comenzar otra carrera y preocupación por financiar estudios. La UI conserva las alternativas del cupón, sin interpretación psicológica ni económica automática.

## Establecimiento sugerido y ubicación

El captador selecciona su evento/contexto y puede registrar ubicación según RF04. Si el evento tiene un establecimiento **identificado explícitamente**, la UI puede sugerirlo para el prospecto y pedir confirmación antes de aplicarlo, siempre editable. No basta estar físicamente en ese lugar: el prospecto puede estudiar en otro establecimiento.

GPS registra coordenadas; no determina por sí solo nombre del establecimiento, comuna de residencia, sede de interés ni oferta académica. No se incorpora geocodificación ni búsqueda automática de instituciones por esta decisión. Sin establecimiento configurado o sin confirmación, seguir con entrada manual. Cambiar el contexto no sobrescribe respuestas ya ingresadas.

## Sedes, jornada y catálogo

Las preferencias generales de sede y jornada no son vínculos obligatorios de cada carrera ni confirman disponibilidad. Los permisos del captador se aplican a la sede/evento operativo, no restringen por inferencia las sedes preferidas del prospecto. Una lista vacía significa ausencia de preferencia registrada, no desconocimiento del contexto del captador.

Se permiten carreras de Duoc Online junto a otras dentro de las tres prioridades, identificando la sección online. Duoc Online no es una sede física. Exclusivamente online no requiere sede física ni Diurna/Vespertina; tampoco se inventan valores para rellenarlas.

Q12 queda aplazada: no hay fuente de oferta por sede/jornada ni alcance territorial informado por cliente. Para pruebas E1 se conserva catálogo sintético rotulado. La transcripción de 84 entradas es referencia; no habilitarla automáticamente como oferta de Viña ni prometer equivalencia de programas homónimos entre secciones.

## Nacimiento desde QR: posibilidad conservada, no funcionalidad confirmada

El equipo conserva esta posibilidad condicionada a una fuente técnica verificable. Se incorporará al parser únicamente si una especificación verificable o muestra estructural autorizada demuestra que contiene fecha de nacimiento y permite interpretarla correctamente. Mostrar candidato y pedir confirmación antes de aplicar; permitir corrección/manual si falta, es inválido o el formato no está soportado. No inferir edad/fecha del RUT ni rellenar una fecha inventada.

El QR sintético G6DEMO:1 conserva su contrato actual con fullName/rut; no se le añade birthDate silenciosamente. Una ampliación requiere versión/parser/fixtures y pruebas. El soporte de cédula real y esta extracción no bloquean el recorrido académico E1 ni se presentan como ya implementados.

## Decisiones y pendientes restantes

| ID | Estado | Efecto |
|---|---|---|
| F01 | Aprobado 05/10; dominio/persistencia probados, suite Android v3 aprobada; UI/revisión pendientes | Hasta tres carreras priorizadas |
| F02 | Reglas aclaradas 06/10; dominio/persistencia probados, suite Android v3 aprobada; UI/revisión pendientes | Aplicar tabla anterior, sugerencia confirmada y motivación cerrada opcional |
| F03 | Reglas aclaradas 06/10; dominio/persistencia probados, suite Android v3 aprobada; UI/revisión pendientes | Múltiples sedes opcionales sin máximo adicional; preferencias independientes, combinación online permitida |
| F04 | Posibilidad conservada, sujeta a fuente técnica | Nacimiento por QR sólo al verificar contenido/formato; siempre fallback manual |

No quedan pendientes los cinco puntos aclaratorios. Sigue sin información institucional la etiqueta del grupo Femenino/Masculino/No indica, políticas para datos reales y formato real del documento. Mantenerlos explícitos; no reabrir obligatoriedad/cardinalidad ya resueltas ni convertir esta aprobación del proyecto en validación institucional.

## Adaptación técnica implementada y consumidores pendientes

SRS 1.4 y LOCAL_MODEL definen el objetivo; el incremento 83bbed2 implementa dominio/validación de campos, prioridades y preferencias independientes. El incremento 91b4983 agrega Room v2 y repositorio F2027 con migración conservadora. La suite Android v3 de 24 pruebas pasó; revisar los contratos antes de conectar consumidores; diseño técnico y pruebas en LOCAL_MODEL. Migración no destructiva: conservar fullName/actividad/intereses anteriores sin inventar apellidos, fechas, comunas, ranking o sedes. Datos históricos incompletos no se borran ni se completan automáticamente para satisfacer reglas nuevas.

Pruebas documentales objetivo en [f2027-contract.json](fixtures/f2027-contract.json); no son tests ejecutados. Mantener separado el fixture de base anterior para no atribuirle cumplimiento nuevo.

El cupón remite a una página de consentimiento, cuyo contenido vigente no se validó como texto offline. Mantener consentimiento ficticio; política de menores y autorización institucional necesarias antes de datos reales. GPS/cámara/conectividad, cifrado y frontera E1/E2 no cambian.
