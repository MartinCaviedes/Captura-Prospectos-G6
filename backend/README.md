# Backend de captación

Base funcional mínima, sin integración Android ni despliegue. Continúa el servicio de usuarios de Jeremy (`a7b1675`) y los esqueletos de `9cd979b`, ya integrados en `develop` mediante PR #18. No reemplaza autenticación ni entidades de usuarios.

| Servicio | Puerto predeterminado | Responsabilidad |
|---|---|---|
| api-gateway | 8080 | Entrada del cliente, validación JWT y rutas a URLs fijas configurables. Sin BD ni negocio. |
| users-service | 8081 | Usuarios JPA, credenciales BCrypt, roles, login y JWT. Implementación existente de Jeremy. |
| prospects-service | 8082 | Prospectos persistidos, propiedad, sedes, edición básica y anulación auditada. |

Java 21, Spring Boot 3.5.7, Maven Wrapper 3.9.11. Gateway WebFlux existente; JPA y H2/PostgreSQL para ambos servicios de negocio. Cada servicio posee sus entidades/repositorios y su base. Prospectos no consulta usuarios por petición ni usa claves foráneas entre servicios.

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
| `SPRING_PROFILES_ACTIVE` | Setup usa `local`; el servicio de Jeremy actualmente no limita login/seed a ese perfil. |

Ambos servicios usan H2 **en memoria** por defecto, usuario `sa` sin contraseña; los datos se pierden al detenerlos. Para conservar datos locales, configurar bases distintas:

```bash
export IDENTITY_JDBC_URL='jdbc:h2:file:./.local/identity;MODE=PostgreSQL'
export PROSPECTS_JDBC_URL='jdbc:h2:file:./.local/prospects;MODE=PostgreSQL'
```

Para PostgreSQL usar `jdbc:postgresql://HOST:5432/BASE` con bases/usuarios separados y contraseñas externas. El driver ya está incluido; no se requiere instalar PostgreSQL para probar H2. La prueba funcional usa H2, no acredita PostgreSQL ni migraciones productivas. `ddl-auto=update` conserva la convención del backend actual; migraciones revisadas quedan pendientes.

El seed de Jeremy conserva sus cuentas y UUID determinísticos; `captor-a` pertenece a `site-1`, `captor-b` a `site-2`, supervisor y administrador a ambas. El código existente tiene contraseñas demo predeterminadas: usar siempre el setup que las sobrescribe. Las contraseñas de cuentas ya persistidas no se cambian al regenerar variables. No utilizar el seed como aprovisionamiento productivo.

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

Sin integración Android, despliegue, métricas, catálogos, asignación real a eventos, sincronización, idempotencia, reconciliación, correcciones supervisadas completas, refresh/revocación ni catálogo de consentimientos. `eventId` es un UUID recibido; no se acredita existencia/asignación del evento. `siteIds` del JWT restringe sedes, sin llamadas a usuarios. El consentimiento mínimo no sustituye el modelo completo/evidencia offline propuesto. No Swagger/OpenAPI generado, discovery, Docker, Config Server, mensajería ni infraestructura adicional. El siguiente trabajo del equipo debe completar contratos y permisos de eventos antes de conectar Android.
