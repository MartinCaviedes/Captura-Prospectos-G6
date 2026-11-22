# Backend académico en EC2

Los tres servicios se ejecutan como contenedores separados junto con PostgreSQL 17.12. El gateway se publica solamente en `127.0.0.1:8080`; Nginx expone API y Swagger por HTTPS. Los servicios de usuarios, prospectos y PostgreSQL no publican puertos del host. La administración se realiza mediante Systems Manager, sin habilitar SSH en el grupo de seguridad.

## Instalación inicial

Se utiliza una instancia Linux x86_64 con 8 GiB de RAM, IMDSv2 obligatorio, un disco raíz gp3 de 20 GiB y un disco de datos gp3 cifrado de 10 GiB montado en `/srv/g6`. Este último debe conservarse al terminar la instancia. Antes del arranque de Docker se exige que el disco esté montado.

El paquete de instalación contiene `install.py`, `backup.sh`, `https.sh` y una carpeta por microservicio con su Dockerfile, `target/<servicio>-0.0.1-SNAPSHOT.jar` y, cuando corresponda, `migrations/V*.sql`. Los JAR deben proceder de una compilación verificada con Java 21 y ser posteriores a sus fuentes. No incluir `.local`, archivos de contraseñas ni claves del entorno Windows. Verificar el SHA-256 del paquete antes de extraerlo en `/opt/g6`.

En el host nuevo, con Docker, OpenSSL, Python 3 y Nginx instalados:

```bash
sudo python3 /opt/g6/install.py
sudo bash /opt/g6/https.sh IP_ELASTICA
sudo bash /opt/g6/backup.sh
```

`install.py` se niega a sobrescribir una configuración existente. Genera contraseñas aleatorias y un par RSA exclusivo del servidor, aplica V001 y V002 de ambas bases y crea cuentas de base de datos con permisos limitados. El perfil `prod` valida el esquema y no carga el seed demo. El administrador inicial es `g6-admin`; su contraseña queda exclusivamente en `/srv/g6/config/admin-access.txt`, con permisos de root. No publicar este archivo ni reutilizar las contraseñas demo locales. Su dirección de correo inicial es un marcador académico, no una cuenta de correo real.

Las claves y configuraciones están en `/srv/g6/config`, PostgreSQL en `/srv/g6/postgres`, los respaldos lógicos en `/srv/g6/backups` y los certificados en `/srv/g6/letsencrypt`. El administrador asigna roles y sedes a las cuentas registradas desde la aplicación.

## Acceso y comprobación

Swagger: `https://IP_ELASTICA/docs/index.html`. Contrato: `/docs/openapi/backend.openapi.json`. La API conserva la autenticación JWT; exponer Swagger no vuelve públicos los datos. La documentación se habilita mediante `API_DOCS_ENABLED=true` en el gateway. No abrir 8080, 8081, 8082 o 5432 en el grupo de seguridad.

La IP utiliza un certificado público de corta duración de Let's Encrypt. El temporizador `g6-cert-renew.timer` revisa la renovación dos veces al día; el puerto 80 permite el desafío ACME y redirige el resto a HTTPS. Los temporizadores persistentes ejecutan las tareas pendientes al volver a encender la instancia.

```bash
sudo docker ps
sudo systemctl status nginx g6-cert-renew.timer g6-backup.timer
curl --fail https://IP_ELASTICA/actuator/health/readiness
sudo systemctl start g6-cert-renew.service
sudo systemctl start g6-backup.service
```

Para una app conectada a AWS, compilar desde `android/` con `-Pg6ApiBaseUrl=https://IP_ELASTICA/`. La APK local anterior sigue apuntando al backend local hasta instalar una compilación con esta URL. Las cuentas y los datos de ambos entornos son independientes.

## Reinicio, respaldos y costos

Los contenedores tienen política `unless-stopped`. Reiniciar una instancia conservando sus volúmenes no requiere reinstalar, volver a aplicar migraciones ni ejecutar bootstrap. Para reiniciar servicios de aplicación sin tocar datos:

```bash
sudo docker restart g6-users-service g6-prospects-service g6-api-gateway
```

El respaldo diario es un `pg_dumpall` comprimido, guardado con permisos restrictivos y retenido unos nueve días. Contiene hashes y credenciales de roles de base de datos: tratarlo como información secreta. Los archivos permanecen en el mismo disco y no sustituyen un respaldo externo. Una instantánea EBS del disco añade una copia independiente del volumen, pero las instantáneas adicionales requieren una acción explícita o configurar posteriormente su programación. Comprobar la restauración en un contenedor aislado antes de intervenir las bases reales.

Detener la instancia interrumpe el servicio y detiene el costo de cómputo; el almacenamiento, las instantáneas y la IP elástica siguen generando cargos. No terminar la instancia, borrar el disco, liberar la IP ni modificar recursos de otros proyectos como parte de una detención rutinaria. La estimación de una semana no establece un apagado automático ni un límite de gasto.

Los identificadores reales de AWS, la IP y las evidencias de la instalación se registran fuera del repositorio, en la carpeta de documentación local de despliegue. No incluir credenciales en capturas de Swagger, archivos de entrega o commits.

## Validación física y cuentas de presentación · 22/11/2026

Martín confirmó el recorrido guiado de la APK de AWS en Redmi 9A sin errores observados. Reportó expresamente capturas con internet y estado de transporte visible, guardado local sin conexión, envío al recuperar internet y conservación de sesión/datos tras cerrar y reabrir la app. Esta es evidencia manual reportada por el usuario; no se presenta como una nueva suite automática ni como medición de tiempos de sincronización.

Se crearon, mediante la API administrativa, tres captadores y dos supervisores habilitados para facilitar la demostración sin repetir el registro. Cada cuenta tiene contraseña propia, conservada fuera del repositorio. Se verificaron por HTTPS su login, identidad, rol, sedes, estado ACTIVE y acceso al listado de prospectos. Esto no acredita consolidación del panel entre dispositivos ni convierte el rol supervisor en administrador.
