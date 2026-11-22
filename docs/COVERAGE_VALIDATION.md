# Cobertura JaCoCo · E2 candidata

22/11/2026. Medición de líneas de producción ejecutadas por pruebas, con JaCoCo 0.8.16. La cobertura no equivale a corrección funcional ni sustituye la defensa o la validación de los criterios del profesor.

## Revalidación del candidato 1.1.0 · 22/11/2026

Esta ejecución posterior incluye la sesión recordada y la imagen de perfil en Inicio. Se recompiló el backend con `clean verify -Pcoverage`; Android partió de `:app:clean`, con `g6AndroidCoverage=true` y URL de pruebas `http://10.0.2.2:18080/`. La suite se dirigió exclusivamente a `emulator-5554`, API 34. No se instalaron pruebas en el Redmi ni se utilizaron cuentas o bases de AWS.

| Alcance | Líneas cubiertas / total | Cobertura |
|---|---:|---:|
| api-gateway | 114 / 116 | 98,28 % |
| users-service | 291 / 345 | 84,35 % |
| prospects-service | 196 / 200 | 98,00 % |
| Dominio Android | 521 / 541 | 96,30 % |
| ViewModels Android | 1039 / 1134 | 91,62 % |
| Datos Android | 1324 / 1636 | 80,93 % |
| **Lógica Android** | **2884 / 3311** | **87,10 %** |
| Presentación escrita a mano | 1956 / 2488 | 78,62 % |
| Informe Android completo, incluido generado | 7121 / 8603 | 82,77 % |

Pasaron **60 tests de backend**, **268 JVM Android** y **141 instrumentados autónomos**, sin fallos ni errores. Después pasaron los **dos recorridos instrumentados con gateway aislado**: cuentas/permisos y sincronización con pérdida deliberada de ACK. Se recogió su `.ec` y se regeneró el informe conjunto sobre las mismas clases, sin repetir las suites ni reutilizar datos de compilaciones anteriores. Son **143 casos instrumentados aprobados**; la prueba asistida de Wi-Fi no se ejecutó nuevamente en esta medición.

El informe autónomo previo a esos dos recorridos aprobó con 2863/3311 líneas de lógica (86,47 %). El resultado final combinado es el de la tabla anterior. Los cinco tests del analizador de cobertura también pasaron. Su primer intento encontró una restricción del directorio TEMP de Windows; la repetición utilizó un directorio temporal dentro del workspace, sin modificar el analizador ni su criterio.

Evidencia local fuera del repositorio: `Docs/cierre-e2/coverage-final.json`, `coverage-autonomous-final.json`, `backend-final-verification.json` e informes HTML/XML bajo `Docs/cierre-e2/cobertura/`. Logs `final-release-backend-coverage.log`, `final-release-android-coverage.log`, `final-release-gateway-coverage.log` y `final-release-merged-coverage.log` en `Residuos ChatGPT/`. El backend efímero de pruebas se detuvo y el backend local persistente de Windows quedó restaurado. La firma/distribución de la APK se documenta en [Release](RELEASE_DISTRIBUTION.md); esta cobertura mide la variante debug, no una instrumentación de la APK release.

## Métrica y alcance

Objetivo: al menos 80 % de líneas (`LINE / COVEREDRATIO`), calculado como líneas cubiertas / (cubiertas + no cubiertas). Se compara sin redondear. Las ramas se conservan en los informes, pero no se declaran como el criterio del 80 %.

- **Backend:** todas las clases de producción de cada servicio, incluido arranque, configuración, seguridad, controladores, modelos y aprovisionamiento. No se excluyen clases para mejorar el resultado. Cada servicio debe superar individualmente el mínimo; un servicio no compensa a otro.
- **Lógica Android:** todos los archivos escritos a mano de `domain/**`, `data/**` y todos los `*ViewModel.kt`, incluidos persistencia, sesión, integración remota y personalización. Se suman contadores por archivo fuente para evitar duplicar líneas Kotlin que comparten varias clases.
- **Presentación Android:** Activity, Composables, componentes y tema se muestran por separado. Su ejecución se comprueba con pruebas de interfaz, pero el porcentaje de lógica no se presenta como cobertura total de la app.
- **Código generado Android:** Room/KSP y BuildConfig quedan visibles en el informe completo. Para el resumen de lógica se identifica como generado un archivo del informe que no existe en `app/src/main/java`. No se retiran manualmente archivos de lógica que tengan poca cobertura.

La interpretación de «código lógico» se declara explícitamente; debe contrastarse con las indicaciones docentes. No se afirma que el requisito académico esté aceptado únicamente por pasar un umbral técnico.

## Reproducción

Desde `backend`, con Java 21 y Maven Wrapper:

```powershell
.\mvnw.cmd clean verify -Pcoverage -B
```

El perfil activa agente, informe y comprobación del 80 %. Un resultado inferior falla en `verify`. El agente reemplaza los datos de su ejecución anterior (`append=false`), evitando aprobar por cobertura residual de pruebas retiradas. Los informes quedan en `<servicio>/target/site/jacoco/index.html` y `jacoco.xml`. Las pruebas usan recursos aislados, mayoritariamente H2 y HTTP controlado; estos informes no son una nueva prueba de PostgreSQL/AWS ni incluyen la cobertura de procesos iniciados por scripts externos.

Desde `android`, para medir únicamente JVM, sin dispositivo ni backend:

```powershell
.\gradlew.bat :app:createDemoDebugUnitTestCoverageReport
```

Informe: `app/build/reports/coverage/test/demo/debug/index.html`. Esta medición deja sin ejecutar las implementaciones que necesitan Android.

Para combinar JVM e instrumentación en el emulador preparado:

```powershell
$env:ANDROID_SERIAL='emulator-5554'
.\gradlew.bat :app:jacocoDemoDebugCombinedReport '-Pg6AndroidCoverage=true'
```

La cobertura instrumental se activa solo con esa propiedad; no es necesario generar APK instrumentada para el Redmi. La tarea combina datos JVM `.exec` y del emulador `.ec` sobre las clases originales de AGP 9.4.1. No se instalan pruebas en el teléfono físico. `ANDROID_SERIAL` debe apuntar explícitamente al emulador; no ejecutar con selección ambigua de dispositivos.

El build configura `notAnnotation=io.github.grupo6.capturaprospectos.testing.RequiresExternalFixture` para esta ejecución. La anotación identifica tres recorridos que necesitan preparación externa: `RemoteAccountJourneyTest`, el método `CaptureSyncStorageTest.actualGatewayRecoversLostAckThenEditsAndCancelsOneRemoteCapture` (ambos requieren el backend aislado) y `ConnectivityTransitionTest` (orquestación especial de pérdida y recuperación real de red). Los otros once métodos de `CaptureSyncStorageTest` se ejecutan normalmente. Sus exclusiones **no retiran código de producción del denominador** ni equivalen a aprobar esas pruebas. No se envían coordenadas o imágenes de pruebas a servicios públicos.

Informe conjunto: `app/build/reports/jacoco/jacocoDemoDebugCombinedReport/html/index.html` y XML junto a la carpeta HTML. No se combinan porcentajes: se unen los datos de ejecución de la misma compilación.

Desde la raíz del repositorio, con Python 3:

```powershell
python scripts/coverage_summary.py --output coverage-summary.json
```

El script falla con código 1 si cualquiera de los tres servicios o la lógica Android queda bajo 80 %; código 2 si falta un informe o su XML es inválido. Muestra además dominio, ViewModels, datos, presentación y código generado por separado. El JSON es evidencia generada, no un archivo que deba editarse para aprobar.

El verificador cuenta con cinco pruebas (`python scripts/test_coverage_summary.py`): promedio ponderado por líneas, conservación de datos sin cobertura, separación de código generado/presentación, límite exacto y rechazo bajo el límite aunque se redondee a 80 %, cobertura vacía e informe ausente.

## Resultados anteriores a la revalidación 1.1.0

Backend: 57 pruebas aprobadas, cero fallos/errores. Se incorporaron cuatro pruebas del rechazo seguro del aprovisionamiento administrativo: configuración incompleta, identidad/contraseña inválida, sedes inválidas y base distinta de PostgreSQL, sin conectar a una base ni crear cuentas.

| Servicio | Líneas cubiertas / total | Cobertura |
|---|---:|---:|
| api-gateway | 105 / 107 | 98,13 % |
| users-service | 291 / 345 | 84,35 % |
| prospects-service | 195 / 199 | 97,99 % |

Primera medición Android **solo JVM**: 260 pruebas aprobadas; dominio 493/541 (91,13 %), ViewModels 882/1071 (82,35 %), datos 258/1575 (16,38 %). Lógica total 1633/3187 (51,24 %). No se presenta ese resultado como el de la ejecución combinada.

La primera ejecución instrumental terminó sin aprobar: 137 pruebas pasaron, un recorrido de sincronización falló al no estar disponible su backend de prueba y la omisión asistida de Wi-Fi se reflejó como fallo en el XML de AGP. Se añadieron las anotaciones de preparación externa para separar estos recorridos; no se cambió la lógica de producción ni se eliminó código del informe para resolver esas incidencias.

La repetición con el filtro por anotación terminó correctamente: **137 pruebas instrumentadas, cero fallos**, exclusivamente en emulador API 34. Combinadas con las 260 pruebas JVM dieron 2748/3187 líneas de lógica (86,23 %); este es el alcance reproducible con el comando automático anterior, sin backend.

Después se ejecutaron explícitamente los dos recorridos que requieren gateway contra los tres JAR locales aislados y cuentas ficticias: registro/aprobación/login/desactivación y sincronización con pérdida deliberada de ACK seguida de edición y anulación. **Ambos aprobados**. Se recogió su archivo JaCoCo `.ec` en `outputs/code_coverage/demoDebugAndroidTest/connected/external-fixture/coverage.ec`, y se regeneró únicamente el informe conjunto usando las clases originales de la misma compilación. El backend temporal se detuvo al finalizar. La prueba asistida de transición real de Wi-Fi continúa fuera de esta validación.

| Android: JVM + 137 pruebas autónomas + 2 de gateway | Líneas cubiertas / total | Cobertura |
|---|---:|---:|
| Dominio | 521 / 541 | 96,30 % |
| ViewModels | 985 / 1071 | 91,97 % |
| Datos, incluidos Room, sesión y API | 1263 / 1575 | 80,19 % |
| **Lógica total** | **2769 / 3187** | **86,88 %** |
| Presentación escrita a mano | 1954 / 2474 | 78,98 % |
| Informe completo, incluido generado | 7004 / 8465 | 82,74 % |

Los porcentajes de filas distintas no se promedian. La lógica no incluye presentación; el informe completo sí incluye presentación y código generado. El script aprobó el umbral por servicio y lógica Android, y sus cinco pruebas también terminaron correctamente.

Para volver a unir datos instrumentales ya obtenidos, sin repetir pruebas, se utilizó:

```powershell
.\gradlew.bat :app:jacocoDemoDebugCombinedReport '-Pg6AndroidCoverage=true' -x :app:connectedDemoDebugAndroidTest -x :app:createDemoDebugAndroidTestCoverageReport
```

Este último comando solo es válido después de obtener datos de pruebas aprobadas de la misma compilación. No reemplaza ejecutar las pruebas. Para una medición nueva, utilizar el comando completo sin exclusiones de tareas. La ejecución autónoma nueva elimina los datos instrumentales anteriores; los dos recorridos de gateway deben repetirse para reproducir el 86,88 % final, en lugar del 86,23 % autónomo.

Evidencia local conservada en `Docs/revision-cobertura/` de la carpeta AppMovil: JSON del resumen y los cuatro XML JaCoCo (tres servicios y Android). Los informes HTML navegables permanecen en las rutas de compilación indicadas. No se generaron APK release ni despliegues como parte de esta medición.

La prueba manual de apodo/foto en Redmi permanece pendiente, tal como se registra en [Personalización](PROFILE_PERSONALIZATION.md).

## Referencias técnicas

- [Informes y activación de cobertura Android](https://developer.android.com/studio/test/coverage-report).
- [JaCoCo Maven: agente e informes](https://www.jacoco.org/jacoco/trunk/doc/maven.html).
- [JaCoCo check: métrica y umbral](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html).
