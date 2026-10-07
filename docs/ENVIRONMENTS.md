# Entornos y configuración del producto

Actualizado 06/10/2026. E1 tiene wrapper Gradle, tests y verificadores SQL del host; no existen scripts de backend, recursos cloud ni variables remotas cargadas. [ENTREGAS](ENTREGAS.md) separa el arranque Android/local E1 del entorno con servicios E2. La configuración JDBC/JWT y scripts de backend siguientes pertenecen a E2; no son prerrequisitos de E1-BASE-01.

## Entornos

| Entorno | API/base | Datos | Propósito |
|---|---|---|---|
| e1-local-demo | Android + Room cifrado, sin API | Perfiles y registros ficticios locales identificados | Ejecutar/verificar E1 sin infraestructura remota |
| local | Dos Spring Boot y PostgreSQL locales | Seed ficticio reproducible | Trabajo independiente y respaldo de demostración |
| test | Servicios/base efímeros, sin apuntar a cloud compartida | Fixtures aisladas por ejecución | Tests/CI destructivos sin riesgo para otro integrante |
| integration | Servicios compartidos por definir tras piloto; Neon recomendado | Ficticios compartidos, migraciones revisadas | Prueba conjunta de dos captadores/supervisor |

No hay producción ni debe añadirse un perfil llamado producción que use claves/demo. Build release es un tipo de empaquetado y firma, no prueba de que exista entorno productivo.

Las filas local/test/integration con servicios corresponden a E2. E1 necesita además pruebas unitarias e instrumentadas locales aisladas, sin PostgreSQL. No crear passwords JDBC/JWT ni contratar nube para ejecutar E1. Sus claves de cifrado se generan/protegen en el dispositivo según SECURITY.

## Bases y responsabilidad

Propuesta E2: dos bases PostgreSQL, `identity_db` y `recruitment_db`. Al implementarlas, cada servicio tendrá usuarios runtime/migración distintos y deberá comprobar sus privilegios; no están creadas ni probadas actualmente. Identity administra credenciales, roles generales y refresh; Recruitment administra scopes de sede, asignaciones y captación. No crear foreign keys entre bases ni dar lectura al servicio sobre tablas de credenciales ajenas.

Una base local no se «sincroniza» con otra copiando el historial Git: Git comparte migraciones y seeds; Room sincroniza inscripciones con API; el servidor persiste en PostgreSQL. Son tres mecanismos distintos.

Flyway por servicio. Numeración de migraciones coordinada por tarea antes de integrar; no modificar scripts ya aplicados. Validación Hibernate de esquema, nunca recreación/actualización automática sobre integración. Separar comando de migración privilegiado del proceso runtime.

## Catálogo de configuración propuesto

Los nombres se fijarán en `.env.example` al crear el build. No usar la misma variable `DATABASE_URL` global para dos servicios sin separar sus entornos.

| Nombre | Uso | Secreto |
|---|---|---|
| `IDENTITY_JDBC_URL` | Conexión identidad; sin usuario/password dentro de URL | No por sí sola; metadato operativo |
| `IDENTITY_DB_USER` | Rol runtime identidad | Identificador de acceso; tratar como configuración interna |
| `IDENTITY_DB_PASSWORD` | Password runtime identidad | Sí |
| `RECRUITMENT_JDBC_URL` | Conexión captación | No por sí sola; metadato operativo |
| `RECRUITMENT_DB_USER` | Rol runtime captación | Configuración interna |
| `RECRUITMENT_DB_PASSWORD` | Password runtime captación | Sí |
| `IDENTITY_MIGRATION_*`, `RECRUITMENT_MIGRATION_*` | Credenciales del proceso de migración; expandir a URL/USER/PASSWORD en plantilla | Password sí; fuera de runtime |
| `JWT_PRIVATE_KEY_PATH` | Referencia a archivo externo de identidad | Ruta no; contenido de clave sí |
| `JWT_PUBLIC_KEY_PATH` | Verificación de firma por captación | Clave pública no secreta; integridad necesaria |
| `JWT_ISSUER`, `JWT_AUDIENCE` | Validación de tokens | No |
| `IDENTITY_API_BASE_URL`, `RECRUITMENT_API_BASE_URL` | Configuración Android | No; no incluir tokens en URL |
| `DEMO_SEED_ENABLED` | Carga explícita de fixtures locales | No; false fuera del perfil local-demo |
| `ANDROID_KEYSTORE_PATH`, `ANDROID_KEY_ALIAS` | Firma release | Ruta/alias no equivalen a clave privada |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD` | Firma release | Sí |

No poner JDBC ni clave privada JWT en Android. El access token de usuario se obtiene al hacer login y no es una constante del build. Variables de entorno no garantizan secreto si su contenido se compila dentro del APK.

## Script de preparación a implementar

`setup-local` deberá comprobar JDK/SDK/runtime PostgreSQL, crear configuración excluida si falta, generar claves locales, aplicar migraciones, sembrar fixtures y comprobar salud de servicios. No regenerar claves de un entorno existente sin solicitud explícita. No imprimir secretos en logs persistentes. Preferir entregar credenciales de demo en archivo local protegido y explicar su ubicación.

`verify-local` deberá verificar esquema/configuración, ejecutar tests y recorrido de integración. `reset-demo` deberá verificar que el destino sea una base local ficticia permitida antes de borrar; no permitir apuntarlo a una URL arbitraria de integración.

Docker Compose es una opción propuesta para reproducir PostgreSQL/servicios; si un equipo no puede ejecutarlo, documentar alternativa equivalente de PostgreSQL nativo. No prometer que Docker está instalado ni convertirlo en requisito sin comprobar los equipos. La validación Android puede hacerse en emulador compatible o dispositivo físico con depuración autorizada; registrar el entorno realmente utilizado.

## Migraciones y PR

El PR debe indicar migración nueva, compatibilidad hacia atrás, prueba desde base vacía y desde versión anterior, impacto en seed y plan de recuperación. Aplicación a integration se serializa; nunca dos features hacen cambios manuales a la base compartida para luego «documentarlos». Roles sin DDL deben fallar al intentar ALTER/DROP en una prueba de permisos.

Conservar pool JDBC pequeño y configurar timeouts. Si se usa suspensión de compute cloud, ensayar reconexión y arranque en frío sin marcar registros como perdidos. Cuotas y retención del proveedor se comprueban al crear recursos; no son características garantizadas del producto.

## Criterios de arranque reproducible

Objetivo E1: obtener código identificado → preparar JDK/SDK/versiones Android fijadas → build → instalar demo local → usar GPS/cámara/conectividad y recuperar datos cifrados tras reinicio. Actualmente build y almacenamiento/contexto están comprobados; UI y recursos nativos aún no completan este recorrido. Ningún login remoto o archivo privado del equipo debe ser necesario para ese recorrido. Configuración y datos demo permanecen aislados de integración E2; no se envían automáticamente al añadir servicios.

E2:

1. Otro integrante clona el producto y ejecuta la preparación con instrucciones públicas sin archivos privados del grupo.
2. Se generan credenciales únicamente para su entorno, sin reutilizar las de integración.
3. Android autentica contra API real local, guarda offline y sincroniza al recuperar acceso.
4. Las credenciales privadas de cloud/firma no son necesarias para compilar debug ni ejecutar tests locales.
5. La generación release firmada sí exige la clave de firma externa y un custodio; no inventar una clave nueva para cada actualización del mismo entregable.

El build y su explicación deben ser reproducibles con código, configuración de ejemplo y documentación técnica pública, sin archivos privados del equipo.
