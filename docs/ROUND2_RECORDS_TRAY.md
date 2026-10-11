# Ronda 2 · registros locales corregidos

Candidato en `feature/e1-ronda2-records-tray`, sobre 08df94f con la corrección previa de alta/hub. RECORD_CONTRACTS conserva su carácter de propuesta pendiente de aceptación humana; esta rama prepara y prueba su implementación técnica. No constituye integración en develop ni cierre de ronda/RF.

## Comportamiento

El repositorio original devolvía Saved para edit sin escribir. Ahora edición/anulación se resuelven dentro de una transacción Room cifrada: autorización uniforme por propiedad/sede/rol, revisión, transición, validación F2027 con PrepareAdmission, duplicados, fila/selecciones/clave activa y auditoría. La UI de detalle pertenece a la rama siguiente; esta bandeja sólo consulta y anula.

Las inscripciones activas conservan RUT normalizado, autor, sede/evento, instalación, instante y consentimiento; no se degradan a borrador. Un borrador propio puede corregirse/confirmarse; NEEDS_CONTEXT puede recibir ubicación nueva válida. Ubicación ausente conserva la evidencia histórica, sin exigir que un GPS original siga siendo reciente. Las selecciones de catálogo sin cambios conservan sus snapshots. Los valores opcionales pueden retirarse explícitamente. LEGACY admite lectura/listado y anulación conservadora, con edición F2027 UnsupportedContract.

Captador: registros propios dentro de sus sedes. Supervisor: registros de sus sedes sin borradores ajenos; edición ajena requiere motivo. Un registro inaccesible no revela revisión ni existencia. Un fallo de SQLite produce resultado tipado, conserva datos y revierte toda mutación. CancellationException se propaga.

Consulta normalizada por nombre/RUT, fechas UTC, evento y estado. La autorización precede a la paginación exclusiva (instante DESC, UUID canónico DESC). La bandeja consume páginas de 50 con «Cargar más registros», refresca desde inicio al cambiar filtros y cancela lecturas obsoletas. No ofrece snapshot entre páginas. Conserva el motivo ante fallo de almacenamiento y bloquea segundo envío, cierre y salida mientras muta. Su ViewModel se retiene por actor/rol/sedes y se cancela antes de cerrar Room. No se simula sincronización.

## Auditoría y migración candidata

Room v4 añade a local_audit previousRevision, reasonText y changedFields mediante 3→4 aditiva. Los atributos desconocidos de auditorías históricas quedan NULL; no se fabrican motivos. Una mutación efectiva aumenta la revisión una vez y guarda actor, fecha, acción, revisión anterior/resultante, motivo cuando corresponde y nombres de campos modificados. No duplica los valores personales en changedFields; una edición sin cambios no crea otra revisión. Se conservan los esquemas 1/2/3/4 y todas las migraciones, sin fallback destructivo.

La prueba cifrada compara todos los valores v3 y los índices/FK antes/después, con capturas DRAFT/READY/NEEDS_CONTEXT/CANCELLED, claves y auditorías. Nuevos consumidores deben incorporar esta corrección completa, no introducir otra migración v4. La coordinación/aceptación humana del contrato sigue pendiente.

## Controles y límites

121 JVM aprobadas, build de ambas APK y Lint 20 avisos/0 errores. En Xiaomi Android 14/API 34: 37 cifradas y dos UI, ejecutadas por separado. Las diez pruebas nuevas de Room cubren escritura real, autorización, revisión, transición, unicidad, auditoría y rollback, además de 120 registros paginados. Las ocho JVM originales son pruebas con doble, sin certificar reglas SQL; se conservan y agregan cuatro de paginación/cancelación. La UI también usa doble y apertura asistida ADB; no certifica accesibilidad completa ni un recorrido conjunto con Room. Detalle, giro real de MainActivity, TalkBack, cámara/GPS y revisión cruzada pendientes.

[Resultados y reproducción](ANDROID_BASELINE.md), [fuentes/APK](evidencias/records-correction-2026-10-10.json). No se acredita cierre E1 ni aceptación del contrato.
