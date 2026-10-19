# QR ficticios para probar el escáner

Abre estas imágenes en otra pantalla o imprímelas; el teléfono con la app debe enfocarlas con su cámara.

| Prueba | Imagen | Resultado esperado |
|---|---|---|
| Formato admitido | [QR ficticio válido](qr-ficticio-valido.png) | Candidato Persona Ficticia Alfa, RUT 12345678-5. Sólo se aplica el RUT después de confirmar. |
| Formato no compatible | [QR no compatible](qr-no-compatible.png) | Aviso de formato no admitido y posibilidad de continuar manualmente; no borra ni reemplaza campos. |

## Recorrido

1. Ingresa como Captador A, elige un evento y abre Nueva captura.
2. Escribe nombres y apellidos ficticios para comprobar que se conservan.
3. Pulsa Escanear código QR asistido y concede Cámara si Android lo solicita.
4. Enfoca el QR válido, revisa el candidato y confirma: aplica sólo el RUT. Nombres y apellidos se completan manualmente. Si ya existe otro RUT, confirma explícitamente su reemplazo.
5. Enfoca el QR no compatible: debe informar el rechazo y conservar los datos. Continúa manualmente.
6. Sal y descarta el formulario sin guardarlo.

El QR válido contiene el formato sintético `G6DEMO:1` con datos de prueba; no acredita identidad ni pertenece a una cédula real. El segundo QR es legible, pero su contenido no pertenece al formato admitido: el rechazo es el comportamiento esperado, no un fallo de la cámara.

Ambos PNG son copias exactas de los fixtures de `android/app/src/androidTest/assets`. El escáner no guarda fotos ni contenido crudo del QR, ni abre enlaces. El soporte de cédula real queda aplazado a E2, sujeto a verificar formato y requisitos; no se añaden dependencias para esta entrega.

Consulta [instalación, cuentas y datos de muestra](../../DISTRIBUCION_DEMO.md).
