# API externa · localidad aproximada del GPS

22/11/2026 · candidato local. Se mantiene el alcance fundamental de E2/examen acordado por Martín. Eventos/asignaciones/catálogos remotos, consolidación entre dispositivos y otras ampliaciones del SRS quedan aplazados para evaluar después de los pendientes fundamentales y de la posible mejora del QR; no se descartan ni se declaran cumplidos.

## Decisión y límites

Se integra **BigDataCloud Client-Side Reverse Geocoding**, sin cuenta ni clave API, mediante HTTPS directo desde Android. Su política permite consultar coordenadas actuales del propio dispositivo obtenidas con permiso; no se usa el endpoint gratuito desde backend, con ubicaciones de eventos, registros históricos, coordenadas de otras personas o procesos por lotes.

La consulta es opcional y explícita después de adquirir/confirmar GPS durante el formulario. Antes del botón se explica que se enviarán coordenadas e IP al proveedor y se enlaza su política de privacidad. El proveedor usa esa asociación para mejorar su geolocalización IP, según su documentación. No se envían nombre, RUT, correo, teléfono, identidad del captador, JWT ni datos de la ficha. No se hace una consulta automática al abrir el formulario, confirmar GPS o recuperar red.

El nombre de localidad describe el lugar del **dispositivo que captura**. No rellena domicilio/comuna del prospecto, establecimiento ni sede; no acredita una dirección exacta ni una comuna administrativa oficial. El GPS y sus coordenadas siguen siendo la evidencia local; la etiqueta externa se conserva solo en memoria mientras esté abierto el formulario, no modifica Room ni el contrato de sincronización.

## Contrato de transporte

`GET https://api.bigdatacloud.net/data/reverse-geocode-client?latitude={lat}&longitude={lon}&localityLanguage=es`

Cliente Retrofit/OkHttp independiente del gateway, sin cabeceras de sesión, cookies, caché HTTP, logs de URL/payload ni reintentos automáticos. Timeout total 5 segundos; conexión/lectura de 3 segundos. Sin redirecciones para evitar reenviar coordenadas a otro destino. Adaptador sustituible mediante la interfaz de dominio.

Respuesta utilizada: `latitude`, `longitude`, `lookupSource`, `locality`, `city`, `principalSubdivision`, `countryName`. Se aceptan los indicadores de coordenadas documentados por el proveedor (`coordinates` y `reverseGeocoding`), no estimaciones por IP. Se comprueba que las coordenadas devueltas correspondan a las solicitadas, números válidos y nombres de longitud acotada. Se muestra localidad (o ciudad), región y país cuando estén disponibles, con crédito BigDataCloud.

Solo se permite una adquisición nativa reciente (máximo 60 segundos, precisión local válida). La ubicación histórica inicial de una edición no habilita consultar. Si envejece se solicita adquirir GPS otra vez; no se sustituye silenciosamente por IP. Una consulta en curso no se duplica y el mismo resultado se reutiliza en memoria para esa posición. Reintentos solo por botón, con intervalo mínimo de 5 segundos.

Fallos de red/timeout, límite 429, restricción 402, respuesta inválida o servicio no disponible muestran un mensaje recuperable. 401/403 de este proveedor **no** invalidan la cuenta de la aplicación. Se permite guardar sin etiqueta geográfica. Cambiar/quitar GPS, descartar, guardar o cerrar cancela/descarta resultados obsoletos; no cambia datos del formulario ni el consentimiento del prospecto.

## Fuentes y validación prevista

- [Endpoint y esquema oficial](https://www.bigdatacloud.com/docs/free-api/free-reverse-geocode-to-city-api).
- [Uso permitido del endpoint gratuito](https://www.bigdatacloud.com/docs/article/fair-use-policy-for-free-client-side-reverse-geocoding-api).
- [Motivo de la oferta gratuita y uso de señales de ubicación/IP](https://www.bigdatacloud.com/docs/article/why-is-reverse-geocoding-api-free).
- [Privacidad del proveedor](https://www.bigdatacloud.com/privacy-and-cookie-policy).
- [SDK oficial: indicador coordinates](https://github.com/bigdatacloudapi/flutter-reverse-geocode-client).
- [Artículo oficial: indicador reverseGeocoding](https://www.bigdatacloud.com/blog/new-feature-update-free-client-side-reverse-geocoding-api-with-ip-geolocation-fallback).

Las pruebas automáticas usan posiciones ficticias únicamente contra dobles/HTTP local controlado; **no envían coordenadas simuladas al endpoint gratuito**. La prueba real del proveedor requiere un teléfono con ubicación actual, permiso y pulsación explícita del usuario. No se atribuye éxito contra el proveedor a la compilación o a un mock, ni se declara cumplimiento institucional de privacidad.

## Implementación y pruebas ejecutadas

| Archivo Android (paquete base `io/github/grupo6/capturaprospectos`) | Responsabilidad |
|---|---|
| `domain/device/ReverseGeocoder.kt` | Contrato sustituible, resultado y validación de posición reciente |
| `data/remote/BigDataCloudGeocoder.kt` | DTO/Retrofit independientes, HTTPS, timeout, validación y límite de frecuencia |
| `ui/admission/AdmissionFormViewModel.kt` | Acción explícita, posición nativa actual, cancelación y estados |
| `ui/admission/LocalityLookupCard.kt` | Aviso, privacidad, acción, carga, errores y atribución |
| `ui/admission/AdmissionFormComponents.kt` | Conexión de acciones y tarjeta de localidad |
| `data/local/RoomContextSession.kt` / `ui/context/ContextSessionOwner.kt` | Inyección del adaptador en altas y edición |
| `ui/records/RecordTrayViewModel.kt` | El editor recibe el mismo contrato; su ubicación histórica no habilita consultas |

Estas rutas están bajo `android/app/src/main/java/`. No se añade migración Room, permisos ni dependencia de Google Maps. Se corrigió también el mensaje de guardado heredado de E1 que todavía decía que no existía envío remoto: ahora indica guardado local y remite al estado del envío en Registros.

| Comprobación | Resultado |
|---|---|
| `testDemoDebugUnitTest` | 241 pruebas, 0 fallos; 17 casos nuevos entre adaptador HTTP y ViewModel |
| `assembleDemoDebug` | APK debug generada, bibliotecas de 32 y 64 bits incluidas |
| `lintDemoDebug` | 0 errores, 38 advertencias; sin baseline nuevo ni reglas desactivadas |
| Paquete instrumentado `ui.admission`, emulador API 34 | 11 pruebas, 0 fallos; 5 nuevas de consulta explícita, aviso, carga, resultado y error |

HTTP loopback verifica parámetros reales, idioma, ausencia de Authorization/Cookie, rechazo de estimación por IP/coordenadas diferentes, JSON incorrecto, límites 402/429 y que 401/403 externos no cierren la sesión propia. ViewModel verifica que no hay consultas automáticas, no se duplican pulsaciones, no cambia comuna/consentimiento, se descartan respuestas tardías y el guardado puede continuar sin etiqueta.

Logs locales fuera del repositorio: `Residuos ChatGPT/geocoding-validation.log`, `geocoding-device-build.log`, `geocoding-ui.log` y `geocoding-phone-build.log`. Candidato en `feature/e2-api-geocodificacion`; mantiene los incrementos anteriores sin publicar.

## Prueba física preparada

Redmi 9A, Android 10/API 29, ARM de 32 bits. Se verificó conexión ADB autorizada y compatibilidad del APK. El endpoint propio del gateway ahora se configura al compilar mediante `-Pg6ApiBaseUrl=...`, sin credenciales: HTTPS para servidor remoto, o HTTP de desarrollo en localhost/127.0.0.1/10.0.2.2. El valor predeterminado del emulador se conserva. No cambia el endpoint HTTPS independiente de BigDataCloud.

Para la prueba USB se construyó una APK con `http://127.0.0.1:8080/` y se preparó `adb reverse tcp:8080 tcp:18080` hacia gateway/usuarios/prospectos locales temporales con cuentas ficticias. No se expuso el backend en la red ni se usó AWS. APK local: `Docs/revision-api-externa/G6-api-externa-Redmi9A-debug.apk` dentro del workspace. La primera instalación tenía SHA256 `165d351a3f4185fe4a3a33448817f37cb2f1afda61fa3fec6149c848964cf21f`; la actualización de espera GPS de 30 segundos tiene SHA256 `0e86302f5e2a260e7c32494f295eaac3d64fe0e6ef0738364e9e99cfb2e609ac`.

MIUI rechazó los primeros intentos ADB con INSTALL_FAILED_USER_RESTRICTED. Como alternativa se copió la misma APK a `Descargas/Captura-G6-API-debug.apk` del teléfono; no se borró ninguna aplicación ni se desactivaron controles del sistema.

Tras confirmar Martín que activó Instalar vía USB, `adb install -r` devolvió Success. Se abrió MainActivity; el proceso continuó activo y la consulta de AndroidRuntime de ese proceso no mostró errores. El gateway temporal respondió HTTP 200 a readiness y el puerto USB quedó preparado. Esto acredita instalación y arranque en Redmi 9A, no un login ni una consulta real de localidad. La comprobación del proveedor queda pendiente de pulsación explícita del usuario con GPS actual e internet; no se declara prueba real exitosa hasta observarla o recibir su reporte.

Martín confirmó después que puede acceder y navegar, pero la adquisición GPS agota 10 segundos. A petición suya se amplió el límite a **30 segundos**, manteniendo precisión/antigüedad y confirmación explícita. Los mensajes de carga/timeout leen la misma configuración. Permisos FINE/COARSE concedidos y ubicación activada verificados sin consultar coordenadas. Dos pruebas existentes comprueban recepción después de 15 segundos, vencimiento exacto a 30 y cierre de listeners. Resultado: 241 JVM aprobadas, Lint 0 errores/38 advertencias y APK actualizada mediante `install -r` sin desinstalar ni resetear Room. Se restableció el backend temporal al haber expirado. La prueba real de adquisición y BigDataCloud continúa pendiente con el nuevo límite.

## Dirección aproximada · extensión candidata

Martín reportó que con 30 segundos adquirió GPS y que BigDataCloud mostró correctamente localidad/comuna, región y país. Se registra como evidencia física reportada por el usuario; no se almacenan las coordenadas ni el lugar concreto en este informe.

A petición de Martín se prueba una segunda consulta opcional para **calle y número cuando existan** mediante `android.location.Geocoder`. BigDataCloud no devuelve direcciones de calle por diseño; su integración REST se conserva y sigue cubriendo la API pública externa. Geocoder utiliza la implementación disponible en el teléfono y puede recurrir a un servicio de red; no configura una clave de Google Maps. Aviso y botón independientes explican esa transferencia. El resultado solo se muestra como referencia aproximada y nunca modifica la residencia del prospecto ni se almacena en Room.

Se aceptan calle (`thoroughfare`) y número opcional (`subThoroughfare`), con longitud acotada. Si no se proporciona calle, no se presenta una ciudad como si fuera dirección. Se reutilizan únicamente GPS nativo reciente y confirmado, límites de antigüedad/precisión, cancelación de resultados obsoletos y guardado sin resultado. Consultas serializadas, máximo una operación nativa pendiente y espera de UI de 5 segundos; una llamada nativa antigua puede continuar en su servicio después del timeout, sin bloquear la UI ni generar nuevos hilos por reintento.

En Android >=33 se usa callback asíncrono; en versiones anteriores, llamada en un ejecutor dedicado acotado. No hay operación bloqueante de geocodificación en el hilo principal. La disponibilidad y precisión no están garantizadas y el número puede faltar o corresponder a un edificio cercano: no se declara dirección exacta.

Fuentes: [Geocoder de Android](https://developer.android.com/reference/android/location/Geocoder), [campos de Address](https://developer.android.com/reference/android/location/Address) y [límite de BigDataCloud a localidades](https://www.bigdatacloud.com/reverse-geocoding). Las pruebas automáticas de esta extensión usan una fuente nativa simulada y no hacen consultas externas con posiciones ficticias.

Implementación: `data/device/AndroidAddressGeocoder.kt`, inyectada como otro `ReverseGeocoder` en `ContextSession`, `ContextSessionOwner` y el editor de bandeja. El formulario comparte validación, vigencia y cancelación entre localidad y dirección, con estados separados; una consulta no sustituye la otra. `StreetAddressLookupCard` añade aviso, botón explícito y resultado aproximado. No cambian Room ni los payloads del backend.

Verificación de la extensión: **251 pruebas JVM, 0 fallos**, **13 instrumentadas del paquete ui.admission aprobadas**, Lint **0 errores/38 advertencias**. Siete casos nuevos verifican calle/número opcional, respuesta incompleta, servicio ausente, timeout de 5 segundos, cancelación, callback tardío/duplicado y fallo de red. Tres nuevos de ViewModel verifican acción explícita/aislamiento de la comuna, ausencia de consultas paralelas y rechazo de ubicación de evento/recreación. Dos nuevos de UI verifican aviso y atribución aproximada; el recorrido del formulario prueba ambas consultas con dobles explícitos. No equivalen a una calle real devuelta por el teléfono.

Logs: `Residuos ChatGPT/street-address-validation.log` y `street-address-ui.log`. APK USB: `Docs/revision-api-externa/G6-api-externa-direccion-Redmi9A-debug.apk`, SHA256 `f23128dc9c98676883130af9465dc3a1a374ea99663228efdbed23c1f22586b8`. Se conserva también el APK anterior de localidad/GPS de 30 segundos. `adb install -r` devolvió Success en el Redmi y se abrió MainActivity; la copia de Descargas también se actualizó. No se publican ni se registran direcciones personales en la documentación.

Resultado físico reportado por Martín el 22/11/2026: la consulta de Android mostró que el servicio del teléfono no entregó una dirección de calle y permitió continuar con la localidad, sin problemas observados por el usuario. Se acredita ese comportamiento alternativo en Redmi 9A/API 29, junto con el resultado de localidad correcto reportado previamente para BigDataCloud. No acredita devolución real de calle/número, disponibilidad en otras zonas/dispositivos ni determina si faltaban datos, servicio o tiempo de respuesta. Se mantiene la dirección como mejora opcional; no bloquea el cierre de este incremento de API externa ni se amplía el alcance actual para sustituirla por otro proveedor.
