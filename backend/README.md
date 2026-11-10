# Esqueletos de backend · Spring Boot

Base para desarrollar la incorporación del backend solicitada para la entrega 3. No representa la entrega terminada ni conecta todavía Android. Los documentos históricos llaman E2 al incremento remoto; esta base no cambia esa numeración ni da por cumplidos sus requisitos.

## División recomendada

| Aplicación | Puerto | Responsabilidad |
|---|---|---|
| `api-gateway` | 8080 | Punto de entrada único, validación inicial del JWT y enrutamiento. |
| `users-service` | 8081 | Usuarios, credenciales, roles generales y emisión de JWT. |
| `prospects-service` | 8082 | Captados/inscripciones, eventos, asignaciones y permisos por propietario o sede. |

Es una división coherente con `docs/API_CONTRACTS.md` y `docs/ENVIRONMENTS.md`: identidad cambia por razones distintas a captación. Administración de cuentas pertenece a usuarios; supervisión de captados pertenece a prospectos. Crear un microservicio genérico de «admin» mezclaría ambas responsabilidades. Eventos y catálogos pueden permanecer dentro de prospectos por ahora.

Las tres aplicaciones se construyen desde un agregador Maven, pero generan JAR y procesos independientes. El gateway no contiene reglas de negocio ni accede a bases de datos: enruta `/api/v1/auth/**` y `/api/v1/me` a usuarios. Envía a prospectos las familias de rutas ya propuestas en `docs/API_CONTRACTS.md`: asignaciones, catálogo, eventos, inscripciones y reportes, además de su ruta técnica de estado. Los servicios de negocio no comparten código de dominio ni necesitan llamadas entre sí para validar cada petición. Cuando incorporen persistencia, cada servicio debe ser dueño de sus tablas/base: sin repositorios compartidos ni claves foráneas entre servicios. PostgreSQL es la continuidad propuesta en la documentación; su instalación, entidades y migraciones quedan para una tarea posterior.

## Qué incluye

- Java 21, Spring Boot 3.5.7 y Maven Wrapper 3.9.11. Usar un **JDK**, no solamente un JRE. Android conserva su configuración JDK 25 independiente.
- Spring Web, WebFlux Gateway, Validation, Security y Resource Server; Spring Security verifica los JWT con sus componentes estándar.
- JWT RS256 de 15 minutos, con validación de firma, emisor, audiencia y vigencia. Tolerancia de reloj estándar de Spring Security: 60 segundos.
- Login exclusivamente en perfil `local`, con dos cuentas en memoria y contraseñas BCrypt generadas por el setup. Las cuentas se reconstruyen al reiniciar.
- Paquetes `controller`, `service`, `repository`, `model` y `security`. Los paquetes de negocio pendientes se conservan con `package-info.java`; no contienen CRUD ficticio.
- Pruebas de login, rutas del gateway, 401, 403, roles, firma incorrecta, vencimiento, audiencia y emisor.

No hay Swagger, descubrimiento de servicios, balanceador dinámico ni infraestructura adicional. El gateway usa URLs configurables y conserva el encabezado `Authorization`; usuarios y prospectos vuelven a validar el token como defensa en profundidad. No se han implementado CRUD, persistencia, refresh/logout revocable, asignaciones, idempotencia, sincronización ni integración Android. El `sub` local usa el nombre de cuenta de prueba; al persistir usuarios debe pasar a un UUID estable. `ADMIN` administra cuentas; `CAPTADOR` es el perfil de captura. Los permisos de supervisor por sede se implementarán en prospectos, además del JWT.

## Arranque local

Requisitos: JDK 21 o superior, Bash y OpenSSL. En Windows usar Git Bash/WSL para preparar claves, o generar el par RSA con OpenSSL y configurar las mismas variables en PowerShell; Maven también dispone de `mvnw.cmd`.

Desde `backend/`:

```bash
bash scripts/prepare-local.sh
source .local/env.sh
./mvnw clean verify
```

El script genera claves y contraseñas en `.local/`, excluido de Git, con permisos restringidos. Se puede repetir sin reemplazar archivos existentes. No compartir ni subir ese directorio. Cada integrante genera su configuración. Las contraseñas están disponibles localmente en `LOCAL_ADMIN_PASSWORD` y `LOCAL_CAPTOR_PASSWORD`; no se imprimen en el setup.

En tres terminales, desde `backend/`, cargando las variables en todas:

```bash
# Terminal 1: punto de entrada de Android y otros clientes.
source .local/env.sh
unset JWT_PRIVATE_KEY LOCAL_ADMIN_PASSWORD LOCAL_CAPTOR_PASSWORD
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

```bash
# Terminal 2
source .local/env.sh
java -jar users-service/target/users-service-0.0.1-SNAPSHOT.jar
```

```bash
# Terminal 3: prospectos solo recibe la clave pública.
source .local/env.sh
unset JWT_PRIVATE_KEY LOCAL_ADMIN_PASSWORD LOCAL_CAPTOR_PASSWORD
java -jar prospects-service/target/prospects-service-0.0.1-SNAPSHOT.jar
```

También se puede desarrollar cada aplicación desde el IDE o con `./mvnw -pl api-gateway spring-boot:run`, `./mvnw -pl users-service spring-boot:run` y `./mvnw -pl prospects-service spring-boot:run`. Spring Boot no crea un backend productivo por arrancar sin perfil local: el login solo existe en `local` y aún debe implementarse autenticación con usuarios persistidos.

## Rutas disponibles

| Aplicación | Método/ruta | Acceso |
|---|---|---|
| Las tres | `GET /actuator/health` | Público, solo estado general del proceso consultado. |
| Gateway | `POST /api/v1/auth/login` | Público; se reenvía a usuarios. |
| Gateway | `GET /api/v1/me` | JWT válido; se reenvía a usuarios. |
| Gateway | Rutas de prospectos descritas en `docs/API_CONTRACTS.md` | JWT válido; se reenvían a prospectos. |
| Usuarios | `POST /api/v1/auth/login` | Público, solo perfil `local`. JSON `username`, `password`; devuelve `accessToken`, `tokenType`, `expiresIn` en segundos. |
| Usuarios | `GET /api/v1/me` | JWT válido. Devuelve sujeto y roles del token. |
| Prospectos | `GET /api/v1/prospects/status` | JWT válido. Ruta técnica para verificar acceso; no lista captados. |
| Ambas | `GET /api/v1/admin/status` | Rol `ADMIN`. Ruta técnica para comprobar 403/200. |

Las cuentas locales son `demo-admin` y `demo-captor`. Un token emitido por usuarios funciona en el gateway y en prospectos con la misma clave pública, emisor y audiencia. Enviar `Authorization: Bearer <accessToken>`; credenciales inválidas producen 401, campos inválidos 400 y un captador que accede a una ruta administrativa recibe 403. No registrar contraseñas ni tokens en logs. Sin cookies de autenticación: la API es stateless y desactiva CSRF para este flujo Bearer. El gateway elimina cualquier encabezado `Cookie` antes de reenviar. No hay CORS abierto; Android nativo no lo necesita.

Prueba rápida de salud y acceso anónimo:

```bash
curl --fail http://127.0.0.1:8080/actuator/health
curl --fail http://127.0.0.1:8081/actuator/health
curl --fail http://127.0.0.1:8082/actuator/health
curl -i http://127.0.0.1:8080/api/v1/prospects/status # Debe devolver 401
```

Los contratos funcionales de `/registrations` siguen pendientes; las rutas `status` son del esqueleto. No se crea OpenAPI ni Swagger para este alcance. Antes del CRUD, acuerden ejemplos JSON y reglas en documentación simple. Para un entorno compartido se requiere HTTPS, claves fuera del repositorio y una política de intentos de login; el perfil local y sus cuentas no son una configuración de despliegue.

## Configuración

| Variable | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE=local` | Habilita las cuentas y el login de desarrollo. |
| `JWT_PRIVATE_KEY` | Recurso PEM PKCS#8, solo usuarios. |
| `JWT_PUBLIC_KEY` | Recurso PEM público X.509 en las tres aplicaciones. |
| `JWT_ISSUER` | Predeterminado `captura-g6-users`. Igual en las tres. |
| `JWT_AUDIENCE` | Predeterminado `captura-g6-api`. Igual en las tres. |
| `LOCAL_ADMIN_PASSWORD`, `LOCAL_CAPTOR_PASSWORD` | Contraseñas solo del perfil local en usuarios. |
| `USERS_SERVICE_URL` | Destino del gateway; por defecto `http://127.0.0.1:8081`. |
| `PROSPECTS_SERVICE_URL` | Destino del gateway; por defecto `http://127.0.0.1:8082`. |
| `PORT` | Sobrescribe el puerto de cada proceso por separado. |

## Reparto del trabajo siguiente

1. Usuarios: modelo de cuenta/roles, repositorio y autenticación persistida; reemplazar el proveedor en memoria manteniendo el contrato JWT.
2. Prospectos: modelo de inscripción y consentimiento, repositorio, validaciones y CRUD según contratos existentes. Derivar captador del JWT; verificar propietario/asignación en el servidor.
3. Gateway: mantener solo rutas, autenticación transversal y observabilidad mínima; no trasladar allí validaciones de usuarios o prospectos.
4. En conjunto: acordar UUID de usuario y roles, errores y permisos; después integrar Android usando el gateway como URL base. No enviar las credenciales demo de Android al backend como si fueran cuentas reales.

Los tests del esqueleto validan seguridad e inicio de Spring. No acreditan persistencia ni autorización de datos que todavía no existe.
