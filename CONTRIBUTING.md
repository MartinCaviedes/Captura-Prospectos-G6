# Contribuir al proyecto

Declarar tarea, RF, E1/E2, criterios, dependencias, base de código y archivos compartidos antes de editar. Leer [SRS](docs/SRS.md), [ENTREGAS](docs/ENTREGAS.md) y el contrato aplicable. Una subtarea E1 terminada no cierra automáticamente el RF final.

## Ramas y revisión

Flujo acordado: `feature → develop → main`, con revisión humana de otro integrante. Una rama por tarea; usar también `fix/`, `docs/` y `chore/`. Coordinar antes de modificar Gradle, Manifest, navegación, esquema, migraciones o APIs compartidas. Cada persona usa su clon, sin un working tree compartido por sincronización de carpetas.

Esta base preparada todavía no acredita origin/main/develop remotos. Las instrucciones siguientes se usan después de verificar esos recursos y el acceso individual. No clonar encima de una carpeta con trabajo previo, forzar ramas compartidas, borrar cambios ajenos ni rehacer autorías para repartir contribuciones.

En un clon ya incorporado:

```powershell
git status --short --branch
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/e1-rf02-validacion
```

Detenerse si hay cambios inesperados, conflictos o falla fast-forward; preservar el trabajo y resolver la causa antes de continuar. Sustituir el nombre de rama por la tarea real.

## Commit y push

Formato: `tipo(alcance): descripción breve`, preferentemente en español. Tipos: feat, fix, docs, test, refactor, chore, build, ci. Ejemplo de formato: `feat(captura): validar RUT según RF02`. No atribuir ejecución, integración o participación que no ocurrió.

Plantilla opcional por clon:

```powershell
git config --local commit.template .github/COMMIT_MESSAGE.txt
```

Revisar archivos modificados y nuevos, seleccionar sólo los de la tarea y comprobar el diff. `RUTA_REAL` es un marcador que debe sustituirse:

```powershell
git diff
git status --short
git add -- RUTA_REAL
git diff --cached --check
git diff --cached
git commit
```

Un push publica commits; no tiene un mensaje independiente. Cuando esté autorizado para la tarea/repositorio:

```powershell
git push -u origin feature/e1-rf02-validacion
git rev-parse HEAD
git ls-remote --heads origin feature/e1-rf02-validacion
```

Comparar SHA local/remoto y abrir PR hacia develop con la plantilla. Solicitar revisión de otro integrante; corregir observaciones antes de integrar. main recibe hitos comprobados mediante PR. La existencia de plantillas no acredita protecciones, checks ni revisión realizada.

## Desarrollo en paralelo

Dividir por incremento verificable, no por RF completo. Formulario consume AdmissionRepository.create/get y el contexto existente; no necesita esperar la edición/anulación. Parser QR, ubicación y conectividad pueden desarrollarse de forma aislada. Para listados/edición/métricas, revisar primero firmas, errores y fixtures: después UI y persistencia trabajan contra el mismo contrato, con dobles explícitos hasta integrar el repositorio real.

Coordinar temporalmente navegación, Gradle/Manifest y migraciones; el resto trabaja en archivos propios. Incorporar cambios de develop a la rama de tarea mediante merge y volver a verificar lo afectado. No reescribir una rama ya compartida ni declarar integrada una pantalla que sólo consume un doble.

## Evidencia y cuidado de datos

Ejecutar controles proporcionales al cambio, siguiendo [Android](android/README.md). Distinguir build, tests JVM e instrumentación realmente ejecutada. Cambios documentales requieren revisar consistencia/enlaces; cambios de contrato o esquema requieren sus consumidores/migraciones y pruebas pertinentes.

El PR explica problema, resultado, criterios, pruebas, límites y compatibilidad. No incluir secretos, claves, datos personales reales, local.properties, APKs de trabajo, cachés ni documentación operativa privada. Usar únicamente fixtures ficticias. Consentimiento, cifrado y conservación de pendientes se mantienen durante cada incremento.

Cambios de negocio o hito requieren decisión humana visible; detalles técnicos reversibles dentro del contrato se resuelven y documentan. Publicación, PR, revisión y merge son operaciones distintas: registrar sólo las que realmente ocurrieron.
