# Backend de captación

Microservicios Spring Boot integrados con la aplicación Android mediante Retrofit (`GatewayApiClient.kt` en `:app`) a través de `api-gateway`. Continúa el servicio de usuarios (`a7b1675`), la persistencia de prospectos (`e3a1366`) y la integración móvil (`10ba22d`) en `develop`.

| Servicio | Puerto predeterminado | Responsabilidad |
|---|---|---|
| api-gateway | 8080 | Entrada del cliente Android (Retrofit), validación JWT y rutas a URLs fijas configurables. Sin BD ni negocio. |
| users-service | 8081 | Usuarios JPA, credenciales BCrypt, roles, login y JWT. |
| prospects-service | 8082 | Prospectos persistidos, propiedad, sedes, edición básica y anulación auditada. |

Java 21, Spring Boot 3.5.16, Maven Wrapper 3.9.11. Gateway WebFlux existente; JPA y H2/PostgreSQL para ambos servicios de negocio. Cada servicio posee sus entidades/repositorios y su base. Prospectos no consulta usuarios por petición ni usa claves foráneas entre servicios.

## Preparación y compilación

Instalar un JDK 21, Python 3 (solo prueba funcional), Bash y OpenSSL. Desde `backend/`:

```bash
bash scripts/prepare-local.sh
source .local/env.sh
./mvnw clean verify
```

El setup genera un par RSA 2048 (privada PKCS#8, pública X.509) y contraseñas aleatorias en `.local/`, excluido de Git y con permisos restringidos. No sobreescribe configuraciones existentes ni imprime claves/contraseñas. Cada integrante genera sus propios archivos. `JAVA_HOME` debe apuntar a un JDK 21; no cambiar la versión del POM para compilar con Java 17.

Compilación independiente desde `backend/`, sin instalar los otros servicios:

```bash
./mvnw -f users-service/pom.xml clean verify
./mvnw -f prospects-service/pom.xml clean verify
./mvnw -f api-gateway/pom.xml clean verify
```

## Ejecución

En tres terminales desde `backend/`, cargar `.local/env.sh` en todas:

```bash
# Terminal 1
source .local/env.sh
java -jar users-service/target/users-service-0.0.1-SNAPSHOT.jar
```

```bash
# Terminal 2: solo clave pública
source .local/env.sh
unset JWT_PRIVATE_KEY LOCAL_ADMIN_PASSWORD LOCAL_CAPTOR_PASSWORD
java -jar prospects-service/target/prospects-service-0.0.1-SNAPSHOT.jar
```

```bash
# Terminal 3: solo clave pública
source .local/env.sh
unset JWT_PRIVATE_KEY LOCAL_ADMIN_PASSWORD LOCAL_CAPTOR_PASSWORD
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

Cada proceso permite sobrescribir `PORT`. También se puede usar `./mvnw -pl <servicio> spring-boot:run`.

## Configuración sin secretos en Git

| Variable | Servicio y significado |
|---|---|
| `JWT_PRIVATE_KEY` | Solo usuarios; URI `file:/ruta/privada.pem`. |
| `JWT_PUBLIC_KEY` | Los tres; URI de la misma clave pública. |
| `JWT_ISSUER` | Los tres; predeterminado `captura-g6-users`. |
| `JWT_AUDIENCE` | Los tres; predeterminado `captura-g6-api`. |
| `LOCAL_ADMIN_PASSWORD` | Usuarios; seed demo-admin y supervisor. Usar valor aleatorio del setup. |
| `LOCAL_CAPTOR_PASSWORD` | Usuarios; seed demo-captor, captor-a y captor-b. Usar valor aleatorio del setup. |
| `IDENTITY_JDBC_URL`, `IDENTITY_DB_USER`, `IDENTITY_DB_PASSWORD` | Usuarios; configuración de Jeremy. |
| `JPA_DDL_AUTO` | Usuarios; predeterminado `update`. |
| `PROSPECTS_JDBC_URL`, `PROSPECTS_DB_USER`, `PROSPECTS_DB_PASSWORD` | Prospectos; base separada. |
| `PROSPECTS_DDL_AUTO` | Prospectos; predeterminado `update`. |
| `USERS_SERVICE_URL` | Gateway; predeterminado `http://127.0.0.1:8081`. |
| `PROSPECTS_SERVICE_URL` | Gateway; predeterminado `http://127.0.0.1:8082`. |
| `PORT` | Puerto del proceso correspondiente. |
| `SPRING_PROFILES_ACTIVE` | El seed requiere `local` y se desactiva si también está activo `prod`. El login sigue disponible con cuentas persistidas. |

Ambos servicios usan H2 **en memoria** por defecto, usuario `sa` sin contraseña; los datos se pierden al detenerlos. Para conservar datos locales, configurar bases distintas:

```bash
export IDENTITY_JDBC_URL='jdbc:h2:file:./.local/identity;MODE=PostgreSQL'
export PROSPECTS_JDBC_URL='jdbc:h2:file:./.local/prospects;MODE=PostgreSQL'
```

Para PostgreSQL usar `jdbc:postgresql://HOST:5432/BASE` con bases/usuarios separados y contraseñas externas. El driver ya está incluido; no se requiere instalar PostgreSQL para probar H2. El smoke básico usa H2. El smoke PostgreSQL verifica PostgreSQL 17.12 con pgJDBC 42.7.12 y scripts V001 sobre bases temporales. Desarrollo usa ddl-auto=update y prod usa validate. La adopción de esquemas previos, TLS administrado y restauración siguen pendientes.

El seed de Jeremy conserva sus cuentas y UUID determinísticos; `captor-a` pertenece a `site-1`, `captor-b` a `site-2`, supervisor y administrador a ambas. El seed exige ambas contraseñas externas generadas por el setup. Las contraseñas de cuentas ya persistidas no se cambian al regenerar variables. No utilizar el seed como aprovisionamiento productivo.

## Contrato de identidad existente

- `POST /api/v1/auth/login`: público, JSON `username` (username o email) y `password`. Respuesta: `accessToken`, `tokenType=Bearer`, `expiresIn=900`, `userId`, `username`, `email`, `displayName`, `rol`, `roles`, `siteIds`. Incorrectas: 401; campos inválidos: 400.
- `GET /api/v1/me`: JWT, identidad de usuarios.
- `POST /api/v1/auth/logout`: JWT, confirmación local; no revoca tokens emitidos.
- `GET /api/v1/users[/{id}]`: ADMIN/SUPERVISOR según seguridad existente. `POST /api/v1/users` y `PATCH /api/v1/users/{id}/estado`: ADMIN.
- `GET /api/v1/admin/status`: ADMIN; enruta a usuarios.

Usuarios firma RS256. Claims: `sub` es username, **`userId` es el UUID propietario**, `roles` es lista de ADMIN/CAPTADOR/SUPERVISOR, `siteIds` lista de sedes. Emisor/audiencia configurables, expiración 15 minutos. Gateway y prospectos verifican firma RS256, emisor, audiencia, vigencia (tolerancia Spring de 60 segundos) y estructura de identidad: UUID, sujeto no vacío, roles reconocidos y sedes string. Rechazan claims ausentes o incompatibles con 401. Los cambios de rol/sede/estado posteriores al login se reflejan al emitir otro token; no hay revocación inmediata.

El gateway conserva `Authorization: Bearer ...`, elimina `Cookie` y mantiene las rutas existentes de usuarios/prospectos. Las familias históricas de eventos, catálogo, asignaciones, inscripciones y reportes siguen sin implementación de negocio. Solo `POST login` y salud son públicos. Cada servicio expone únicamente salud general en `/actuator/health`; no se reenvían actuators internos. Sin CORS abierto.

## API mínima de prospectos

Este incremento usa `/api/v1/prospects`; **no implementa el contrato completo `/registrations`** propuesto en `docs/API_CONTRACTS.md`.

| Método y ruta | Permiso y comportamiento |
|---|---|
| `POST /api/v1/prospects` | CAPTADOR, sede en su JWT. Propietario derivado de `userId`. 201. |
| `GET /api/v1/prospects/{id}` | Captador propietario, ADMIN o SUPERVISOR de la sede. |
| `GET /api/v1/prospects?page=0&size=20` | Mismo filtro aplicado en BD. `items`, `page`, `size`, `total`. Tamaño 1–100, página 0–1000000, orden createdAt/id. |
| `PUT /api/v1/prospects/{id}` | Mismos permisos; reemplaza solo nombres/apellidos/email/teléfono de registro activo. |
| `POST /api/v1/prospects/{id}/cancel` | ADMIN o SUPERVISOR de sede. JSON `reason` obligatorio (máx. 500). Sin borrado físico. |

Creación: `rut` con dígito verificador módulo 11 (normaliza puntos y K), `eventId` UUID, `siteId` (máx. 120), `givenNames`/`familyNames` (máx. 120), `email` (máx. 160), `phone` (8–15 dígitos con `+` opcional), `consentAccepted=true` y `consentVersion` (máx. 80). No enviar `captorId`: campos desconocidos dan 400. El servidor registra consentimiento/fecha/captador y timestamps UTC; no acepta fechas de consentimiento aportadas libremente. Validar que consentimiento ya fue obtenido antes de enviar datos personales.

RUT/evento/sede/propietario/consentimiento permanecen inmutables. RUT/evento activo único mediante restricción BD; duplicado devuelve 409 sin identificar al otro captador. Anular guarda actor, motivo y fecha y libera la clave activa para recreación. Anulado no editable; repetir anulación da 409. No se exponen eliminación ni cambio de dueño. JPA `@Version` protege escrituras concurrentes, devolviendo 409; no hay aún precondición de versión del cliente.

Errores de prospectos: `code`, `message` en español, `fieldErrors` y `correlationId` generado por respuesta. 400 formato/campos, 401 JWT, 403 permisos, 404 inexistente y 409 duplicado/estado/concurrencia. Sin stacktrace ni contenido del registro en errores. No constituye todavía trazabilidad distribuida por correlationId. Gateway y usuarios conservan sus respuestas de seguridad existentes.

## Flujo completo

Desde `backend/` con variables del setup cargadas (sin imprimir contraseñas o JWT):

```bash
python3 - <<'PY'
import json, os, urllib.request, uuid
base = 'http://127.0.0.1:8080'
def post(path, data, token=None):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(base+path, data=json.dumps(data).encode(), headers=headers)
    with urllib.request.urlopen(req) as res: return json.load(res)
login = post('/api/v1/auth/login', {'username':'captor-a','password':os.environ['LOCAL_CAPTOR_PASSWORD']})
prospect = post('/api/v1/prospects', {
    'rut':'12345678-5','eventId':str(uuid.uuid4()),'siteId':'site-1',
    'givenNames':'Persona','familyNames':'Ficticia','email':'ficticia@example.com',
    'phone':'+56912345678','consentAccepted':True,'consentVersion':'v1'
}, login['accessToken'])
print('Creado:', prospect['id'])
PY
```

## Verificación

```bash
./mvnw clean verify
source .local/env.sh
python3 scripts/smoke-test.py
git diff --check
```

La prueba funcional inicia y detiene sus propios tres JAR en 18080/18081/18082 (`SMOKE_BASE_PORT` configurable), usa datos ficticios y bases temporales separadas. Comprueba login inválido/válido, JWT alterado, identidad, creación/consulta/edición/lista, aislamiento entre captadores, lectura administrativa/supervisor, duplicado, anulación y persistencia tras reiniciar prospectos. No modifica bases configuradas del desarrollador ni imprime secretos. Las pruebas JUnit cubren además expiración, emisor/audiencia/firma, claims incompatibles, sedes, campos inválidos y conflictos de estado.

## Límites pendientes

Sin integración Android, despliegue, métricas, catálogos, asignación real a eventos, sincronización, idempotencia, reconciliación, correcciones supervisadas completas, refresh/revocación ni catálogo de consentimientos. `eventId` es un UUID recibido; no se acredita existencia/asignación del evento. `siteIds` del JWT restringe sedes, sin llamadas a usuarios. El consentimiento mínimo no sustituye el modelo completo/evidencia offline propuesto. Existen imágenes Docker y pruebas locales de creación duplicada y bootstrap concurrentes. Permanecen pendientes las carreras de edición/anulación y controles distribuidos. Swagger/OpenAPI, discovery, Config Server y mensajería siguen fuera del alcance. El siguiente trabajo del equipo debe completar contratos y permisos de eventos antes de conectar Android.

## Perfil productivo de persistencia

Activar `SPRING_PROFILES_ACTIVE=prod` en los servicios JPA exige sus variables JDBC, usuario y contraseña indicadas en `.env.example`. Inyectar las variables correspondientes a cada proceso mediante el entorno de ejecución. La plantilla es documentación y Spring no la carga automáticamente. Las claves RSA también siguen siendo obligatorias.

Los perfiles `prod` de usuarios y prospectos usan `ddl-auto=validate` y `spring.sql.init.mode=never`. Una base sin las tablas esperadas impide el arranque. Antes de usar ese perfil, el esquema debe existir y haber sido revisado. Los scripts V001 y su aplicación a PostgreSQL 17.12 están verificados localmente. La evolución de datos previos y la automatización de checksums requieren un procedimiento posterior. Las variables de desarrollo `JPA_DDL_AUTO` y `PROSPECTS_DDL_AUTO` no alteran el valor fijado en ese perfil. Spring permite sobrescrituras explícitas de propiedades, por lo que el entorno de despliegue debe revisar sus propias variables y argumentos.

Los defaults de desarrollo permanecen en `application.yml`. Los tests del perfil productivo usan H2 aislado con conexión explícita para probar la validación. Esta prueba no acredita compatibilidad con PostgreSQL. Tampoco prohíbe una URL H2 elegida explícitamente por un operador. El seed está limitado a `local` y queda excluido si también se activa `prod`.

## Fallos de comunicación del gateway

`GATEWAY_CONNECT_TIMEOUT_MS` fija el límite de conexión en milisegundos, con valor predeterminado 2000. `GATEWAY_RESPONSE_TIMEOUT` usa una duración Spring, por ejemplo `5s`, y su valor predeterminado es 5 segundos. Estos límites iniciales deben ajustarse al presupuesto de latencia del entorno futuro. Se usan las propiedades `spring.cloud.gateway.server.webflux.httpclient` correspondientes a Gateway 4.3.0.

Una conexión rechazada al servicio destino devuelve 503 con `code=UPSTREAM_UNAVAILABLE` y mensaje genérico. Una espera de respuesta que excede el límite devuelve 504 mediante el mecanismo de Gateway. Un timeout puede ocurrir después de que el destino haya procesado una escritura. El cliente debe consultar el estado antes de repetirla. Las rutas actuales carecen de filtro Retry y este cambio conserva ese comportamiento. La idempotencia de escrituras sigue pendiente.

Las pruebas usan upstreams HTTP ficticios en loopback para comprobar respuesta lenta y conexión rechazada, junto con las rutas protegidas existentes. No acreditan latencia bajo carga, fallos durante transmisión de cuerpos ni comportamiento de un despliegue cloud.

## Salud y terminación

Cada proceso expone públicamente las rutas exactas `GET /actuator/health/liveness` y `GET /actuator/health/readiness`, con detalles internos ocultos. Liveness refleja el estado de vida de la aplicación. Readiness devuelve 503 cuando la aplicación rechaza tráfico y 200 cuando acepta tráfico. En usuarios y prospectos incluye el indicador de conexión a BD. La readiness del gateway refleja su propia disponibilidad y no sondea los destinos. Los fallos de esos destinos se comprueban mediante las peticiones y los límites HTTP documentados.

Los tres procesos configuran `server.shutdown=graceful` y un límite por fase ajustable con `SHUTDOWN_TIMEOUT`, inicialmente `30s`. El futuro ejecutor de procesos debe conceder un margen de terminación mayor y enviar SIGTERM al proceso Java. La ventana de salud del balanceador y la terminación de contenedores deberán coordinarse durante el diseño de infraestructura.

El smoke local espera readiness y envía SIGTERM al reiniciar/detener sus JAR. Configura `SHUTDOWN_TIMEOUT=5s`, espera hasta 15 segundos y exige evidencia de cierre controlado en logs. Si necesita forzar la terminación, la prueba falla y limpia los demás procesos. El smoke general comprueba cierre sin solicitudes en curso. scripts/drain-smoke.py verifica además que una solicitud activa del gateway termine con 200 durante SIGTERM. El drenaje bajo carga y en los servicios MVC sigue pendiente.

## Imágenes de ejecución y control de calidad

Compilar y probar primero desde backend con `./mvnw clean verify`. Después construir cada imagen con el directorio del módulo como contexto:

```bash
for service in users-service prospects-service api-gateway; do
  docker build --tag "g6-$service:quality" "$service"
done
source .local/env.sh
python3 scripts/container-smoke.py
```

Los Dockerfiles empaquetan el JAR verificado y fijan Temurin JRE 21.0.13 por digest. La compilación Maven se realiza antes del empaquetado. La imagen ejecuta Java mediante ENTRYPOINT exec con UID/GID 10001. Cada .dockerignore admite solo Dockerfile y el JAR esperado. La versión y el digest deben revisarse con los parches del runtime. La fijación de entradas no acredita por sí sola un build idéntico byte a byte ni ausencia de vulnerabilidades.

La prueba de contenedores usa el daemon Docker local, tres imágenes quality, puertos aleatorios en loopback y una red temporal exclusiva. Prueba sistema de archivos de solo lectura y /tmp temporal, readiness, JWT, creación de prospectos y cierre SIGTERM. Monta las claves de prueba individualmente desde un directorio temporal 0700, con archivos legibles por el usuario del contenedor. Las contraseñas se pasan por archivos temporales 0600 y no forman parte de la imagen. Al terminar elimina sus contenedores y su red. Los servicios usan H2 en memoria para esta prueba. El futuro montaje de secretos productivos debe asegurar acceso exclusivo al usuario correspondiente.

`.github/workflows/backend-quality.yml` prepara el mismo recorrido en Ubuntu 24.04 con Java 21. Se activa por PR a develop, push en develop y ejecución manual. Compila y ejecuta pruebas, prepara claves ficticias, comprueba tres procesos, construye imágenes y prueba contenedores. Las acciones checkout/setup-java se fijan a commits oficiales. El permiso del workflow es contents:read. Su ejecución alojada y el comportamiento en forks deben comprobarse cuando se publique el PR autorizado al finalizar la auditoría.

Referencias de mantenimiento: [imagen oficial Temurin](https://github.com/docker-library/docs/blob/master/eclipse-temurin/README.md), [setup-java](https://github.com/actions/setup-java) y [checkout](https://github.com/actions/checkout). Esta preparación genera artefactos locales y controles de calidad. La publicación de imágenes y los procedimientos de despliegue quedan para la infraestructura futura.

## Esquema PostgreSQL inicial y prueba reproducible

Usuarios y prospectos tienen un script `db/migrations/V001__initial.sql` por módulo. Cada script crea el esquema de su servicio y registra versión 1 en schema_version dentro de una transacción. Debe aplicarlo un operador con permisos de esquema, antes de iniciar prod, mediante un cliente psql con `ON_ERROR_STOP=1`. Los scripts fallan al repetir una versión sobre una base ya inicializada. No contienen borrados ni reescriben entidades existentes. No son una conversión automática desde H2 o desde esquemas creados previamente por Hibernate. Esos casos necesitan un plan de adopción revisado, respaldos y pruebas.

La selección y aplicación de migraciones es manual en este incremento. Verificar el archivo y registrar su checksum antes de usarlo en otro entorno. No editar una V001 ya aplicada; los futuros cambios deben usar versiones nuevas. La tabla registra versión/descripción/fecha y no sustituye un runner con validación automática de checksums. La aplicación permanece en validate y no ejecuta los scripts al arrancar.

Desde backend, después de compilar y cargar el setup local:

```bash
source .local/env.sh
python3 scripts/postgres-smoke.py
```

La prueba usa PostgreSQL oficial 17.12 fijado por digest, puerto aleatorio en loopback y datos temporales en Docker local. Crea dos bases y roles separados, aplica las V001, confirma que repetirlas falla conservando la versión registrada y limita permisos runtime a lectura/inserción/actualización. Para disponer de cuentas ficticias carga el seed existente con local, usando validate sobre las tablas migradas, y después inicia los tres JAR con prod.

Comprueba login, permisos, CRUD, anulación, reinicio persistido y dos creaciones concurrentes iguales (201/409 y una sola fila). El contenedor PostgreSQL y todos los JAR temporales se eliminan/detienen al finalizar. No admite una URL de BD externa ni usa bases configuradas del desarrollador. Requiere Docker local accesible y JAVA_HOME/JDK21. No valida TLS de RDS, restauración de backups, adopción de datos previos ni carreras de edición contra anulación.

## Primer administrador en una base productiva nueva

Después de aplicar y revisar el esquema de usuarios, un operador autorizado puede ejecutar el JAR de usuarios con el único argumento `--bootstrap-admin`. Este modo usa JDBC sobre PostgreSQL existente y termina sin iniciar servidor HTTP. No crea tablas ni modifica cuentas existentes. La operación requiere las variables `IDENTITY_JDBC_URL`, `IDENTITY_DB_USER` e `IDENTITY_DB_PASSWORD`, además de:

| Variable | Requisito |
|---|---|
| BOOTSTRAP_ADMIN_USERNAME | Usuario obligatorio, máximo 120 caracteres |
| BOOTSTRAP_ADMIN_EMAIL | Email obligatorio, máximo 160 |
| BOOTSTRAP_ADMIN_DISPLAY_NAME | Nombre obligatorio, máximo 120 |
| BOOTSTRAP_ADMIN_PASSWORD | Secreto externo, mínimo 12 caracteres y máximo 72 bytes UTF-8 |
| BOOTSTRAP_ADMIN_SITES | CSV opcional, máximo 255; cada sede no vacía y máximo 120; omisión usa site-1 como la creación existente |

Inyectar las credenciales mediante el mecanismo seguro del entorno. No poner contraseñas en argumentos, scripts versionados o historial. Comprobar el destino JDBC y la identidad de la base antes de ejecutar:

```bash
java -jar users-service/target/users-service-0.0.1-SNAPSHOT.jar --bootstrap-admin
```

El modo acepta configuración exclusivamente por variables de entorno y rechaza argumentos adicionales. El proceso devuelve 0 al crear el administrador activo y 1 ante configuración inválida, error de acceso o cualquier cuenta ya existente. Los mensajes son genéricos y no imprimen credenciales ni excepciones JDBC. La contraseña usa el mismo BCrypt de la autenticación existente y el rol se fija en ADMIN.

Un bloqueo transaccional exclusivo de la tabla usuarios serializa dos bootstraps simultáneos, con espera máxima de bloqueo de 10 segundos. Solo una ejecución puede crear la primera cuenta. La conexión requiere permisos de lectura, inserción y bloqueo, sin DDL. Después de usarlo, retirar las variables BOOTSTRAP_ADMIN_* del entorno operativo. El arranque habitual del servicio no ejecuta bootstrap. Para una base con cuentas existentes, utilizar el procedimiento administrativo/recovery autorizado, dado que este modo rechaza toda repetición.

La prueba PostgreSQL local valida contraseña/email inválidos sin crear cuentas, dos ejecuciones simultáneas con un solo éxito, rechazo posterior y login ADMIN real por el gateway. Sus cuentas y bases son ficticias y temporales.

## Límite local de intentos de login

Gateway limita POST /api/v1/auth/login por dirección del peer, con una ventana fija por instancia. LOGIN_RATE_LIMIT_REQUESTS admite 60 intentos por defecto, LOGIN_RATE_LIMIT_WINDOW_SECONDS fija 60 segundos y LOGIN_RATE_LIMIT_MAX_CLIENTS acota el mapa a 10000 direcciones. Se cuentan peticiones exitosas e inválidas, sin leer credenciales ni conservarlas. Los límites deben ser positivos; una configuración inválida impide el arranque.

Al agotar la cuota devuelve 429, code LOGIN_RATE_LIMITED, Retry-After en segundos y Cache-Control no-store. Rutas codificadas y parámetros de segmento se comparan con la semántica de path de Spring. El mapa sincroniza comprobación/incremento; al alcanzar su capacidad recupera entradas vencidas y rechaza nuevas direcciones si sigue lleno. El bloqueo se libera al renovar la ventana. No hay bloqueo persistente de cuentas.

El gateway fija forward-headers-strategy=none y el filtro ignora X-Forwarded-For aportado por el cliente. Detrás de ALB/proxy, varias personas pueden compartir la cuota del peer. Antes de exponer el entorno deben acordarse límites en el borde y una estrategia de identidad IP confiable con red restringida. No cambiar la estrategia de forwarded headers para confiar en cabeceras arbitrarias. Los contadores no se comparten entre réplicas y se pierden al reiniciar. El acceso directo al login de usuarios debe quedar restringido a la red interna; este filtro protege la entrada por gateway.

Los valores iniciales son una protección local básica y requieren dimensionamiento por tráfico real. Los ataques distribuidos y las cuotas globales requieren controles adicionales del entorno. La configuración de esos controles permanece pendiente y no se aprovisionó infraestructura.
