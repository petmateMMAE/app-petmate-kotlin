# Checklist de seguridad de la app — Unidad 001 · T-042

> Estado: **16 pruebas de seguridad ejecutables, todas en verde** · R-31, C-19, C-24

Este documento responde la pregunta del documento de evaluación —
*¿se implementaron pruebas de seguridad? ¿cuántas resultaron?*— para el lado móvil,
igual que el backend la responde con sus 206.

La diferencia entre este checklist y una auditoría manual es que aquí **cada fila
tiene quién la vigila**. Una protección que nadie comprueba es una protección que
alguien va a aflojar sin darse cuenta.

## 1. Resumen

| | Cantidad |
|---|---|
| Pruebas de seguridad unitarias | **16** |
| Fallos | **0** |
| Clases | 3 (`app/src/test/java/mx/petmate/app/security/`) |
| Suite completa de la app | 460 pruebas, 0 fallos |
| Android Lint (`lintDebug`) | en verde |

Cada clase tiene su propio job en la matriz `feature-tests` del pipeline, así que
una protección aflojada aparece como un job rojo con nombre propio y no escondida
dentro de una corrida de 460 pruebas.

## 2. Qué protege cada control y quién lo vigila

| Control | Por qué importa | Quién lo vigila |
|---|---|---|
| Tráfico en claro prohibido | Un `http://` mal escrito viajaría sin cifrar | `ManifestSecurityTest` |
| Respaldo automático apagado | `adb backup` se llevaría la base de datos y los tokens | `ManifestSecurityTest` |
| Solo `MainActivity` exportada | Cada componente exportado es una puerta que otra app puede tocar | `ManifestSecurityTest` |
| Protección StrandHogg 2.0 | Sin `singleInstance` + `taskAffinity=""`, una app maliciosa se pone encima y cosecha la contraseña | `ManifestSecurityTest` |
| Permisos dentro del catálogo de la spec 003 | Un permiso nuevo debe ser decisión de producto, no descuido | `ManifestSecurityTest` |
| Cámara no obligatoria para instalar | Declarar `CAMERA` excluiría teléfonos sin cámara; el escáner tiene código manual | `ManifestSecurityTest` |
| La transferencia entre dispositivos no se lleva la sesión ni la BD | `allowBackup="false"` **no** apaga el D2D en Android 12+ | `BackupRulesSecurityTest` |
| El respaldo en la nube tampoco | Defensa en profundidad si alguien cambia `allowBackup` | `BackupRulesSecurityTest` |
| Las reglas de Android 11 o anterior tampoco filtran | `minSdk` es 24: esos teléfonos siguen en el alcance | `BackupRulesSecurityTest` |
| Los archivos de reglas no son la plantilla de Android Studio | Un archivo con las reglas comentadas *parece* configurado | `BackupRulesSecurityTest` |
| El manifiesto sigue apuntando a los dos archivos de reglas | Si se borra el atributo, las exclusiones dejan de aplicar en silencio | `BackupRulesSecurityTest` |
| Sin secretos con forma reconocible en el APK | Un secreto en el APK es público: se saca con `apktool` | `NoEmbeddedSecretsTest` |
| Sin credenciales escritas a mano fuera de `ui/` | Una credencial fija en el código se filtra al repositorio | `NoEmbeddedSecretsTest` |
| La URL de producción no está incrustada | `BASE_URL` entra por `local.properties` → `buildConfigField` | `NoEmbeddedSecretsTest` |
| `local.properties` no se versiona | Ahí viven `BASE_URL` y las credenciales de firma | `NoEmbeddedSecretsTest` |
| El escaneo encuentra archivos y los patrones funcionan | Un escáner roto pasaría en verde sin mirar nada | `NoEmbeddedSecretsTest` |
| Secretos en el repositorio y su historial | — | gitleaks (etapa 1 del pipeline) |
| SAST móvil (OWASP MASVS) | — | mobsfscan (los hallazgos graves bloquean) |
| SAST general | — | CodeQL |
| Dependencias vulnerables | — | Trivy (HIGH y CRITICAL bloquean) + SBOM con Syft |
| El APK de release no es depurable | — | prueba de humo (etapa 8) |

## 3. Cómo comprobar que las pruebas sirven (C-19, C-24)

Una prueba que nadie vio fallar no se sabe si protege. Estas violaciones se
provocaron a propósito el 27/11/2026 y cada una fue cazada por su prueba. Para
repetirlo: aplica el cambio, corre `./gradlew :app:testDebugUnitTest`, y **deshaz
el cambio**.

| Violación que provocas | Qué debe fallar |
|---|---|
| Quitar `<device-transfer>` de `res/xml/data_extraction_rules.xml` | `BackupRulesSecurityTest > la transferencia entre dispositivos...` |
| Poner una cadena con forma de clave de Google (`AIza...`, 39 caracteres) en cualquier `.kt` de `src/main` | `NoEmbeddedSecretsTest > no hay secretos con forma reconocible...` |
| Cambiar `usesCleartextTraffic` a `"true"` en el manifiesto | `ManifestSecurityTest > el trafico en claro esta prohibido` |
| Poner `android:exported="true"` en otro componente | `ManifestSecurityTest > solo MainActivity esta exportada` |
| Agregar un `<uses-permission>` fuera del catálogo de la spec 003 | `ManifestSecurityTest > no se piden permisos peligrosos...` |
| Dejar `res/xml/backup_rules.xml` solo con comentarios | `BackupRulesSecurityTest > los archivos de reglas no quedaron con la plantilla...` |
| Quitar `local.properties` de `.gitignore` | `NoEmbeddedSecretsTest > local properties no se versiona` |

### Por qué el manifiesto y `res/xml` están declarados en `build.gradle.kts`

Estas pruebas abren los archivos reales con `File(...)`, y Gradle no tiene forma
de saber que son entradas suyas. Antes de declararlas, **un cambio hecho solo en
el manifiesto o en `res/xml` dejaba la tarea en `UP-TO-DATE` y las pruebas no se
volvían a ejecutar**: alguien podía aflojar una protección, ver la suite en verde
en local y subirlo. En CI no pasaba, porque el checkout es limpio. El bloque
`tasks.withType<Test>` de `app/build.gradle.kts` lo arregla, y está comprobado:
con él, cambiar solo el XML vuelve a correr las 460 pruebas y falla.

## 4. Decisiones tomadas a propósito

| Decisión | Motivo |
|---|---|
| `DemoAccount.PASSWORD` se queda en el código, como excepción escrita en `NoEmbeddedSecretsTest` | Es una cuenta de demostración pública, sin datos reales, documentada en `UserProfile.kt`; existe con las mismas credenciales en el backend real y en el simulado. La excepción es por archivo **y** por valor: una credencial nueva en ese mismo archivo sí falla |
| La regla de credenciales escritas a mano no mira `ui/` | Los `@Preview` de Compose llevan datos de ejemplo (`password = "petmate123"`) que no son de nadie. Perseguirlos volvería la prueba ruido, y una prueba ruidosa termina con un `@Ignore` encima. Un secreto de verdad en `ui/` lo sigue cazando la regla por forma, que sí recorre todo `src/main` |
| Se excluyen dominios completos (`path="."`) y no archivo por archivo | Así una tabla nueva o un archivo de preferencias nuevo queda excluido solo, sin que nadie tenga que acordarse de volver al XML |
| Las reglas de respaldo se escribieron aunque hoy `allowBackup="false"` ya las haga inertes en Android 11 o anterior | Para el día en que alguien ponga `allowBackup="true"` sin pensar en lo que se lleva consigo |

## 5. Lo que sigue siendo manual

Estas no están automatizadas y hay que revisarlas a mano antes de liberar:

- [ ] Que el token de sesión no aparezca en el `Logcat` de un build de release.
- [ ] Que las pantallas con datos del carnet no salgan en la captura del
      multitarea (`FLAG_SECURE`), si el equipo decide que aplica.
- [ ] Revisar los informes de mobsfscan y CodeQL del último pipeline: hoy quedan
      como artefactos y nadie los lee si el job pasa.
- [ ] Verificar en un dispositivo real que una migración entre teléfonos no se
      lleva la sesión. Las exclusiones están declaradas y la prueba las vigila,
      pero el comportamiento del D2D depende del fabricante.

> Las pruebas instrumentadas no entran aquí: en esta máquina
> `connectedDebugAndroidTest` revienta por memoria. La secuencia que funciona
> está en el README.
