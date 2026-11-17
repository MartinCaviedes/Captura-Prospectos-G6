# Evaluación de preparación AWS

Revisión documental del 17/11/2026 sobre a489882 y cambios locales de auditoría. La recomendación requiere revisión del equipo antes de diseñar infraestructura. Ninguna cuenta, región, red, base o recurso AWS se inspeccionó ni modificó.

## Estado evaluado

La aplicación tiene tres JAR e imágenes independientes, configuración externa, JWT RSA, conexiones JDBC separadas y gateway HTTP configurable. Las imágenes se probaron localmente con UID 10001, filesystem de solo lectura, readiness y SIGTERM. El perfil prod de los servicios JPA valida el esquema existente. Estas capacidades reducen cambios de aplicación para ejecutar contenedores.

La recomendación actual es **no listo para producción**. La validación local ya incluye PostgreSQL, scripts V001, análisis de dependencias y concurrencia de creación/bootstrap. Quedan adopción de datos previos, triage de riesgos residuales y concurrencia de edición/anulación. El login tiene una cuota local por peer y requiere una política de abuso de borde para proxies/réplicas. Se verificó drenaje de una solicitud activa en gateway. Quedan dimensionamiento, drenaje bajo carga/MVC, recuperación de datos y observabilidad del entorno. El workflow de calidad está preparado y su ejecución alojada está pendiente.

## Alternativas y propuesta inicial

| Opción | Aplicación al proyecto | Coste operativo y condición |
|---|---|---|
| Un servicio ECS Fargate, una definición de tarea con tres contenedores | Conserva los procesos y bases separados, usa puertos 8080/8081/8082 y permite destinos localhost | Propuesta inicial para esta escala. Escala/reinicia los tres juntos y comparte el límite de confianza e IAM de la tarea. Revisar CPU/memoria conjunta. |
| Tres servicios ECS Fargate | Permite escalado y ciclo de publicación por microservicio | Requiere destinos privados estables y diseño adicional de conectividad. Adecuado si carga, disponibilidad o aislamiento justifican esa complejidad. |
| ECS sobre EC2 | Ejecuta las mismas imágenes con administración de hosts | Evaluar solo si existe capacidad operativa y una razón de coste/carga verificable. |
| EC2 con procesos/contenedores | Técnicamente compatible con JAR y Docker | El equipo asume parches, supervisor, capacidad y disponibilidad del host. No se selecciona por defecto. |

La propuesta es una inferencia desde el tamaño y acoplamiento operativo actual, pendiente de aprobación del diseño. Fargate permite comunicación localhost dentro de una tarea. Con ALB, sus targets deben ser de tipo ip. Los contenedores conservarían puertos distintos. El ALB registraría únicamente el gateway en 8080. La decisión de red debe bloquear ingreso directo a 8081/8082. [Red de tareas Fargate](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html)

Si las credenciales o el aislamiento de usuarios/prospectos exigen límites distintos, revisar la opción de tres tareas. Los roles IAM son por tarea y una tarea conjunta comparte recursos de red. La aplicación seguirá verificando JWT en cada servicio.

## Componentes futuros y justificación

| Componente | Uso propuesto | Trabajo pendiente |
|---|---|---|
| ECS Fargate | Ejecutar las tres imágenes con supervisión de procesos | Recursos, arquitectura CPU, límites, plataforma, health checks y estrategia de actualización |
| ECR privado | Guardar versiones de imágenes identificadas por digest | Repositorios, política de retención, análisis de imágenes y publicación autorizada futura |
| ALB | Terminar HTTPS y dirigir tráfico al gateway | Certificado, dominio, reglas, target group y política de salud |
| RDS PostgreSQL | Persistencia compatible con el driver y JPA actuales | Elegir versión probada, crear bases/roles separados, migraciones, backup/restauración y TLS |
| Secrets Manager o Parameter Store SecureString | Custodiar contraseñas y material RSA | Elegir uno según rotación y operación, materialización PEM y permisos por recurso |
| CloudWatch Logs | Recibir stdout/stderr de contenedores | Retención, acceso, alertas y comprobación de ausencia de PII |
| IAM | Autorizar extracción de imágenes, logs y secretos | Roles mínimos de ejecución y, cuando sea necesario, de tarea |

ALB mantiene al gateway de la aplicación como entrada funcional. Amazon API Gateway puede evaluarse si aparecen requisitos concretos de administración de APIs. La aplicación actual no requiere ese servicio adicional para enrutar. La decisión ALB debe validarse con el contrato de TLS, dominios y controles de abuso que acuerde el equipo. [ALB con ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/alb.html)

No se justifica añadir colas, caches, discovery o almacenamiento compartido desde las necesidades de código observadas. El diseño futuro debe seleccionar región, subredes y acceso saliente tras evaluar disponibilidad y presupuesto. Las rutas hacia ECR, logs y secretos pueden usar endpoints o salida administrada según la red. No se dimensionaron ni presupuestaron recursos en esta auditoría.

## Persistencia administrada

Usuarios y prospectos deben utilizar bases y roles propios, aun si comparten una instancia PostgreSQL inicial. Evitar permisos cruzados y usar un rol separado para cambios de esquema. El runtime prod usa validate, por lo que una base vacía no arrancará. Las V001 ya se probaron en PostgreSQL aislado. Antes de producción hace falta revisar la adopción de esquemas y datos previos.

Configurar JDBC con verificación de identidad TLS y CA vigente. Ejemplo documental sin credenciales, `jdbc:postgresql://HOST:5432/BASE?sslmode=verify-full&sslrootcert=/run/secrets/rds-ca.pem`. Se requiere validar ese montaje con el driver 42.7.12 y la versión de PostgreSQL elegida. Deben probarse rotación de CA y recuperación de conexión. [TLS en RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/PostgreSQL.Concepts.General.SSL.html)

Los pools Hikari existen por JPA. Sus tamaños deberán fijarse para que la suma de conexiones de todas las réplicas deje margen a administración y migraciones. No se midieron capacidad de RDS ni latencias del driver. H2 con modo PostgreSQL no acredita esa compatibilidad.

## Secretos y claves RSA

Contraseñas JDBC pueden inyectarse mediante referencias de secretos de la definición de tarea. Las variables JWT_PUBLIC_KEY/JWT_PRIVATE_KEY actuales esperan recursos PEM, como file:/run/secrets/private.pem. Introducir el contenido PEM directamente en esas variables no satisface el contrato actual. Gateway/prospectos solo reciben pública, usuarios recibe ambas.

Una opción futura es un contenedor inicializador que materialice PEM en volumen efímero y termine antes del arranque Java. El volumen privado se montaría únicamente en usuarios y los archivos deben ser legibles por UID 10001. Esa integración AWS todavía necesita implementación y pruebas. El patrón de sidecar con volumen está documentado por AWS. [Entrega de secretos en ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/specifying-sensitive-data.html)

La rotación de secretos inyectados en variables requiere nuevas tareas. El backend tampoco recarga claves PEM dinámicamente. Actualmente cada decoder usa una clave pública y el emisor no ofrece un juego de claves rotables. Antes de rotar RSA, acordar una transición coordinada y probar la vigencia de tokens emitidos, sin prometer rotación sin interrupción. [Secretos como variables de ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/secrets-envvar-secrets-manager.html)

## IAM, operación y escala

El rol de ejecución permite al agente extraer imágenes y enviar logs. La lectura de secretos por configuración de ECS también necesita permisos de ejecución concretos. Un inicializador que llame a Secrets Manager necesitaría permisos de tarea limitados a los secretos elegidos. La aplicación Java actual no llama a APIs AWS. Evitar credenciales estáticas dentro de las imágenes. [Roles IAM de ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-iam-role-overview.html), [rol de ejecución](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_execution_IAM_role.html)

Configurar logs por contenedor, retención y acceso. El logger SQL que exponía el RUT en duplicados fue silenciado, pero falta examinar fallos inesperados y el driver PostgreSQL. Las métricas operativas y correlación siguen pendientes. La sonda readiness es pública con estado mínimo. Una caída de usuarios/prospectos no modifica la readiness del gateway, de modo que se necesitan alertas sobre sus respuestas 503/504.

Coordinar retirada de tráfico, espera del balanceador y terminación del proceso con el tiempo graceful. Probar solicitudes activas y despliegues sucesivos. La aplicación es stateless respecto de sesiones, pero escalar usuarios requiere el mismo material criptográfico y base durable. El seed local queda excluido de prod. Probar concurrencia sobre restricciones e impedir que cada réplica migre el esquema al iniciar.

## Qué queda preparado y qué requiere trabajo

Preparado localmente: artefactos, imágenes, configuración externa, perfil prod, probes, timeout HTTP, SIGTERM, separación de identidad/persistencia y controles de calidad reproducibles.

Pendiente en aplicación/validación: evolución de datos previos, riesgo de soporte/SCA residual, abuso distribuido de login, concurrencia de edición/anulación, drenaje bajo carga/MVC y observabilidad. Pendiente en infraestructura futura: decisiones de capacidad/red, ECR/ECS, ALB/TLS, RDS, secretos PEM, IAM, logs, backups y rollback probado. Los pasos ordenados se documentan en 06-DEPLOYMENT-PREREQUISITES.md.

## Actualización de evidencia PostgreSQL local

Después del diagnóstico inicial se añadieron scripts V001 y se ejecutó postgres-smoke.py con PostgreSQL 17.12 y pgJDBC 42.7.12. Validó esquema inicial, roles separados, prod/validate, operaciones por gateway, reinicio y duplicado concurrente. Esta evidencia reduce la incertidumbre de compatibilidad básica PostgreSQL. RDS/TLS, backups/restauración, adopción de datos existentes y actualización/anulación concurrentes continúan pendientes. El aprovisionamiento inicial del administrador productivo aún requiere un procedimiento operativo revisado; las cuentas del test son fixtures locales.

## Actualización de aprovisionamiento inicial

El JAR de usuarios incorpora --bootstrap-admin, documentado en README y probado en PostgreSQL local. Permite preparar la primera cuenta ADMIN sobre tablas existentes sin iniciar HTTP. La infraestructura futura deberá proporcionar variables externas y una ejecución operativa autorizada, luego retirar las variables de bootstrap. El proceso rechaza bases con cualquier cuenta. La prueba inicial no acredita procedimientos de recuperación de cuentas, rotación de secretos o ejecución en AWS.
