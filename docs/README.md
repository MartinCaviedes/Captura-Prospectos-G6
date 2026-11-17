# Documentación técnica

Actualización 17/11/2026 (`10ba22d`): eliminación de dependencias DEMO en el flujo de la aplicación, catálogo de Admisión Duoc UC 2027, creación/autenticación de cuentas reales en Room cifrado y por JWT contra el `api-gateway`, creación de eventos por captadores y supervisores, e integración móvil con Retrofit (`GatewayApiClient.kt`) hacia `users-service` y `prospects-service` (PostgreSQL).

Actualización 11/10/2026: [refinamiento visual E1](UI_REFINEMENT_E1.md), recorridos manuales y [evidencias del candidato](ANDROID_BASELINE.md). Los estados fechados 09/10 describen sus incrementos anteriores.

Lectura breve: portada → ENTREGAS. Desarrollo: SRS → contrato de la tarea. Verificación: README Android → ANDROID_BASELINE y [Backend](../backend/README.md).

| Consulta | Documento |
|---|---|
| Reglas y criterios finales | [SRS 1.4](SRS.md) |
| Cuándo se implementa cada requisito | [ENTREGAS](ENTREGAS.md) |
| Campos y preferencias F2027 | [FORMULARIO_ADMISION_2027](FORMULARIO_ADMISION_2027.md) |
| Contrato de datos/acciones locales | [LOCAL_MODEL](LOCAL_MODEL.md) |
| Contexto, catálogo y cuentas locales | [DEMO_CONTEXT](DEMO_CONTEXT.md) |
| Capas, responsabilidades y evolución | [ARCHITECTURE](ARCHITECTURE.md) |
| Pantallas, componentes y accesibilidad | [DESIGN](DESIGN.md) |
| Consentimiento, cifrado y claves | [SECURITY](SECURITY.md) |
| Configuración de entornos | [ENVIRONMENTS](ENVIRONMENTS.md) |
| Contratos remotos y API de prospectos | [API_CONTRACTS](API_CONTRACTS.md) · [Backend](../backend/README.md) |
| Verificaciones reales y límites | [ANDROID_BASELINE](ANDROID_BASELINE.md) |
| Evolución del producto | [CHANGELOG](CHANGELOG.md) |

Orden para desarrollar: SRS → ENTREGAS → contrato de la tarea. Datos locales: LOCAL_MODEL, DEMO_CONTEXT y SECURITY; UI: DESIGN. Las instrucciones de compilación y prueba están en [Android](../android/README.md) y [Backend](../backend/README.md).
