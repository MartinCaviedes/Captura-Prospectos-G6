# Contratos OpenAPI del backend E2

[backend.openapi.json](backend.openapi.json) reúne 21 operaciones implementadas y se sirve en Swagger UI del gateway. Ver [activación, alcance y validación](../SWAGGER_VALIDATION.md). [accounts.openapi.json](accounts.openapi.json) y [prospects.openapi.json](prospects.openapi.json) son los contratos fuente editables, con OpenAPI 3.1.0 y JSON Schema 2020-12. `tools/build_backend_contract.py` regenera el unificado; `--check` detecta diferencias. Maven empaqueta estos JSON directamente desde esta carpeta.

Alcance de cuentas: registro, login/logout, identidad actual y administración de usuarios; reglas en [ACCOUNT_ACCESS_CONTRACT](../ACCOUNT_ACCESS_CONTRACT.md). Prospectos y sincronización están en el segundo contrato fuente. Diagnóstico y salud se agregan al unificado. Eventos remotos, API externa y Microsoft/Duoc no se declaran endpoints propios implementados. Los ejemplos son ficticios; las contraseñas solo sirven como datos de prueba y el marcador de token no es un JWT válido.

## Cambios de compatibilidad introducidos en cuentas

- Registro público y PATCH access son rutas nuevas.
- GET users pasa de array a página y deja de admitir SUPERVISOR.
- GET me y respuestas administrativas usan UserSummary: estado/versión/fechas y campos explícitos, sin devolver entidades JPA. TokenResponse conserva `rol`/`roles` del login actual; UserSummary usa `role`.
- PATCH estado conserva la ruta, pero añade expectedVersion obligatorio y no permite aprobar solicitudes pendientes. Actualizar consumidores; no se promete compatibilidad con el cuerpo anterior.
- Crear usuario administrativo conserva nombres username/rol/sedesAsignadas, pero ya no permite ADMIN ni sedes por defecto. La primera cuenta ADMIN mantiene bootstrap.
- El username derivado del correo y el login requieren ampliar el límite previo de 120 a 160 también en almacenamiento.
- GET /admin/sites ofrece el catálogo canónico versionado de las ocho sedes para el futuro selector. Backend valida contra ese recurso; el esquema solo verifica forma/cantidad. No hay descubrimiento institucional automático. Username de alta administrativa no admite @, para mantener separado el espacio de correos de autorregistro y prevenir identificadores ambiguos.

## Límites de validación

El esquema rechaza propiedades extra, permisos en registro, estados/roles inválidos, listas duplicadas, ausencia de versión y activación sin asignaciones. Las condiciones del estado y null están expresadas como JSON Schema, no solo en prosa.

No verifica: correo único en BD, contraseña BCrypt de máximo 72 bytes UTF-8, sedes existentes/habilitadas, identidad del actor, coincidencia `rol`/`roles`, estado actual de cuenta, versión vigente, protección de administradores, transacciones ni respuestas HTTP. Estos controles requieren pruebas del backend. `maxLength` cuenta caracteres y no sustituye el límite de bytes.

La ventana de JWT online de hasta 14400 segundos (4 horas) no define la vigencia offline. La [sesión recordada](../REMEMBERED_SESSION.md) permite recuperar el JWT cifrado sin renovar su plazo y exige /me antes de autorizar. La sesión offline y sus límites están en [su validación](../OFFLINE_SESSION_VALIDATION.md), incluida la exigencia de login online tras reiniciar el teléfono. No se extiende un token vencido para transmitir ni se permite fallback tras rechazo del servidor. No hay revocación inmediata ni refresh implementado.

## Comprobación reproducible

Con Python 3 y `openapi-spec-validator==0.7.2` en un entorno de herramientas:

```text
python tools/validate_account_contract.py
python tools/build_backend_contract.py --check
python tools/validate_backend_contract.py
```

Si se instaló el validador fuera del entorno Python, pasar `--deps RUTA_A_DEPENDENCIAS`. No es una dependencia de Android ni de los microservicios. El script comprueba la especificación, ejemplos, expectativas de las [fixtures](../fixtures/accounts/cases.json), seguridad declarada e identificadores de operación. No llama a servidores y no representa pruebas funcionales aprobadas.
