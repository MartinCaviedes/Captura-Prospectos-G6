# Documentación técnica

Actualizado 09/10/2026: base compartida e interfaces locales, posteriores a la preparación 0.1.13. [Selector y red](ROUND0_BASE.md), [formulario y dispositivos](ROUND1_INTERFACES.md), [propuesta de registros](RECORD_CONTRACTS.md). Esta documentación distingue lo construido del producto objetivo.

Lectura breve: portada → ENTREGAS. Desarrollo: SRS → contrato de la tarea. Verificación: README Android → ANDROID_BASELINE. No es necesario leer toda la propuesta E2 para ejecutar E1.

| Consulta | Documento |
|---|---|
| Reglas y criterios finales | [SRS 1.4](SRS.md) |
| Cuándo se implementa cada requisito | [ENTREGAS](ENTREGAS.md) |
| Campos y preferencias F2027 | [FORMULARIO_ADMISION_2027](FORMULARIO_ADMISION_2027.md) |
| Contrato de datos/acciones E1 | [LOCAL_MODEL](LOCAL_MODEL.md) |
| APIs y fixture de contexto implementados | [DEMO_CONTEXT](DEMO_CONTEXT.md) |
| Capas, responsabilidades y evolución | [ARCHITECTURE](ARCHITECTURE.md) |
| Pantallas, componentes y accesibilidad | [DESIGN](DESIGN.md) |
| Consentimiento, cifrado y claves | [SECURITY](SECURITY.md) |
| Configuración E1 y propuesta E2 | [ENVIRONMENTS](ENVIRONMENTS.md) |
| Semántica remota E2 propuesta | [API_CONTRACTS](API_CONTRACTS.md) |
| Verificaciones reales y límites | [ANDROID_BASELINE](ANDROID_BASELINE.md) |
| Evolución del producto | [CHANGELOG](CHANGELOG.md) |

Orden para desarrollar: SRS → ENTREGAS → contrato de la tarea. Datos E1: LOCAL_MODEL, DEMO_CONTEXT y SECURITY; UI: DESIGN. Las instrucciones de build están en [Android](../android/README.md).

Los fixtures JSON son casos documentales, no tests ejecutados automáticamente. La transcripción del cupón no es una oferta académica habilitada; el seed ejecutable es sintético y vive en DemoSeed. Servicios, OpenAPI y sincronización corresponden a E2 y aún no están implementados.
