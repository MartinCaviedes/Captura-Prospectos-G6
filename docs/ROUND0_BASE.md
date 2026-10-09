# Base compartida: incremento local del 09/10/2026

La actividad principal abre el selector demo con `ContextSessionOwner`, `DemoContextViewModel`, `DemoContextRoute` y `RoomDemoContextRepository`. La apertura cifrada se ejecuta fuera de Main. El dueño sobrevive a cambios de configuración; al cerrarse cancela y espera a los consumidores antes de cerrar Room. Un fallo de apertura ofrece reintento explícito, sin borrar ni regenerar datos o claves. Inicialización, perfil, evento y salida requieren acciones explícitas.

`AndroidConnectivityObserver` implementa el contrato de red aislado: UNKNOWN, OFFLINE, CONNECTED_UNVALIDATED y VALIDATED. Registra callbacks mientras hay consumidor y los libera al cancelar. Usa únicamente ACCESS_NETWORK_STATE, permiso normal. No afirma disponibilidad del backend ni modifica estados de envío. La presentación de red y el recorrido de captura se integran en ronda 2. Referencia: [estado de red Android](https://developer.android.com/develop/connectivity/network-ops/reading-network-state).

## Revisión técnica de contratos

- Se conserva AdmissionRepository para create/get F2027 y la lectura Legacy; no se convierte a CaptureInput.
- El selector delega autorización, inicialización y lecturas a RoomDemoContextRepository. Cambio de perfil elimina selección de evento; preferencias de sede no conceden permisos.
- A06 continúa cubierto por pruebas de filas corruptas, sin borrar datos. No se oculta cancelación.
- El cierre del recurso tiene pruebas de cancelación antes y durante la apertura, reintento y orden de liberación. Lint detectó ViewModelStore.put restringido en producción; se sustituyó por ViewModelProvider, sin suprimir el control.
- No se modificó esquema Room v3, Gradle, F2027, cifrado ni alcance E1/E2. La auditoría de edición/anulación todavía exige desarrollo de ronda 2.

## Evidencia y límites

79 pruebas JVM aprobadas (71 anteriores y 8 nuevas); APK de aplicación e instrumental compilados; Lint 19 avisos y 0 errores. Xiaomi 11T, Android 14/API 34: ejecución del paquete `data` con **28 pruebas aprobadas en 30,139 s**, incluyendo 27 de persistencia y una de conectividad que abre/cierra 110 colecciones. No incluye ExampleInstrumentedTest ni las seis UI presentacionales anteriores.

Ejecución separada de `ContextRoomUiTest`: **1 prueba aprobada en 16,302 s**, con apertura asistida de ComponentActivity por ADB por restricción MIUI. Verifica preparación explícita → perfil → evento → lectura desde otra conexión cifrada → cambio de perfil → salida; ninguna captura creada. Base exclusiva `test-route-<UUID>.db`, limpiada sólo por su prueba. No es una prueba de muerte de proceso: esa persistencia se cubre en pruebas de repositorio. La actividad principal real abrió y mostró el estado sin inicializar; los toques ADB fueron rechazados por MIUI, sin cambios de permisos.

Ampliación posterior durante R1, sin cambiar el adaptador: pérdida/recuperación Wi-Fi VALIDATED → OFFLINE → VALIDATED aprobada en 2,676 s y Wi-Fi restaurado. Ver ANDROID_BASELINE para la ejecución separada. Pendientes de aceptación: revisión humana y recorrido visual de accesibilidad/tema; portal cautivo aún no probado. No hay RF completo cerrado ni publicación por estos resultados.
