# OpenAPI y Swagger UI · E2 candidata

22/11/2026. Documentamos y probamos las rutas implementadas del backend, sin ampliar el alcance funcional del SRS. Swagger UI permite consultar modelos, ejemplos, permisos y respuestas y ejecutar peticiones contra el mismo gateway.

## Uso

Con las claves JWT y las URLs de servicios configuradas como indica [Backend](../backend/README.md), habilitar la documentación en el proceso del gateway:

```powershell
$env:API_DOCS_ENABLED='true'
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

Abrir `http://localhost:8080/docs/index.html` (o `/docs`, que redirige allí). El puerto cambia con `PORT`. La vista de prueba aislada utiliza `http://localhost:18080/docs/index.html`; es temporal y no constituye un despliegue AWS.

1. Abrir Identidad → POST `/auth/login` → Try it out. Utilizar una cuenta habilitada del entorno de prueba.
2. Ejecutar y copiar `accessToken` de la respuesta.
3. En Authorize, pegar únicamente ese token, sin escribir el prefijo Bearer.
4. Probar una ruta permitida para esa cuenta. Swagger utiliza el prefijo `/api/v1` del mismo host/puerto; las sondas de salud usan `/actuator/health`.
5. Logout en Swagger cierra la autorización de la interfaz; POST `/auth/logout` confirma cierre en el cliente, pero no revoca inmediatamente un JWT ya emitido.

Try it out no ejecuta operaciones por abrir la página: debe pulsarse Execute. POST/PUT/PATCH sí escriben en la base del entorno seleccionado. Usar cuentas y datos ficticios en la demostración.

`API_DOCS_ENABLED` está desactivado por defecto, también sin perfil activo. Cuando está desactivado no se registran los recursos `/docs/**` y la seguridad impide acceder incluso con un JWT válido. Habilitar la documentación permite consultar sus GET sin autenticación; no abre las rutas privadas del API ni cambia los roles. Los recursos de Swagger UI 5.35.1 están en un WebJar del gateway, sin CDN ni validador remoto. La autorización de Swagger se mantiene solo en memoria; recargar la página la pierde. Se deshabilitó configuración por query string y el interceptor rechaza peticiones a otros orígenes.

## Contratos y alcance

| Archivo | Contenido |
|---|---|
| [accounts.openapi.json](openapi/accounts.openapi.json) | 10 operaciones: registro, login/logout, cuenta actual y gestión administrativa |
| [prospects.openapi.json](openapi/prospects.openapi.json) | 6 operaciones: creación/lista/consulta/edición/anulación y sincronización |
| [backend.openapi.json](openapi/backend.openapi.json) | Unión de los anteriores más 2 estados técnicos y 3 sondas de salud: 21 operaciones |

Los dos primeros son fuentes editables. `tools/build_backend_contract.py` produce el unificado; `--check` detecta si quedó desactualizado. Maven incluye directamente los JSON de `docs/openapi` al empaquetar el gateway: no se mantiene otra copia manual en resources. No se usa generación automática de rutas mediante Springdoc; conservamos los esquemas OpenAPI 3.1/JSON Schema existentes y sus condiciones de estado/permisos.

Documentamos las diferencias efectivas del código: roles por operación, propietario/sede, anulación lógica, RUT normalizado, paginación por página, `sourceRevision`/`expectedRevision`, ACK idempotente, conflictos y campos de auditoría. El estado local READY/NEEDS_CONTEXT/CANCELLED de sync no equivale al estado ACTIVE/CANCELLED de la ficha remota. `expectedRevision` identifica la revisión de sincronización, no la columna `version` de Prospect.

La API devuelve una ficha mínima en GET prospectos; no entrega el snapshot completo ni implementa reconciliación/descarga de formularios. No documentamos `/events`, `/registrations`, `/assignments`, `/catalog` o `/device-reports` como endpoints disponibles solo porque el gateway tenga reglas de enrutamiento preparadas. BigDataCloud es un proveedor consumido directamente por Android y no una ruta propia del gateway. La ruta interna de diagnóstico `/admin/status` de prospectos queda eclipsada por la de usuarios en el gateway, por lo que se documenta una sola operación pública.

El gateway puede devolver 401 sin cuerpo y 504 sin el formato normalizado de los servicios. Se indica explícitamente, sin prometer un esquema único de errores. Las validaciones de sedes, unicidad, permisos vigentes, BCrypt de máximo 72 bytes UTF-8 y contenido/estado de la base requieren pruebas funcionales; un esquema no las reemplaza. En sync, el servidor verifica que snapshotJson sea un objeto JSON con marcador F2027_1, sin validar exhaustivamente su formulario interno.

## Verificación reproducible

Desde la raíz del repositorio, con Python 3 y las dependencias de herramientas `openapi-spec-validator==0.7.2` y `jsonschema`:

```powershell
python tools/build_backend_contract.py --check
python tools/validate_account_contract.py
python tools/validate_backend_contract.py
```

Los validadores admiten `--deps RUTA` si las bibliotecas están en una carpeta de herramientas. No son dependencias de ejecución de Android/Java. Se verifican especificaciones, ejemplos, seguridad, operationId únicos y correspondencia de las 18 operaciones de controladores con el contrato. El inventario lee el estilo actual de anotaciones Java: si cambia ese estilo también debe revisarse su parser; no es un analizador general de Java.

Desde `backend`:

```powershell
.\mvnw.cmd -Pcoverage verify -B
```

GatewayDocumentationTest comprueba recursos locales, JSON empaquetado y autorización de rutas; ApiGatewayIntegrationTest comprueba que la documentación permanezca cerrada por defecto incluso con JWT válido. La activación de docs no permite consultar prospectos o identidad anónimamente.

Con una fixture **efímera y aislada** en loopback 18080/18081/18082, perfil local, cuentas ficticias `demo-admin`, `captor-a` y `supervisor` y las contraseñas de prueba del procedimiento local:

```powershell
python tools/smoke_openapi.py --isolated-fixture
```

Este script exige puerto 18080 y loopback, crea datos de prueba y valida respuestas contra OpenAPI. No debe dirigirse a una base con datos del equipo. No imprime tokens ni cuerpos si falla la validación. No inicia servicios por sí solo; requiere la fixture ya preparada. El ensayo local utilizó el helper temporal `Residuos ChatGPT/android_account_fixture.py`, con base H2 en memoria, claves efímeras y duración máxima de 30 minutos.

## Resultados observados

- OpenAPI 3.1: cuentas, prospectos y unificado válidos. 20 ejemplos del unificado validados; 41 expectativas de fixtures de cuentas conservadas.
- Inventario: 18 operaciones de controladores y 3 sondas de gateway, sin rutas funcionales futuras añadidas.
- Backend: **60 pruebas aprobadas**, cero fallos/errores. Cobertura de líneas: gateway 114/116 (98,28 %), usuarios 291/345 (84,35 %), prospectos 195/199 (97,99 %). No se repitieron pruebas Android porque no cambió su código.
- HTTP contra servicios reales aislados: todas las 21 operaciones recorridas; respuestas comparadas con sus esquemas, incluyendo administración, CRUD, ACK/replay idéntico y conflicto de replay. No se usan ubicaciones ni fotos reales.
- Navegador: Swagger cargó la especificación y sus grupos; GET salud mediante Try it out / Execute devolvió HTTP 200 desde el gateway correcto. El retorno incluye status=UP; las sondas no acreditan salud de todos los servicios dependientes.
- La prueba manual de foto/apodo en Redmi sigue pendiente. AWS, TLS, APK release firmada y distribución final mantienen seguimiento separado.

## Referencias

- [Configuración oficial de Swagger UI](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/).
- [Swagger UI 5.35.1](https://github.com/swagger-api/swagger-ui/releases/tag/v5.35.1).
