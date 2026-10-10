# Ronda 2 · DEVICE-01-B: Recursos nativos (GPS y Cámara)

Incremento local sobre la base de ronda 2 (`feature/e1-ronda2-device-resources`). Implementa los recursos nativos exigidos para la entrega E1 (P2/P3): proveedor de ubicación GPS nativo (`AndroidLocationProvider`), asistente de cámara y lectura de códigos QR sintéticos (`DemoQrParser`), e integración completa con el formulario de admisión y persistencia.

## Alcance técnico implementado

| Entregable | Responsabilidad e integración |
|---|---|
| **GPS-01-A (RF04)** | `AndroidLocationProvider` mediante `LocationManager` del framework de Android. Verificación de permisos de tiempo de ejecución (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), estado de proveedores satelitales/red, timeout de trabajo (10s) y cancelación reactiva inmediata sin fugas de listeners, sin caché arbitrario ni fallbacks implícitos. |
| **POL-01-A (Ubicación)** | Aplicación de `LocationPolicy` (precisión <= 100 m, antigüedad <= 5 min, coordenadas válidas). Diferenciación estricta entre candidato GPS (`CaptureLocation.Device`) y confirmación explícita de ubicación de evento (`CaptureLocation.EventConfirmed`). |
| **CAM-01-A (RF11)** | Asistente de lectura QR en interfaz Compose (`AdmissionFormComponents`). Integración con `DemoQrParser` para formato sintético `G6DEMO:1:{"fullName":"...","rut":"..."}`. Validación de formato, visualización de candidato reconocido y confirmación manual obligatoria antes de volcar al formulario. Cero almacenamiento de imágenes o payloads crudos. |
| **FORM-01-C (Persistencia)** | Vinculación de ubicación en `AdmissionFormViewModel.save()`. Las admisiones completas con ubicación confirmada se registran en estado `READY`; las completas sin ubicación confirmada se guardan en estado `NEEDS_CONTEXT`; los borradores consentidos se guardan en `DRAFT`. |
| **PERM-01-A (Manifiesto)** | Declaración en `AndroidManifest.xml` de permisos `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA` y de features no obligatorias correspondientes. |

## Reglas de negocio y privacidad preservadas

1. **Sin inferencia ni fallbacks implícitos:** El GPS nunca infiere automáticamente la comuna de residencia o el establecimiento educativo del prospecto. La confirmación de las coordenadas de la sede del evento es una acción explícita del captador, nunca un sustituto silencioso de una falla de GPS.
2. **Privacidad del código QR:** El asistente no ejecuta URLs, no abre navegadores ni envía datos a servicios externos. La cédula real no se procesa sin especificación autorizada; únicamente se acepta el formato sintético de prueba.
3. **No persistencia de imágenes ni datos crudos:** La cámara no almacena fotografías en almacenamiento local ni temporal; el texto escaneado es efímero y se descarta si el usuario cancela o sale del diálogo.
4. **Estados de captura acordes al contrato:**
   - Captura completa + consentimiento + ubicación válida → `READY` (transporte `NOT_QUEUED`).
   - Captura completa + consentimiento + sin ubicación → `NEEDS_CONTEXT` (transporte `NOT_QUEUED`).
   - Borrador explícito + consentimiento → `DRAFT` (transporte `NOT_QUEUED`).

## Pruebas agregadas

- `LocationPolicyTest` (9 pruebas JVM):
  - Aceptación de posición GPS dentro de umbrales (15 m, antigüedad 10s).
  - Rechazo de precisión insuficiente (> 100 m).
  - Rechazo de posiciones antiguas (> 5 min) o en el futuro.
  - Rechazo de coordenadas inválidas (fuera de rango latitud/longitud o NaN).
  - Aceptación de confirmación de evento cuando coincide con la sede y actor declarados.
  - Rechazo de confirmación de evento con coordenadas o actor no coincidentes.
- `AndroidLocationProviderTest` (3 pruebas JVM):
  - Exigencia de timeout estrictamente positivo.
  - Detección de permisos denegados sin quiebre.
  - Manejo de LocationManager no disponible.
- `AdmissionFormDeviceTest` (9 pruebas JVM):
  - Adquisición de GPS y aplicación de candidato validado según política.
  - Manejo tipado de permisos denegados, servicios desactivados y timeout.
  - Confirmación manual de ubicación del evento.
  - Limpieza de ubicación asignada.
  - Lectura de código QR sintético válido y precarga de nombres/RUT tras confirmación.
  - Reporte de error ante código QR no compatible o JSON mal formado.
  - Descarte del modal de escaneo sin alterar datos del formulario.
- `LocationAndroidTest` (1 prueba instrumental Android):
  - Inicialización y consulta segura de `AndroidLocationProvider` en entorno de dispositivo/emulador.
