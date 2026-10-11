# Ronda 3 · Capacidad de 400 capturas y medición local

Candidato de corrección sobre el original 882dfb8 y las seis correcciones precedentes. La afirmación original de certificación total E1 basada en SQLite host/JVM era mayor que su evidencia; se retira. Sin merge, aceptación humana ni cierre RF. Se mantiene E1/E2 del SRS y ENTREGAS.

## Comprobación acumulada y dataset real · 11/10/2026

187 JVM, cero fallos/errores/omitidas; ambas APK y Lint 0 errores/25 avisos. Xiaomi 11T Android 14/API 34: **51 de datos/nativos (93.926 s)** y **22 UI (180.681 s)** en grupos separados; apertura asistida MIUI en UI. Mismas APK verificadas por SHA256, identificadas con fuentes y runners en [evidencia](evidencias/dataset-correction-2026-10-11.json). Las 51 incluyen 48 cifradas (el ensayo nuevo de 400 y 47 regresiones) y tres decoder/proveedor. Las 22 reúnen selector/contexto, formulario/dispositivo, bandeja/detalle, métricas y administración. No se suman ejecuciones históricas como si fueran una suite actual.

Dataset400StorageTest creó 400 admisiones mediante AdmissionRepository y anuló 40 mediante RecordRepository, sobre contexto sintético seleccionado y SQLCipher/Room v4 reales. Estados finales: READY 260, NEEDS_CONTEXT 60, DRAFT 40, CANCELLED 40; 600 intereses, 320 claves activas y 440 auditorías. Reapertura conserva filas/claves/auditoría; encabezado cifrado e integridad FK comprobados. Cada captador recupera 200 filas mediante cursor real sin repetición/omisión; supervisor 360, sin borradores ajenos. Por evento: 160 inscripciones activas, 160 personas y conteos de programas 160/80; consulta fuera de alcance denegada.

Latencia de llamada completa al repositorio (incluye validación, contexto, dispatcher y transacción; excluye inicialización, selección de contexto y GPS/red), elapsedRealtimeNanos y percentiles nearest-rank: altas n=400 p95 **114.626 ms**, anulaciones n=40 p95 **100.073 ms**, conjunto n=440 p95 **113.457 ms**. Altas p99 153.829 ms y máximo 195.897 ms. Meta PT p95 <150 ms observada en este ensayo debug; no significa que toda llamada dure menos de 150 ms. Sin perfilado de frames, no acredita ausencia de jank ni fugas de memoria.

StressDataset400Test comprueba distribución y aislamiento con LocalCaptureAccess, incluyendo realmente 50 borradores ajenos en su fixture específico de 350 visibles. Sus lotes en memoria no prueban un cursor ni rendimiento. verify_dataset_400_sql.py usa esquema v4, UUID/RUT/enums coherentes y verifica estructura/FK/conteos/cursor del host; no mide ni certifica Android.

Consultas completas observadas: paginar las 200 filas de captador A 652.249 ms y B 557.665 ms (siete llamadas por captador); métricas A 126.099 ms y B 158.650 ms (una muestra por evento). No son percentiles ni la latencia de una página. Se conservan las agregaciones Kotlin sobre consultas Room autorizadas; no se afirma optimización SQL adicional.

## Reproducir

Desde android ejecutar las tareas configuradas testDemoDebugUnitTest, assembleDemoDebug, lintDemoDebug y assembleDemoDebugAndroidTest según README. Instalar ambas APK identificadas y ejecutar AndroidJUnitRunner con la clase io.github.grupo6.capturaprospectos.data.local.Dataset400StorageTest. El runner emite G6_DATASET400_RESULT como JSON, sin datos personales ni identificador ADB. El test sólo crea y limpia bases test-dataset400-* y sus claves, preservando la base de la app. python tools/verify_dataset_400_sql.py ejecuta el control estructural independiente del host.

Pendientes: revisión cruzada de otro integrante, aceptación específica RECORD_CONTRACTS/auditoría v4, giro real de MainActivity, TalkBack/visual completa, permisos restantes/cámara y GPS adversos, portal cautivo, ensayo de defensa P3 y matriz de dispositivos. La lectura física QR y adquisición/confirmación/descarte GPS reportadas por Martín pertenecen al incremento nativo, no a esta prueba con coordenadas ficticias. Backend/auth/sincronización y consolidación remota siguen en E2.
