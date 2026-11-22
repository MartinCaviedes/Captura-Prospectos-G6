# Ejemplos de cuentas E2

[cases.json](cases.json) contiene peticiones y respuestas ficticias asociadas a esquemas de [OpenAPI](../../openapi/accounts.openapi.json).

- `validSchema`: resultado esperado de validación estructural, formatos y condiciones JSON Schema.
- `kind=schema`: caso estructural, positivo o negativo.
- `kind=business`: petición estructuralmente válida cuyo rechazo requiere contexto del servidor. expectedStatus/expectedCode son resultados futuros, no respuestas observadas.

## Precondiciones de los escenarios de negocio

| Caso | Preparación requerida para la futura prueba HTTP |
|---|---|
| registro-password-utf8-excesiva | Correo no registrado; 37 caracteres á ocupan 74 bytes UTF-8. Debe rechazar sin truncar. |
| registro-duplicado | Cuenta existente con ese correo/username normalizado; repetir también con mayúsculas y con solicitudes concurrentes. |
| habilitar-sede-inexistente | ADMIN activo, usuario pendiente versión 0 y site-no-existe ausente del catálogo. |
| version-obsoleta | ADMIN activo y usuario objetivo con versión 1; petición expectedVersion=0. |
| captador-administra | Token válido CAPTADOR, objetivo pendiente versión 0 y sede válida; prohibir antes de mutar datos. |
| objetivo-admin | Actor ADMIN activo y objetivo ADMIN con versión coincidente; operación no permitida incluso si es otro ADMIN. |
| login-pendiente | Cuenta PENDING con la contraseña de la fixture; devolver 403 sin token. |
| login-password-incorrecta | Misma cuenta PENDING y clave distinta; devolver 401 sin revelar estado. Repetir con DISABLED e inexistente. |
| login-desactivado | Cuenta DISABLED con contraseña correcta; devolver 403 sin token. |

Los ejemplos `site-1` son referencias ficticias que el escenario debe sembrar como sede habilitada. Ninguna fixture crea ni consulta datos reales.

Pruebas futuras adicionales: dos aprobaciones con igual versión, desactivación concurrente de administradores, HTTP sin token, JWT vencido/alterado, filtro/paginación, PATCH estado intentando eludir permisos, sedes deshabilitadas, registro con fallo de conexión y conservación de capturas. Esos comportamientos no quedan acreditados por validar JSON.
