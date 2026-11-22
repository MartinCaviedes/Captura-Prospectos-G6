# Backend local en Windows e IntelliJ

22/11/2026. Configuración de desarrollo para las cuentas de demostración y el Redmi, independiente del backend efímero de pruebas. No configura producción ni AWS.

## Diagnóstico del arranque

El log de IntelliJ mostró `Could not resolve placeholder 'JWT_PUBLIC_KEY'` en los tres servicios. Las clases ya se estaban ejecutando; el fallo sucedía al crear los componentes de seguridad durante el arranque. No era un fallo de compilación.

También mostró `No active profile set`. El sembrador de cuentas solo se activa con `local` y sin `prod`. Abrir directamente las clases main no carga las variables que se habían usado en el helper de pruebas. Los archivos `.env.example` y `.local/env.sh` tampoco se importan automáticamente al proceso de IntelliJ.

`captor-a` / `test-captor-password-only` era una cuenta de la fixture temporal: base en memoria, claves efímeras y apagado a los 30 minutos. Para trabajar de manera continuada se preparó un entorno local distinto, con claves reutilizables y bases en disco.

## Preparación inicial

PowerShell 7+, Java 21, desde `Captura-Prospectos-G6/backend`:

```powershell
.\scripts\prepare-local.ps1 -UseDemoCredentials
```

El flag selecciona las credenciales ficticias conocidas: `captor-a`, `captor-b` y `demo-captor` usan `test-captor-password-only`; `demo-admin` y `supervisor` usan `test-admin-password-only`. Sin el flag, una preparación nueva genera contraseñas aleatorias. Un entorno ya preparado conserva sus archivos: el comando no restablece contraseñas existentes ni modifica cuentas de otra base.

Se generan, exclusivamente en `backend/.local/windows/`:

- Un par RSA: privada PKCS#8 y pública X.509, coherentes entre servicios. Solo usuarios recibe la privada.
- `users.properties`, `prospects.properties`, `gateway.properties`: perfil local, puertos 8081/8082/8080 y escucha en loopback.
- Dos bases H2 distintas en archivo, `identity.mv.db` y `prospects.mv.db`, creadas al iniciar. Se conservan al detener los servicios.
- Logs y registro de procesos del lanzador.

La carpeta nueva restringe acceso al usuario actual, SYSTEM y administradores, y está excluida de Git. No se imprimen claves, contraseñas o JWT. No se sobreescriben claves/configuraciones existentes ni se reinician bases. No se cambia la configuración predeterminada/productiva de los servicios. El seed mantiene las cuentas existentes y solo crea las ausentes.

## Opción A: iniciar desde PowerShell

Con los JAR compilados (si faltan: `.\mvnw.cmd verify`):

```powershell
.\scripts\run-local.ps1
```

Inicia usuarios, prospectos y gateway en ese orden, comprueba readiness y conserva los procesos en segundo plano. No tiene el apagado de 30 minutos de la fixture. Detecta puertos ocupados y evita duplicar sus procesos; no detiene procesos ajenos para liberar puertos.

Swagger: `http://localhost:8080/docs/index.html`. Login de la app: gateway en 8080. Los archivos locales habilitan Swagger explícitamente.

Para detener únicamente los procesos de este lanzador:

```powershell
.\scripts\run-local.ps1 -Stop
```

Se comprueban PID, instante de inicio y argumentos antes de detener cada proceso, evitando confundir un PID reutilizado. Las bases/configuraciones permanecen. Si se iniciaron desde IntelliJ, detenerlos desde el IDE.

## Opción B: ejecutar desde IntelliJ

Abrir `backend/pom.xml` como proyecto Maven. Las configuraciones compartidas de `backend/.run/` son:

1. **G6 local - Usuarios**
2. **G6 local - Prospectos**
3. **G6 local - Gateway**

Usan los módulos correspondientes y `--spring.config.additional-location` para cargar el archivo privado de cada servicio. No guardan credenciales en XML. Si no aparecen, recargar/reabrir el proyecto backend para que el IDE descubra las configuraciones compartidas.

Las configuraciones antiguas `UsersApplication`, `ProspectsApplication` y `ApiGatewayApplication` no se modificaron automáticamente: ejecutarlas sin aportar configuración seguirá produciendo el error del log. Para usar IntelliJ después del lanzador PowerShell, detener primero el lanzador con `-Stop`; ambos utilizan los mismos puertos y bases. No iniciar las dos opciones simultáneamente.

Las rutas de los XML están preparadas para Windows y para abrir **backend** como raíz del proyecto. Si se abre la raíz completa del repositorio, ajustar la ruta de configuración agregando `backend/` y comprobar los nombres de los módulos importados.

## Conexión del Redmi

La APK preparada para USB usa `http://127.0.0.1:8080/`. Con el teléfono autorizado y conectado:

```powershell
& 'D:/DevelopmentData/AndroidSDK/platform-tools/adb.exe' -s 8XKFIVVOEEFQZ9AU reverse tcp:8080 tcp:8080
```

La redirección debe restablecerse después de desconectar el USB o reiniciar ADB/teléfono. El destino ahora es el gateway persistente **8080**, no el 18080 de la fixture anterior. El emulador usa `http://10.0.2.2:8080/` en su compilación predeterminada. No cambiar de base URL reinstalando/borrando datos sin necesidad.

El PC debe mantener el backend encendido y la conexión USB activa para este modo. El despliegue AWS/HTTPS posterior permitirá trabajar sin depender de este PC. Si queda una sesión emitida por las claves efímeras anteriores, volver a iniciar sesión con las credenciales; no borrar datos de la app ni su cola de sincronización.

Los datos que solo existían en el servidor efímero anterior no se recuperan al crear esta base. La cola/localización de fichas ya sincronizadas conserva su seguimiento y no se reinicia automáticamente. Esta reparación del arranque no acredita conciliación de datos entre bases distintas.

## Verificación realizada

- Tres servicios arrancados: readiness `UP` en 8080, 8081 y 8082.
- POST login de `captor-a` con las credenciales indicadas: HTTP 200, rol CAPTADOR y GET `/me` correcto. JWT no mostrado.
- GET `/me` sin JWT: HTTP 401, manteniendo la protección.
- Detención y arranque completos del lanzador: cuenta conserva UUID y fecha de creación; login vuelve a devolver HTTP 200. Archivos H2 no vacíos.
- Segunda invocación del lanzador con los tres activos: detecta la ejecución y no crea duplicados.
- Redirección USB del Redmi restablecida a `tcp:8080 → tcp:8080`, sin reinstalar ni borrar datos del teléfono.
- Las configuraciones IntelliJ se prepararon a partir del tipo/módulos observados en el proyecto. No se afirma que se hayan ejecutado mediante la interfaz del IDE; la comprobación de arranque utilizó los mismos archivos de propiedades con los JAR.
- Martín confirmó el ingreso desde Redmi, cámara, galería y apodo, y posteriormente la conservación/visualización de la foto en Inicio y la recuperación de sesión al cerrar/reabrir la app o cambiar de pestaña. Ver [sesión recordada y foto en Inicio](REMEMBERED_SESSION.md).
