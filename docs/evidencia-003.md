# Evidencia — Unidad 003 · Permisos

> Fecha: 17/11/2026 · Estado: **escrita, pendiente de verificar en dispositivo**

## 1. Decisiones tomadas en las preguntas abiertas

| Pregunta | Decisión | Por qué |
|---|---|---|
| Q-01 ¿Precisa o aproximada? | Se piden las dos; con la aproximada basta | El sistema deja elegir al usuario desde API 31 y ordenar por cercanía funciona con cualquiera |
| Q-02 Formato del código manual | 8 caracteres alfanuméricos | Es el que ya valida `ScanViewModel.resolve` |
| Q-03 ¿Cuándo se piden notificaciones? | Solo al activarlas en 006 o 017 | R-01; queda listo `NotificationFallback` para esas pantallas |
| Q-05 ¿Selector de fotos o permiso clásico? | Selector de fotos del sistema en todas las versiones | En API 24–29 llega por Google Play Services; no se declara `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE` (R-14) |
| Q-04 y Q-06 | **Abiertas** | Los textos están en `strings.xml` para que el equipo o el profesor los aprueben; no se registra nada en analítica |

## 2. Criterios de éxito

| Criterio | Cómo se cumple | Prueba |
|---|---|---|
| C-01 Nada se pide al arrancar | El mapa llama `requestLocation(false)` al entrar; los permisos solo se piden al tocar un botón | Revisión de `MapScreen` y `ScannerScreen` |
| C-02 Explicación antes del sistema | `rememberPermissionRequest` siempre pasa por `PermissionRationale` | `PermissionRationaleTest` |
| C-03 Sin ubicación | El mapa abre en el centro por defecto; `LocationFallback` | `PermissionMatrixTest` |
| C-04 Sin cámara | Aviso `CameraFallback` sobre la captura manual del código | `PermissionMatrixTest` |
| C-05 Sin fotos | `PhotoFallback` con iniciales y reintento | `PermissionMatrixTest`, `PhotoFallbackInitialsTest` |
| C-06 Sin notificaciones | `NotificationFallback` dentro de la app | `PermissionMatrixTest` |
| C-07 Segunda negativa → ajustes | `PermissionFlow.MAX_SYSTEM_DIALOGS = 2` | `PermissionFlowTest` |
| C-08 Volver de ajustes | `LifecycleResumeEffect` relee y activa sin reiniciar | `PermissionFlowTest` (lógica); falta prueba manual en dispositivo |
| C-09 Rotación | El estado del flujo se guarda con `rememberSaveable` | `PermissionRationaleTest` (`StateRestorationTester`) |
| C-10 Sin literales en `ui/` | `grep -rn 'Manifest.permission' app/src/main/java/mx/petmate/app/ui` no encuentra nada | `grep` |

## 3. Manifiesto (R-14)

| Permiso | Origen | Lo usa |
|---|---|---|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | `AndroidManifest.xml` | Mapa, afiliación, mi negocio |
| `CAMERA` | Lo agrega `zxing-android-embedded` al fusionar manifiestos | Escáner de QR |
| `POST_NOTIFICATIONS` | **No se declara todavía** | Se agrega con 006, cuando exista la pantalla que active notificaciones |

## 4. Lo que falta

- Correr `./gradlew test` y `./gradlew connectedAndroidTest` en un dispositivo.
- Prueba manual del regreso desde ajustes (C-08) y de la negativa permanente en API 30+.
- Aprobación de los textos (Q-04) y decisión de analítica (Q-06).
