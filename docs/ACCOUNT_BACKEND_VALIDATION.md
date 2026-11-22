# Cuentas E2 · implementación y validación del backend

21/11/2026 · incremento backend preparado en feature/e2-cuentas-backend, desde 97e6e0e más los contratos preparados en la sesión. Candidato sin commit/PR/merge publicado y sin aprobación docente atribuida. Su continuación Android está en [ACCOUNT_ANDROID_VALIDATION](ACCOUNT_ANDROID_VALIDATION.md), conservando el trabajo previo en feature/e2-cuentas-android. Las referencias a Android pendiente inferiores describen el corte anterior del backend.

## Comportamiento implementado

- Registro público remoto crea PENDING, sin JWT ni sedes y sin aceptar campos privilegiados. Correo/usuario normalizados; BCrypt con máximo 72 bytes UTF-8, sin truncar. Login comprueba contraseña antes de revelar el estado de cuenta.
- ADMIN activo consulta cuentas paginadas, detalle y catálogo de sedes. Cambia estado/rol/sedes mediante PATCH access con expectedVersion. GET /me consulta datos actuales de BD y no devuelve hash/contraseña.
- CAPTADOR/SUPERVISOR no administran cuentas. Las rutas administrativas comprueban tanto el JWT como la identidad y estado/rol actual del actor en BD. Todo ADMIN está protegido frente a modificaciones por estas rutas, incluida la propia cuenta.
- Bloqueo de fila y @Version evitan sobrescrituras. Un no-op con versión vigente conserva versión y no duplica auditoría. Una versión obsoleta devuelve 409. Cambio y auditoría ocurren en la misma transacción.
- Registro/login comparten rate-limit por peer en gateway. Error 429 usa RATE_LIMITED con Retry-After. Se mantiene el límite local por instancia: no es un control distribuido ni una garantía tras proxies. Los upstreams deben permanecer privados.
- Fuente de asignaciones: catálogo versionado de ocho sedes en users-service/src/main/resources/catalog/sites.json, alineado con el snapshot Android inspeccionado. GET /admin/sites permite construir el selector sin duplicar validación en el cliente. No se consulta una API institucional.

## Compatibilidad y migración

V001 permanece intacta. V002 añade estado, versión, fechas, auditoría e índices de unicidad insensibles a mayúsculas; deriva ACTIVE/DISABLED desde activo y conserva datos existentes. No hay reset ni conversión automática desde H2/Room. Usuarios requiere V001 y V002 antes de iniciar prod/validate; prospectos conserva V001. Aplicación manual con rol migrador, siguiendo backend/README.md.

La migración falla transaccionalmente si encuentra cuentas ambiguas/duplicadas por mayúsculas o cuentas operativas sin asignaciones válidas. Requieren revisión de los datos originales; no se fusionan ni se asignan sedes inventadas. createdAt/updatedAt de cuentas históricas registran el momento de migración, no su fecha real de creación. Los IDs site-1…site-8 de V002 son el snapshot F2027 de esta versión; cambios futuros de catálogo necesitan revisar su evolución.

GET users cambia de array a página; GET me y respuestas administrativas usan UserSummary (id, role, status, version, fechas). Login conserva userId/rol/roles para no romper innecesariamente el cliente. PATCH estado exige expectedVersion y no aprueba pendientes; crear usuarios por HTTP no admite ADMIN ni username con @. El bootstrap sigue siendo el aprovisionamiento de la primera cuenta ADMIN. Estas modificaciones requieren adaptar clientes externos/Android; no se afirma compatibilidad completa con el registro previo.

## Evidencia ejecutada

| Control | Resultado |
|---|---|
| backend/mvnw.cmd -B verify, JDK 21.0.12.1 | BUILD SUCCESS; 46 pruebas, 0 fallos/errores |
| Nuevas pruebas de cuentas | 12 métodos HTTP/BD: pendientes, credenciales, campos privilegiados, BCrypt UTF-8, aprobación, no-op, permisos, sedes, rutas antiguas, protección ADMIN, duplicados/aprobaciones concurrentes, correo de 160 caracteres y paginación |
| Gateway | Registro público enrutado al upstream; login/registro comparten cuota, incluidas variantes codificadas y con parámetros de segmento |
| account-postgres-smoke.py | V001→V002 sobre cuentas ficticias, conservación de datos y rollback de migración; users/gateway reales, PostgreSQL 17.12, prod/validate, bootstrap, aprobación concurrente 200/409 con una auditoría, login, reinicio y desactivación |
| OpenAPI y fixtures | 10 operaciones; 41 expectativas estructurales, 15 ejemplos de esquema válidos. No equivale a ejecutar todas las fixtures contra HTTP |
| git diff --check y compilación Python de scripts | Sin errores |

Las pruebas H2 no se presentan como validación PostgreSQL. La prueba PostgreSQL utiliza contenedor efímero con nombre g6-accounts-*, bases y roles de prueba, claves generadas temporalmente y dos JAR locales. No se conectó a bases compartidas/reales ni AWS; el contenedor y los procesos se limpiaron al terminar. Reiniciar el JAR comprueba persistencia, no cierre graceful en Windows. Logs locales en Residuos ChatGPT/account-backend-verify.log y account-postgres-smoke.log, fuera del repositorio.

El primer recorrido real encontró respuestas de denegación en codificación incorrecta; se corrigió el writer a UTF-8 y se repitió la suite y la prueba HTTP. La primera fixture de correo largo excedía el límite de 64 caracteres de su parte local: se sustituyó por un correo válido de 160 caracteres. No se rebajó la validación de email para hacer pasar la prueba.

## Límites y siguiente paso

- Desactivar no revoca JWT ya emitidos en prospectos/gateway. Vigencia 900 s más tolerancia del validador (60 s con la configuración actual); /me refleja estado actual y nuevos logins se rechazan. El cliente debe revalidar antes de transmitir. No hay refresh ni recuperación de contraseña.
- Acceso local de hasta 12 h, conservación de colas y supresión del fallback tras rechazo remoto siguen pendientes en Android. Este incremento no prueba esos comportamientos ni modifica Room.
- Quedan UI administrativa, registro remoto en Android, presentación de estados pendiente/desactivado y adaptación de DTOs. La pantalla actual todavía permite el registro local anterior: no distribuir esta rama como entrega integrada terminada.
- Sincronización completa, eventos remotos, preferencias/ubicación de prospectos, API externa, personalización y APK release firmada siguen en etapas separadas. No se midió cobertura JaCoCo ni se construyeron imágenes nuevas o desplegó infraestructura.

Fuentes: [contrato funcional](ACCOUNT_ACCESS_CONTRACT.md), [OpenAPI](openapi/accounts.openapi.json), [fixtures](fixtures/accounts/README.md), [ejecución backend](../backend/README.md).
