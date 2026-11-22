# Revisión parcial de seguridad

Base de revisión a489882 más cambios locales. El alcance comprobado comprende el seed, el tratamiento de audiencia ausente en los tres puntos de validación y un recorrido de logging con datos sintéticos.

| Hallazgo | Evidencia | Efecto y tratamiento |
|---|---|---|
| ARQ-01 · alta · usuarios | UsuarioDataSeeder y SeedIsolationTest | El seed podía crear cuentas privilegiadas fuera de desarrollo. El cambio local exige local, excluye prod y exige contraseñas externas. La prueba arrancó con ambos perfiles y encontró cero cuentas. Revisar cuentas preexistentes antes del despliegue. |
| ARQ-03 · media · usuarios | SecurityConfig.jwtDecoder y SecurityIntegrationTest.signedTokenWithoutAudienceReturnsUnauthorized | El acceso directo con JWT RS256 válido sin aud disparaba NullPointerException. La prueba reprodujo la excepción. Se controla la audiencia nula antes de buscar la audiencia esperada. |
| SEC-01 · alta · prospectos | ApiErrors.duplicate, restricción uk_prospect_active y logger Hibernate SqlExceptionHelper | La respuesta al cliente ocultaba el identificador pero el log de Hibernate incluía el RUT y UUID de evento. La reproducción utilizó el duplicado ficticio del test de integración. Se desactiva ese logger concreto. Esto reduce también sus diagnósticos SQL para otros errores. La disponibilidad de la base y el volumen de errores requieren observación independiente. |

El control de logging cubre el duplicado observado con H2. Falta examinar excepciones inesperadas, proveedores JDBC distintos, cuerpos HTTP registrados por proxies y configuración efectiva del entorno futuro. También quedan pendientes análisis de dependencias y protección del login frente a abuso.

## SEC-03 — crítica por avisos de dependencias — pendiente de remediación

Análisis ejecutado el 17/11/2026 con Trivy 0.76.2 oficial, descargado desde la release del proveedor. Se verificó SHA256 del archivo Linux-64bit contra el digest publicado por GitHub, bc0fc726e017b8927a62b2d3531d7a84ee01769daf5c99ed866dced54089fdbf. La herramienta y sus bases permanecen en work/tooling, fuera del repositorio. [Release oficial](https://github.com/aquasecurity/trivy/releases/tag/v0.76.2)

Se analizaron las tres imágenes locales quality mediante image-src docker, scanners vuln, offline-scan y skip-db-update tras descargar las bases públicas. La BD general indica UpdatedAt 2026-11-17T06:59:44Z. El modo offline-scan evita búsquedas de dependencias remotas durante el análisis, pero la preparación inicial descarga las bases general y Java. No se enviaron imágenes ni código a un servicio de análisis. Trivy usó el daemon local. Los tres análisis terminaron con código 0. Ese código expresa ejecución completada, porque no se configuró un umbral de fallo por severidad.

| Imagen | Java críticas | Java altas | Java medias | Java bajas | SO medias | SO bajas |
|---|---:|---:|---:|---:|---:|---:|
| api-gateway | 7 | 33 | 45 | 9 | 35 | 16 |
| users-service | 10 | 26 | 28 | 9 | 35 | 16 |
| prospects-service | 10 | 26 | 28 | 9 | 35 | 16 |

Son coincidencias paquete/aviso por imagen. El inventario suma 393 filas y 183 combinaciones únicas clase/paquete/versión/aviso al deduplicar servicios. Los componentes compartidos se repiten en las imágenes. El CSV dependency-findings.csv conserva componente, versión, identificador, severidad, versiones corregidas reportadas y referencia. Los reportes JSON completos y logs están en work/audit-trivy-<servicio>.json/.log. No se suman como 393 vulnerabilidades explotables independientes.

### Contraste con fuentes primarias y uso del código

- Spring Security web 6.5.6 está en el rango del aviso crítico CVE-2026-22732. El proveedor identifica un problema de escritura de cabeceras HTTP en aplicaciones Servlet y publica corrección 6.5.9. Usuarios/prospectos usan Servlet y SecurityFilterChain. Falta reproducir el camino concreto y evaluar el parche compatible gestionado por Boot. Prioridad alta para remediación por protección de respuestas con datos personales. No se ha demostrado extracción de datos. [Aviso Spring](https://spring.io/security/cve-2026-22732/)
- Gateway WebFlux 4.3.0 coincide con CVE-2025-41243, corregido en 4.3.1. El aviso exige el endpoint Actuator gateway habilitado y accesible sin protección. El repositorio expone únicamente health y habilita rutas exactas de probes. Esa precondición no está configurada. Mantener el endpoint gateway excluido y revisar el parche de la línea 4.3 como higiene de dependencias. [Aviso Spring](https://spring.io/security/cve-2025-41243/)
- Tomcat embebido 10.1.48 acumula avisos de autenticadores, HTTP/2 y restricciones de acceso según el scanner. El proyecto usa autenticación JWT de Spring y no configura autenticadores Tomcat DIGEST/FORM ni HTTP/2 explícito. Esas condiciones deben revisarse por aviso, junto con la línea corregida 10.1.x, sin extrapolar cada severidad del scanner a la configuración del proyecto. [Avisos Tomcat 10](https://tomcat.apache.org/security-10.html)
- También aparecen avisos asociados a Jackson, Netty, Micrometer, Bouncy Castle y Spring Framework. Las versiones corregidas del CSV pueden pertenecer a otras ramas mayores. No deben copiarse directamente al POM. Los caminos XSLT, SSE y SNI indicados en algunos avisos necesitan evaluación de uso real antes de calificar exposición. Se mantienen pendientes.

### Plan de remediación compatible

Evaluar primero actualizaciones de mantenimiento de Spring Boot 3.5 y del BOM Spring Cloud 2025.0 compatibles con Java 21. Revisar qué versiones efectivas gestionan para Security, Framework, Jackson, Tomcat y Netty. Mantener coherencia de BOM, evitar una mezcla de parches individuales sin validar y documentar cualquier override indispensable. La necesidad técnica de actualización queda justificada por los avisos, pero esta ronda solo inventarió y contrastó los resultados.

Tras seleccionar versiones, ejecutar toda la suite, ambos smokes y reconstruir imágenes para repetir el análisis con la misma BD. Comparar inventario y cambios de API/configuración. Revisar por separado las actualizaciones del runtime Temurin y paquetes Ubuntu. Los resultados actuales del SO contienen 35 medios y 16 bajos por imagen y ningún alto/crítico detectado en esa clase.

### Límites de cobertura

El análisis asocia versiones a avisos y no prueba explotación ni alcanzabilidad. Las dependencias Java detectadas son las incluidas en los JAR de ejecución. Dependencias de tests/build, scripts, acciones GitHub y cadenas de suministro requieren revisión independiente. El inventario SO deriva de paquetes Ubuntu reconocidos. No se produjo un informe específico completo de CVEs de la JVM Temurin distribuida como runtime, por lo que ese componente necesita contraste con sus advisories. Las bases pueden cambiar y la ausencia de un hallazgo no certifica seguridad. No se midió abuso bajo carga ni se realizaron pruebas invasivas.

## SEC-03 — actualización de mantenimiento verificada, 17/11/2026

Se actualizaron el parent Spring Boot 3.5.7 → 3.5.16 y el BOM Spring Cloud 2025.0.0 → 2025.0.3, conservando Java 21. La matriz oficial vincula Cloud 2025.0.x con Boot 3.5.x. Los cambios se justifican por los avisos inventariados. Se mantiene la gestión coherente de los BOM sin overrides individuales. [Compatibilidad Cloud](https://spring.io/projects/spring-cloud/)

Los JAR construidos contienen Security 6.5.11, Framework 6.2.19, Jackson core 2.21.4, Tomcat 10.1.55 y Netty 4.1.135.Final según corresponda al módulo; Gateway pasa a 4.3.5. Las 29 pruebas y los smokes de procesos/contenedores pasaron, y se reconstruyeron las tres imágenes antes del análisis comparativo.

Se usó Trivy 0.76.2 con la misma base general/Java, mediante skip-db-update y skip-java-db-update. Los nuevos reportes no contienen CVE-2026-22732 ni CVE-2025-41243. Se conservaron las versiones y resultados iniciales en dependency-findings.csv; la evidencia posterior está en dependency-findings-after-maintenance.csv.

| Imagen | Críticas Java antes | Después | Altas Java antes | Después |
|---|---:|---:|---:|---:|
| Gateway | 7 | 4 | 33 | 11 |
| Usuarios | 10 | 7 | 26 | 7 |
| Prospectos | 10 | 7 | 26 | 7 |

Las filas por paquete/aviso bajan de 393 a 227 y las combinaciones únicas deduplicadas entre servicios pasan de 183 a 91. La base SO conserva sus 35 medias y 16 bajas por imagen. La base Temurin quedó fijada al mismo digest. Los hallazgos críticos/altos restantes mantienen SEC-03 parcialmente abierto y requieren revisión individual de condiciones de uso y versiones corregidas.

Boot 3.5.16 es la última release OSS de su generación y Cloud 2025.0.3 es la última de su línea. El proveedor anuncia fin de soporte abierto para ambas. Este parche reduce avisos existentes, pero la estrategia de soporte debe resolverse para producción. Una migración mayor o contratación de soporte es una decisión pendiente de revisión del equipo. No se inició esa migración. [Release Boot](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/), [release Cloud](https://spring.io/blog/2026/06/11/spring-cloud-2025-0-3-aka-northfields-has-been-released/)

## SEC-03 — triage por uso y parches posteriores al BOM

Se seleccionaron overrides acotados por necesidad de seguridad en las líneas ya usadas. El parent gestiona Jackson BOM 2.21.7, Netty BOM 4.1.139.Final, Tomcat 10.1.61 y pgJDBC 42.7.12. Gateway gestiona bcprov-jdk18on 1.85, que Cloud starter incorporaba en 1.80.2. La inspección de BOOT-INF/lib confirma las versiones en los artefactos. No se añadió ninguna biblioteca al grafo de ejecución que no existiera previamente. Los BOM de Jackson y Netty mantienen alineadas sus familias.

| Componente | Justificación | Evidencia de mantenimiento |
|---|---|---|
| Jackson 2.21.7 | Corrige los avisos de core/databind inventariados en 2.21.4 | [Release 2.21](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21) |
| Netty 4.1.139.Final | Parches de la línea 4.1 posteriores al SNI y los avisos HTTP inventariados | [Release oficial](https://github.com/netty/netty/releases/tag/netty-4.1.139.Final) |
| Tomcat 10.1.61 | Parches posteriores a 10.1.55 para el contenedor Servlet | [Avisos Tomcat](https://tomcat.apache.org/security-10.html) |
| pgJDBC 42.7.12 | Release de seguridad posterior a la versión 42.7.11 gestionada por Boot | [Anuncio PostgreSQL](https://www.postgresql.org/about/news/postgresql-jdbc-42712-security-release-3340/) |
| Bouncy Castle 1.85 | Correcciones PKIX/Name Constraints asociadas a bcprov anterior | [Release del proveedor](https://www.bouncycastle.org/resources/new-release-bouncy-castle-java-1-85/) |

La compatibilidad se comprobó mediante compilación, 29 pruebas y recorrido funcional de los tres JAR. La prueba H2 no valida comportamiento del driver PostgreSQL con una base PostgreSQL; ese recorrido permanece pendiente. Estos overrides deben revisarse si se cambia el parent y retirarse cuando el BOM elegido gestione versiones adecuadas.

### Condiciones de los avisos Spring Framework restantes

| Aviso | Clasificación del proveedor | Condición y evidencia local | Estado de triage |
|---|---|---|---|
| CVE-2026-47884 | MEDIUM | XsltView, mapeo /** con renderizado y nombre de vista implícito. Los controladores MVC son RestController JSON; no se encontraron XsltView, ModelAndView o configuración de vistas | Precondiciones no encontradas en el código revisado |
| CVE-2026-47890 | LOW | Fragmentos de vistas enviados mediante SSE con contenido controlado por otro usuario. No se encontraron SseEmitter, ServerSentEvent ni renderizado de fragmentos en los controladores | Precondiciones no encontradas en el código revisado |
| CVE-2026-47892 | MEDIUM | Endpoints funcionales WebFlux desplegados mediante DispatcherServlet. Gateway ejecuta WebFlux/Netty y RouteLocator; usuarios/prospectos ejecutan MVC. No se encontraron RouterFunction funcionales ni esa combinación | Precondiciones no encontradas en el código revisado |

Fuentes primarias de condiciones y severidad: [CVE-2026-47884](https://spring.io/security/cve-2026-47884/), [CVE-2026-47890](https://spring.io/security/cve-2026-47890/), [CVE-2026-47892](https://spring.io/security/cve-2026-47892/). El scanner los clasifica como críticos por su base de avisos. Se conservan esa clasificación y las filas originales para trazabilidad, junto con esta evaluación contextual. No se agregó archivo de supresiones.

La conclusión está acotada al código y configuración versionados revisados. La configuración efectiva de un despliegue futuro puede cambiar las condiciones. El proveedor ofrece corrección 6.2.20 en soporte empresarial y 7.0.9 en OSS para estos avisos. La elección de soporte o migración mayor continúa pendiente de aprobación. El fin de soporte de la línea actual mantiene el riesgo operativo aunque esas funciones no estén configuradas.

### Resultado posterior a los parches acotados

Se reconstruyeron y probaron las imágenes y se repitió Trivy 0.76.2 sin actualizar ninguna base. En cada imagen quedan 2 avisos Java clasificados CRITICAL por el scanner, 0 HIGH y 1 MEDIUM. Los críticos son exclusivamente los avisos de Framework evaluados en la tabla anterior. El medio corresponde a log4j-api CVE-2026-49844 y sigue pendiente de triage. SO conserva 35 MEDIUM y 16 LOW por imagen. Se conservan todos los avisos sin supresiones.

El CSV dependency-findings-after-patches.csv contiene 162 filas y 56 combinaciones únicas clase/paquete/versión/aviso. Frente al análisis inicial, las asociaciones Java altas pasan de 33/26/26 a 0/0/0 y las críticas de 7/10/10 a 2/2/2. La conclusión sigue condicionada al inventario y a las precondiciones de código revisadas. El riesgo de soporte OSS continúa abierto y no se declara certificación de producción.

## Aprovisionamiento inicial de administrador

OPS-01 se resolvió mediante una ejecución CLI explícita sobre PostgreSQL existente. El arranque normal no ejecuta bootstrap. Requiere credenciales JDBC externas y privilegios de inserción/bloqueo; fija rol ADMIN, valida el DTO compartido y limita contraseña a 12 caracteres mínimos/72 bytes UTF-8 máximos para BCrypt. Los valores no aparecen en argumentos ni logs. Los errores JDBC se traducen a un mensaje genérico.

Un bloqueo de tabla y transacción abarcan comprobación de vacío e inserción. Cualquier cuenta existente causa rechazo, sin modificarla. La prueba temporal verifica un solo éxito entre dos lanzamientos, rechazo de repetición y acceso administrativo real con el hash almacenado. Los operadores deben verificar el destino y retirar las variables BOOTSTRAP_ADMIN_* después del uso. La recuperación de cuentas en una base poblada queda fuera de este modo y requiere proceso autorizado.

## ARQ-04 — limitación local de login

Se añadió LoginRateLimitFilter en gateway, antes de la cadena Security, para POST /api/v1/auth/login. Usa dirección del peer, 60 peticiones/60s por defecto y hasta 10000 buckets de memoria. No procesa credenciales ni utiliza cabeceras del cliente como identidad. Se fijó forward-headers-strategy=none. Los excesos devuelven 429 con Retry-After y no-store.

Las pruebas cubren cuota/renovación con tiempo controlado, capacidad/recuperación del mapa, direcciones separadas, cabeceras falsificadas, path codificado/parámetros de segmento, rutas ajenas y configuración inválida. La integración HTTP verifica que dos logins lleguen al destino y el tercero reciba 429 con el límite de prueba. Se mantienen pruebas de rutas protegidas y readiness.

Estado parcialmente corregido. La cuota es por instancia y peer, se reinicia con el proceso y puede ser compartida detrás de proxy. Para una exposición futura, restringir acceso directo a usuarios y acordar el control de abuso/IP confiable en el borde. No se ha validado ese diseño en AWS ni bajo carga distribuida. No se agrega Redis u otro servicio para sincronizar contadores en este incremento.
