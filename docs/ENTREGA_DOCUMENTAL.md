# Revisión documental de entrega E2 y evaluación final

22/11/2026. Revisamos las pautas «Ev. Parcial 4_Encargo_Estudiante_ DSY1105» (6 páginas) y «EFT_DSY1105_Encargo_ Estudiante» (7 páginas), además de las aclaraciones de alcance comunicadas por el grupo. Este registro identifica archivos y acciones; no acredita recepción ni aprobación docente.

## README y archivos

| Solicitud | Referencia de pauta | Evidencia disponible |
|---|---|---|
| Nombre, integrantes y funcionalidades | Parcial 4, p. 3; EFT, p. 5 | README principal: caso, equipo, alcance y funciones. |
| Endpoints propios y externos | Parcial 4, p. 3; EFT, p. 5 | README: métodos, rutas, bases, API externa y OpenAPI/Swagger. |
| Pasos de ejecución | Parcial 4, p. 3; EFT, p. 5 | README: Android local/AWS, backend Windows y enlaces a PostgreSQL/EC2. |
| APK firmada y evidencia; formato/ubicación de llave | Parcial 4, p. 3; EFT, p. 5 | Release final, verificación apksigner y misma identidad en PKCS12/JKS. README identifica custodia y ruta privada. |
| Código Android y microservicios, historial por integrante | Parcial 4, pp. 3–4; EFT, p. 5 | Repositorio público, directorios android/backend, historial y PR. |
| SRS | Indicación del profesor comunicada por el grupo; no aparece nombrado expresamente en las dos pautas revisadas | SRS.md revisión 1.5 y Word de igual contenido, enlazados desde README. |

La imagen de firma presenta resultados reales de keytool/apksigner, no una captura del asistente de Android Studio. Si el docente pide específicamente esa captura, corresponde añadirla durante la preparación de la demostración; no se simula una pantalla del IDE.

La clave privada no se publica. Si se solicita como archivo en la plataforma de evaluación, el custodio la entrega por el canal privado indicado por el docente. Su copia JKS tiene el mismo certificado que la APK. El respaldo externo de clave y contraseña no se acredita por tener dos formatos en el mismo disco.

## Acciones de entrega

La pauta de Parcial 4 (p. 4) y EFT (p. 5) describe entrega grupal en AVA y activación individual con identificación del integrante, equipo y proyecto. También restringe modificaciones posteriores al plazo de entrega. No se establece aquí una fecha ni se presume que estas acciones se hayan realizado.

El grupo recuerda que el profesor solicitó enlace GitHub y SRS. Esa indicación guía los archivos preparados, pero no permite afirmar que la carga grupal o la activación individual estén dispensadas. Debemos verificar el canal exacto en AVA o con el docente antes de entregar. En los documentos revisados no identificamos otro informe independiente que deba generarse además de las evidencias y archivos descritos; esto no sustituye nuevas indicaciones del profesor.

## Aclaraciones y límites

Trello se excluye por la indicación docente reportada. Utilizamos tres roles con función real; el grupo reporta que no se exige forzar cuatro. Recuperación de contraseña y acceso institucional se mantienen fuera del incremento. Estas aclaraciones y decisiones no equivalen al cumplimiento literal de todos los ítems de las pautas; la evaluación final corresponde al docente.

El SRS conserva requisitos amplios con límites explícitos. Incluye la diferencia entre el objetivo original de retener NEEDS_CONTEXT y el transporte actual que admite ese estado, además de las ampliaciones de consolidación, catálogo y asignaciones remotas. No se realizan cambios funcionales como parte de la revisión documental.
