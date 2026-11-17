# Prerrequisitos de despliegue futuro

Guía documental del 17/11/2026. Los pasos de infraestructura requieren una tarea futura autorizada. Durante esta auditoría solo se ejecutaron compilación, pruebas y Docker local.

## 1. Revisar decisiones y bloqueadores

Aprobar el diseño de ejecución propuesto en 05-AWS-READINESS.md, incluida la elección de una tarea conjunta o tareas independientes. Definir región, presupuesto, responsable de operación, disponibilidad requerida, pérdida de datos tolerable y tiempos de recuperación. Confirmar la ventana permitida entre desactivar usuario y expirar su token.

Resolver o aceptar explícitamente los riesgos de los informes 02 y 03. Completar análisis de vulnerabilidades Java/runtime y revisar cambios de versiones. Implementar y validar protección del login frente a abuso, observabilidad y migraciones. Revisar la evidencia local de PostgreSQL, creación duplicada y bootstrap concurrentes; ampliar con edición/anulación y carga. Estos elementos condicionan la aprobación productiva.

## 2. Obtener artefactos verificables

En un entorno local/CI con JDK 21, Python 3, Bash, OpenSSL y Docker, ejecutar desde backend:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
bash scripts/prepare-local.sh
source .local/env.sh
python3 scripts/smoke-test.py
for service in users-service prospects-service api-gateway; do
  docker build --tag "g6-$service:quality" "$service"
done
python3 scripts/container-smoke.py
```

Revisar el workflow de calidad y comprobar su ejecución cuando se publique el cambio autorizado. Guardar resultados, versión de JDK/Maven, commit, artefactos y digest de cada imagen. Preparar identificación de release consistente. El nombre Maven actual sigue siendo 0.0.1-SNAPSHOT, por lo que no basta como identificador único de entrega.

## 3. Preparar persistencia y recuperación

Elegir una versión PostgreSQL soportada por el equipo y validarla localmente con el driver 42.7.12. Revisar los scripts V001 existentes de usuarios/prospectos y preparar versiones nuevas para cualquier evolución de datos. Probar la creación desde cero, evolución con datos ficticios previos, backup/restauración y plan de reversión. La app productiva no debe aprovisionar tablas durante el arranque.

En la fase futura de infraestructura, preparar bases/roles separados para cada servicio. El usuario runtime debe disponer de permisos mínimos de operación. El rol de migraciones se usa fuera de las tareas de aplicación. Verificar conectividad TLS, CA, DNS, reglas de red y configuración de pool para el número previsto de réplicas. Conservar evidencia de que un fallo de BD se manifiesta en readiness y de que liveness permite recuperación sin ocultar el fallo.

## 4. Preparar configuración y secretos

Usar backend/.env.example como inventario, sin copiar secretos reales al repositorio. Asignar variables por contenedor:

| Proceso | Valores necesarios |
|---|---|
| Todos | SPRING_PROFILES_ACTIVE=prod, PORT, JWT_PUBLIC_KEY, JWT_ISSUER, JWT_AUDIENCE, SHUTDOWN_TIMEOUT |
| Usuarios | JWT_PRIVATE_KEY, IDENTITY_JDBC_URL, IDENTITY_DB_USER, IDENTITY_DB_PASSWORD |
| Prospectos | PROSPECTS_JDBC_URL, PROSPECTS_DB_USER, PROSPECTS_DB_PASSWORD |
| Gateway | USERS_SERVICE_URL, PROSPECTS_SERVICE_URL, GATEWAY_CONNECT_TIMEOUT_MS, GATEWAY_RESPONSE_TIMEOUT |

Mantener los puertos distintos si los tres contenedores comparten tarea. Los valores iniciales son gateway 8080, usuarios 8081, prospectos 8082. Los destinos localhost son válidos solo para una tarea conjunta. Diseñar destinos privados estables para tareas separadas.

Preparar entrega de RSA pública a los tres y privada exclusivamente a usuarios. Probar materialización PEM en volumen efímero, permisos de UID 10001 y orden de arranque del inicializador si se selecciona ese patrón. No asumir que inyectar PEM directamente como JWT_PRIVATE_KEY funciona. Probar rotación de claves y contraseñas con nuevos arranques y tokens existentes.

El seed demo queda excluido de prod. Aprovisionar la primera cuenta administradora mediante un proceso revisado con hash BCrypt compatible. El modo explícito --bootstrap-admin ya implementa y prueba ese procedimiento sobre PostgreSQL local temporal; consultar backend/README.md y ejecutar únicamente después de revisar el destino y los permisos. Si una BD procede de desarrollo, revisar y retirar o deshabilitar sus cuentas demo mediante un cambio autorizado y recuperable. El perfil prod no elimina cuentas existentes.

## 5. Preparar infraestructura en una ejecución futura

Después de aprobar los bloqueadores, otro ingeniero deberá preparar red y salida a dependencias, registro de imágenes, cómputo, base, entrada HTTPS, IAM, secretos y logs. Registrar decisiones de seguridad y costes antes de provisionar. Definir retención y permisos de ECR/CloudWatch, gestión de certificados y backups. El documento 05 justifica las opciones, sin identificar recursos existentes.

En el diseño inicial ALB solo expone gateway. Bloquear ingreso directo a usuarios/prospectos desde clientes. Usar el target type ip requerido para Fargate. Coordinar health checks, política de reemplazo, margen de arranque Java y retirada de tráfico. Aprobar reglas de acceso para consultas administrativas y sedes.

Definir permisos por ARN para extracción, logs y secretos. Separar rol de ejecución de necesidades de la aplicación o inicializador. Una tarea conjunta comparte rol de tarea entre contenedores, aspecto que debe aprobarse como límite de confianza.

## 6. Validar en ambiente de ensayo autorizado

Ejecutar una prueba con imágenes de release y PostgreSQL, secretos montados, perfil prod y TLS reales del ambiente de ensayo. Comprobar login válido/inválido, JWT alterado/expirado, aislamiento por captador/sede, duplicados, creación/edición/anulación, reinicio, eventos de disponibilidad y cierre con peticiones activas.

Probar caídas y recuperación de BD/destinos, timeouts y agotamiento de pool. Evitar reintentos ciegos de escrituras tras timeout. Revisar logs para que no contengan JWT, contraseñas ni datos de prospectos. Confirmar alertas y trazabilidad. Medir recursos antes de fijar capacidad/autoscaling. Registrar diferencias frente a los resultados H2 y PostgreSQL 17.12 locales de esta auditoría.

## 7. Aprobar publicación y recuperación

Preparar un plan de promoción manual de imágenes por digest, estrategia de reemplazo y validaciones de salud. El workflow preparado solo controla calidad. La publicación de imágenes y el despliegue requieren proceso futuro con permisos específicos.

Probar rollback de aplicación y compatibilidad con el esquema desplegado. No utilizar una migración destructiva como mecanismo de reversión. Acordar quién aprueba, quién ejecuta y qué métricas obligan a revertir. Conservar las imágenes previas y un procedimiento de restauración probado.

## Criterio para autorizar producción

El responsable debe revisar los resultados y excepciones de todos los pasos, cerrar los bloqueadores y registrar la aprobación. Actualmente esos requisitos siguen abiertos. La compilación y las pruebas locales se encuentran documentadas en 04-TEST-REPORT.md.

## Condición de exposición del login tras la protección local

La cuota del gateway se configura con LOGIN_RATE_LIMIT_REQUESTS, LOGIN_RATE_LIMIT_WINDOW_SECONDS y LOGIN_RATE_LIMIT_MAX_CLIENTS. Opera por peer y por instancia. Detrás de ALB/proxy puede agrupar clientes y no ofrece una cuota distribuida. Antes de exposición pública, aprobar y probar el límite de borde y la política de identidad IP confiable, mantener usuarios privado y coordinar las réplicas. No desactivar la protección local ni confiar en forwarded headers sin revisar esos efectos.
