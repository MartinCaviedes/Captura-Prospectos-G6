# Cambios del producto

## Base preparada para revisión — 07/10/2026

Documentación pública autosuficiente sobre `d658e76`, preparación 0.1.11. Conserva SRS 1.4, alcance E1/E2 y propuestas identificadas; agrega entrada, contrato F2027, instrucciones y evidencia. No incorpora nuevas funcionalidades Android ni acredita publicación, revisión humana o entrega.

## Contexto/catálogo y prueba Android — 06/10/2026

`2298220`: Room v3, migración aditiva 2→3, seed sintético explícito/idempotente, perfiles/eventos/selección, catálogo independiente y fuente para AdmissionRepository. 58 JVM pasan y 24 instrumentadas aprobadas en Android 14/API 34. `d658e76` registra esa prueba; UI/administración pendientes.

## F2027 — 06/10/2026

`83bbed2`: dominio F2027, nombres separados, nacimiento/residencia, actividad, preferencias opcionales y 1–3 carreras priorizadas. `91b4983`: Room v2, alta/detalle F2027, lectura versionada y migración 1→2 conservadora. 56 JVM pasaron; 16 instrumentadas v2 aprobadas posteriormente. `731c8a6` documenta esa ejecución.

## Persistencia inicial y base — 03/10/2026

`7e99f72`: proyecto demo y validadores locales. `2c63997`: Room/SQLCipher/Keystore, altas/detalle, esquema v1 y pruebas iniciales. Los contratos antiguos permanecen por compatibilidad; las altas F2027 usan AdmissionRepository. Se conserva la historia y autoría reales, sin presentar commits como revisión cruzada.
