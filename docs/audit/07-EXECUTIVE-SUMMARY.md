# Resumen ejecutivo de auditoría backend

Revisión del 17/11/2026 sobre develop a489882 y la rama audit/backend-produccion. Alcance de aplicación, scripts, imágenes y documentación de backend. Se confirmó que la implementación anterior entró mediante PR #19. Esta entrega propone correcciones adicionales mediante PR a develop.

## Resultado

**Recomendación actual: no listo para producción.** La base local queda más segura y reproducible, con perfiles productivos, esquemas iniciales PostgreSQL, contenedores y controles de calidad. La puesta en producción exige decisiones de soporte de Spring, control de abuso en el borde, gestión de datos existentes, recuperación, TLS y validación del entorno real. Esas condiciones están abiertas y no se sustituyen con los resultados locales.

## Correcciones realizadas

- Usuarios evita seed demo en prod y exige contraseñas externas. El JWT sin audiencia se rechaza con 401. PATCH de estado exige activo y creación limita sedes al tamaño de columna.
- El primer ADMIN se aprovisiona por CLI explícita sobre una base vacía, con BCrypt y bloqueo transaccional. Rechaza datos inválidos, repetición y carreras entre dos ejecuciones.
- Gateway acota tiempos de conexión/respuesta, comunica 503/504 y limita login localmente con 429/Retry-After y memoria acotada. Ignora forwarded headers arbitrarios.
- Los tres servicios exponen probes mínimas y cierran con SIGTERM. El gateway completó una petición activa durante el cierre.
- Los servicios JPA usan validate en prod y scripts V001 manuales. Se probó PostgreSQL 17.12, roles separados, CRUD por gateway, persistencia y duplicados concurrentes.
- Se actualizaron Boot/Cloud y parches compatibles de bibliotecas. Se conservó Java 21 y los contratos válidos existentes. Se silenció el logger SQL que exponía RUT/evento en duplicados.
- Se prepararon imágenes no root con runtime por digest, contextos restringidos y workflow de calidad con permisos mínimos y acciones por SHA.

## Pruebas y revisión

El informe 04 consolida los comandos, resultados y límites. El recorrido incluye suite JUnit, empaquetado independiente, tres JAR, PostgreSQL temporal, bootstrap/duplicados concurrentes, drenaje gateway y tres contenedores con filesystem de solo lectura. La revisión independiente del diff no identificó defectos funcionales importantes y señaló documentación contradictoria que se corrigió.

El análisis SCA conserva los avisos completos y las condiciones de uso evaluadas con fuentes primarias. La actualización redujo avisos Java HIGH a cero con la base consultada. Persisten asociaciones CRITICAL de Framework con precondiciones no encontradas en el código y un aviso MEDIUM de log4j-api. Las bases y la detección tienen límites; no se declara explotación ni seguridad total.

## Riesgos y decisiones pendientes

| Condición | Acción antes de producción |
|---|---|
| Boot 3.5 y Cloud 2025.0 sin soporte OSS | Aprobar soporte o migración mayor y repetir validaciones |
| Límite login por peer/instancia | Acordar identidad IP confiable, límites de borde y comportamiento con proxies/réplicas |
| V001 manual sobre base nueva | Revisar adopción de esquemas/datos previos, integridad de migraciones y backups/restauración |
| TLS/secretos del entorno | Implementar entrega PEM e IAM revisados, verificar CA y rotación |
| Concurrencia/drenaje | Ampliar edición/anulación, carga y cierre MVC/retirada de tráfico |
| Observabilidad | Completar correlación, métricas/alertas y auditoría de otros errores/logs |
| Semántica de tokens | Decidir ventana aceptable tras desactivar una cuenta |
| API administrativa | Planificar paginación/errores conservando contratos y permisos acordados |

## Preparación AWS y entrega

Los documentos 05 y 06 evalúan ECS Fargate, ALB, ECR, PostgreSQL administrado, secretos, logs e IAM. La propuesta requiere revisión y dimensionamiento. Todas las ejecuciones de base y contenedores de esta auditoría fueron locales con datos ficticios. No se realizaron operaciones de infraestructura cloud ni se provisionaron recursos externos.

El workflow preparado compila, prueba y construye imágenes. No publica imágenes ni despliega. Su ejecución alojada se comprobará según los permisos de GitHub al entregar el PR. El usuario autorizó commit/push/PR hacia develop al finalizar, manteniendo la prohibición de merge. Los archivos 01–07 y el checkpoint conservan los hechos que otro ingeniero necesita para continuar.
