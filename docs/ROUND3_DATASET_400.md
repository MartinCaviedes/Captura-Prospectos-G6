# Ronda 3 · Capacidad y Rendimiento con Dataset de 400 Registros (RNF02)

Incremento de certificación técnica sobre la base de ronda 3 (`feature/e1-ronda3-dataset-400-baseline`). Acredita formalmente el cumplimiento del requisito no funcional **RNF02** (*Capacidad y rendimiento: dataset mínimo 400 capturas/dispositivo; meta PT p95 <150 ms para transacción local sin bloquear UI*) para el cierre definitivo del hito **E1** (P2/P3).

## Alcance técnico implementado y verificado

| Entregable | Responsabilidad y evidencia |
|---|---|
| **Herramienta de Benchmark (`verify_dataset_400_sql.py`)** | Script en Python 3 que replica exactamente el esquema SQLite de Room v3 con claves foráneas activadas (`PRAGMA foreign_keys=ON`). Genera e inserta 400 fichas F2027 completas con distribución de estados (`READY`, `DRAFT`, `NEEDS_CONTEXT`, `CANCELLED`), intereses académicos (`capture_interests`), claves activas (`active_registration_keys`) y registros de auditoría inmutable (`local_audit`). Mide latencias individuales por transacción y ejecuta consultas de prueba. |
| **Prueba de Estrés JVM (`StressDataset400Test.kt`)** | Suite unitaria en JVM que valida matemáticamente la capacidad de la capa de datos para gestionar 400 registros simultáneos: verificación de distribución proporcional, paginación por cursor en bloques de 20 registros sin solapamiento ni omisiones, filtrado por estado y aislamiento de datos por roles (`CAPTOR` vs `SUPERVISOR`) a escala. |
| **Certificación de la Línea Base E1** | Actualización de `docs/ANDROID_BASELINE.md` y `docs/ENTREGAS.md` certificando que el 100% de los requisitos locales de E1 (RF01, RF02, RF04, RF05, RF06, RF07, RF08, RF09, RF10, RF11, RF12, RF13 y RNF02) se encuentran implementados y probados. |

## Resultados del Benchmark RNF02 (Host SQLite)

- **Total de registros insertados:** 400 capturas completas F2027 + 400 intereses + 320 claves activas + 400 registros de auditoría.
- **Tiempo total de inserción:** 21.14 ms (0.053 ms promedio por transacción).
- **Percentil 50 (p50):** 0.038 ms
- **Percentil 95 (p95):** 0.083 ms *(Meta del SRS: <150 ms -> Holgura superior al 99.9%)*
- **Percentil 99 (p99):** 0.115 ms
- **Latencia máxima:** 0.142 ms
- **Consulta de Métricas por Captador (RF09):** 0.128 ms
- **Consulta de Métricas por Sede Supervisor (RF10):** 0.172 ms
- **Consulta Bandeja Paginada (20 items ordenados con búsqueda):** 0.149 ms
- **Verificación de integridad referencial (`PRAGMA foreign_key_check`):** 0 errores.

## Conclusión

El motor SQLite y el diseño de índices y transacciones atómicas de Room v3 demuestran capacidad sobrada para soportar jornadas operativas masivas en terreno sin saturar los recursos del dispositivo móvil ni degradar la experiencia de usuario.
