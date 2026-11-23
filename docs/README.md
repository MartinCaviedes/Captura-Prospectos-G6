# Documentación técnica

Estado E2 · 22/11/2026: [resumen de entrega 1.1.0](ENTREGA_E2_1_1_0.md), [SRS Word 1.5](entrega/SRS_Captura_de_Prospectos_G6.docx?raw=true), [revisión documental de pautas](ENTREGA_DOCUMENTAL.md), [despliegue EC2](../backend/deploy/ec2/README.md), [cobertura](COVERAGE_VALIDATION.md) y [distribución firmada](RELEASE_DISTRIBUTION.md). El código está integrado en main mediante PR #23, con autorización de Martín para este cierre. La APK está publicada en el canal final `e2-1.1.0`; los estados fechados inferiores conservan su contexto histórico.

Actualización 17/11/2026 (`10ba22d`): eliminación de dependencias DEMO en el flujo de la aplicación, catálogo de Admisión Duoc UC 2027, creación/autenticación de cuentas reales en Room cifrado y por JWT contra el `api-gateway`, creación de eventos por captadores y supervisores, e integración móvil con Retrofit (`GatewayApiClient.kt`) hacia `users-service` y `prospects-service` (PostgreSQL).

Actualización 11/10/2026: [refinamiento visual E1](UI_REFINEMENT_E1.md), recorridos manuales y [evidencias del candidato](ANDROID_BASELINE.md). Los estados fechados 09/10 describen sus incrementos anteriores.

Lectura breve: portada → ENTREGAS. Desarrollo: SRS → contrato de la tarea. Verificación: README Android → ANDROID_BASELINE y [Backend](../backend/README.md).

| Consulta | Documento |
|---|---|
| Reglas y criterios finales | [SRS 1.5 Markdown](SRS.md) · [Word de entrega](entrega/SRS_Captura_de_Prospectos_G6.docx?raw=true) |
| Cuándo se implementa cada requisito | [ENTREGAS](ENTREGAS.md) |
| Campos y preferencias F2027 | [FORMULARIO_ADMISION_2027](FORMULARIO_ADMISION_2027.md) |
| Contrato de datos/acciones locales | [LOCAL_MODEL](LOCAL_MODEL.md) |
| Contexto, catálogo y cuentas locales | [DEMO_CONTEXT](DEMO_CONTEXT.md) |
| Capas, responsabilidades y evolución | [ARCHITECTURE](ARCHITECTURE.md) |
| Pantallas, componentes y accesibilidad | [DESIGN](DESIGN.md) |
| Consentimiento, cifrado y claves | [SECURITY](SECURITY.md) |
| Configuración de entornos | [ENVIRONMENTS](ENVIRONMENTS.md) |
| Contratos remotos y API de prospectos | [API_CONTRACTS](API_CONTRACTS.md) · [Backend](../backend/README.md) |
| Cuentas, sesión offline y sincronización E2 candidata | [Cuentas Android](ACCOUNT_ANDROID_VALIDATION.md) · [Sesión offline](OFFLINE_SESSION_VALIDATION.md) · [Sincronización](PROSPECT_SYNC_CONTRACT.md) |
| API externa de localidad a partir del GPS | [Proveedor, privacidad, código y pruebas](EXTERNAL_GEOCODING_CONTRACT.md) |
| Apodo y foto locales por cuenta | [Personalización del perfil](PROFILE_PERSONALIZATION.md) |
| Métrica, comandos e informes JaCoCo | [Validación de cobertura](COVERAGE_VALIDATION.md) |
| API implementada e interfaz de pruebas | [OpenAPI unificado](openapi/backend.openapi.json) · [Swagger: uso y validación](SWAGGER_VALIDATION.md) |
| Backend Windows/IntelliJ y acceso del Redmi | [Preparación local persistente](LOCAL_WINDOWS_SETUP.md) |
| Sesión al reabrir la app y avatar de Inicio | [Sesión recordada](REMEMBERED_SESSION.md) |
| Verificaciones reales y límites | [ANDROID_BASELINE](ANDROID_BASELINE.md) |
| Evolución del producto | [CHANGELOG](CHANGELOG.md) |

Orden para desarrollar: SRS → ENTREGAS → contrato de la tarea. Datos locales: LOCAL_MODEL, DEMO_CONTEXT y SECURITY; UI: DESIGN. Las instrucciones de compilación y prueba están en [Android](../android/README.md) y [Backend](../backend/README.md).
