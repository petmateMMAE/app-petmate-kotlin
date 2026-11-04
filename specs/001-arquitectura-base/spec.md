# Spec 001 — Arquitectura base de la app móvil

> Unidad de trabajo: `001-arquitectura-base`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 27/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:
1. La app se construye en Kotlin nativo con Jetpack Compose (el repositorio
   ya existe así; queda descartado Flutter para este proyecto).
2. El backend expone una API REST en /api/v1 con autenticación JWT
   (token de acceso + refresh), según el repositorio hermano en ASP.NET Core.
3. Esta unidad entrega SOLO el andamio arquitectónico: ninguna pantalla,
   modelo ni regla de negocio de PetMate.
4. El contrato exacto de los endpoints aún no está cerrado, por lo que la
   capa de red se valida contra un endpoint de salud y un doble de prueba,
   no contra endpoints reales de negocio.
5. minSdk queda en 24 hasta que el profesor confirme si los lineamientos
   obligan API 26.
6. El backend se despliega en **Azure Container Apps** detrás de su balanceador
   y se consume por HTTPS; la URL y el health check exactos son Q-02.
7. La primera publicación va a un **track interno/cerrado** de Google Play; el
   rollout a producción pertenece a la spec de liberación.
→ Corríjanme antes de aprobar esta spec.
```

---

## 1. Objetivo

Dejar lista la **base técnica** sobre la que después se construirá toda la
funcionalidad de PetMate: estructura de capas, inyección de dependencias,
persistencia local, acceso a red, navegación, manejo de errores, sesión,
caché de frontend, tema visual, infraestructura de pruebas y pipeline de
CI/CD.

**Por qué ahora:** la metodología del proyecto es Spec Driven Development y
los lineamientos exigen DevSecOps, caché en tres niveles, pruebas en tres
niveles, CI/CD y una matriz de versionamiento definida *antes* de desarrollar.
Si la funcionalidad se construye sobre un andamio improvisado, cada feature
reinventa su forma de manejar errores, estado, caché y pruebas, y nada de eso
se puede corregir después sin reescribir todo.

**Para quién:** para el equipo de desarrollo y para los agentes de IA que
implementarán las features. El usuario final no ve esta unidad de forma
directa; la percibe como una app que abre sin conexión, no se cae y no pierde
la sesión.

**Cómo se ve el éxito:** un agente de IA puede tomar la spec de una feature
cualquiera y construirla sin tener que decidir nada de infraestructura, porque
todas esas decisiones ya están tomadas, implementadas y probadas.

---

## 2. Alcance

| # | Capacidad |
|---|---|
| A-01 | Matriz de versionamiento escrita y aplicada al build |
| A-02 | Estructura de paquetes de las tres capas (`ui`, `domain`, `data`) y sus auxiliares (`core`, `di`) |
| A-03 | Inyección de dependencias en toda la app |
| A-04 | Base de datos local operativa y verificable, como fuente de verdad de la UI |
| A-05 | Cliente de red operativo contra `/api/v1`, con serialización, tiempos de espera, reintentos y registro |
| A-06 | Modelo único de resultado y de error, del repositorio hasta la pantalla |
| A-07 | Manejo de sesión: guardado cifrado de credenciales, renovación automática y cierre de sesión real |
| A-08 | Caché de frontend (memoria + disco) con política de expiración explícita |
| A-09 | Navegación con rutas tipadas y un solo grafo |
| A-10 | Tema visual (colores, tipografía, modo claro/oscuro) y componentes base reutilizables |
| A-11 | Infraestructura de pruebas en los tres niveles, con dobles y utilidades |
| A-12 | Puerta de calidad automática (estilo y análisis estático) |
| A-13 | Pipeline de CI/CD que compila, prueba, revisa seguridad y genera el artefacto firmable |
| A-14 | Configuración de build por entorno y endurecimiento del `release` |
| A-15 | Observabilidad mínima: registro de eventos y errores con un logger propio |
| A-16 | Publicación en Google Play desde CI: AAB firmado, versión de la matriz y subida a track interno/cerrado |
| A-17 | Consumo del backend en Azure por HTTPS, con resiliencia ante el balanceador (timeouts, reintentos, health) |
| A-18 | Mecanismo de permisos en el momento de uso, con explicación previa y degradación si se niegan |
| A-19 | Escrituras offline: cola persistente y reconciliación al recuperar la red |
| A-20 | Punto único de intercepción de contenido (texto, imagen, video) antes de almacenar |
| A-21 | Base de notificaciones push: token, registro tras login y recepción desacoplada |
| A-22 | Pruebas de seguridad ejecutables en el pipeline |

### Pantalla de verificación
Se permite **una sola** pantalla mínima y desechable, sin valor de producto,
cuyo único fin es demostrar que el andamio funciona de extremo a extremo
(pantalla → ViewModel → caso de uso → repositorio → base local + red). Se
elimina al llegar la primera feature real.

---

## 3. Fuera de alcance

Queda **explícitamente excluido** de esta unidad:

| Qué | Dónde se atenderá |
|---|---|
| Mapa, marcadores y filtros de negocios | spec de funcionalidad |
| Mis mascotas, carnet, vacunas, alergias, historial | spec de funcionalidad |
| QR, compartir historial, escáner | spec de funcionalidad |
| Registro, verificación por código, login, recuperación | spec de funcionalidad |
| Afiliación, panel del negocio, consultas, cobro, seguimiento | spec de funcionalidad |
| Chat de seguimiento | spec de funcionalidad |
| Función de ML (raza por foto) | spec de funcionalidad |
| Función de IA generativa | spec de funcionalidad |
| Política de moderación (proveedor, umbrales, mensajes) | spec 004 (aquí solo el punto de intercepción, A-20) |
| Contenido y segmentación de las notificaciones push | spec 006 (aquí solo la base técnica, A-21) |
| Textos de justificación y UX de permisos por pantalla | spec 003 (aquí solo el mecanismo, A-18) |
| Política de sincronización por entidad y conflictos de negocio | spec 005 (aquí solo el outbox genérico, A-19) |
| Aviso de privacidad, enlace a la landing y eliminación de cuenta | spec de ajustes (017) |
| Entidades y tablas de negocio | spec de funcionalidad (aquí solo se prueba el mecanismo de persistencia) |
| Endpoints de negocio | spec de funcionalidad (aquí solo el cliente y el endpoint de salud) |
| Landing page y Google Analytics | otro repositorio |
| Infraestructura de nube y Terraform | otro repositorio |
| Backend | repositorio `backend-petmate-nosdd-aspnetcore` |
| Rollout abierto en producción y ficha de Play | spec de liberación, posterior (aquí solo track interno/cerrado, A-16) |

**Ninguna regla de negocio de PetMate se escribe en esta unidad.**

---

## 4. Requisitos

### Funcionales de la plataforma

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | La app debe abrir y mostrar datos previamente guardados sin conexión a internet | RF-53, RNF-28 |
| R-02 | La información que ve la UI debe provenir de la base local; la red actualiza la base local | RF-53, RNF-28 |
| R-03 | La app debe conservar la sesión del usuario indefinidamente sin reautenticación | RF-57, RNF-31 |
| R-04 | La app debe renovar el token de acceso de forma automática y transparente al expirar | RNF-31 |
| R-05 | El cierre de sesión debe invalidar las credenciales en el servidor y borrar todo dato de sesión del dispositivo | RNF-31 |
| R-06 | La app debe guardar credenciales de sesión de forma cifrada | RNF-01, RNF-02 |
| R-07 | Toda comunicación con el backend debe ir sobre HTTPS; el tráfico en claro debe estar prohibido | RNF-01, RNF-29 |
| R-08 | La app debe cachear respuestas en memoria y en disco, con expiración explícita por tipo de dato | RNF-28 |
| R-09 | La app debe presentar al usuario un mensaje comprensible ante cualquier falla de red, servidor o datos, sin cerrarse | RNF-16 |
| R-10 | La app debe registrar eventos y errores de forma consultable, sin incluir datos sensibles ni credenciales | RNF-32 |
| R-11 | La app debe permitir navegar entre pantallas conservando el estado al rotar y al volver atrás | RNF-16 |
| R-12 | La app debe ofrecer modo claro y oscuro | RNF-16 |
| R-13 | El número de versión de cada compilación debe provenir de la matriz de versionamiento | RNF-34 |
| R-14 | La app debe funcionar en la versión mínima de Android que se confirme en Q-01 (hoy, API 24) | RNF-17 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-15 | El código debe estar organizado en tres capas con dependencias en una sola dirección | RNF-20, constitución P3 |
| R-16 | La capa de dominio no debe depender de ningún framework ni de Android | RNF-20, constitución P3 |
| R-17 | Toda dependencia debe resolverse por inyección, sin construcción manual ni singletons globales | RNF-20 |
| R-18 | Ninguna operación de entrada/salida debe ejecutarse en el hilo principal | RNF-15 |
| R-19 | No debe existir ningún secreto, llave ni credencial en archivos versionados | RNF-01, constitución P6 |
| R-20 | Debe existir infraestructura para pruebas unitarias, de integración y de interfaz | RNF-33 |
| R-21 | El pipeline debe compilar, correr pruebas, revisar estilo, análisis estático y seguridad en cada cambio propuesto | RNF-33 |
| R-22 | El artefacto de `release` debe estar minificado, sin depuración y sin registro verboso | RNF-01, RNF-15 |
| R-23 | Todas las versiones de dependencias deben estar centralizadas en un único catálogo | RNF-20 |
| R-24 | La compilación de `release` debe firmarse con secretos de CI y subirse a un track interno/cerrado de Google Play, con `versionCode` y `versionName` tomados de la matriz | RNF-34, constitución P7 y P11 |
| R-25 | Toda comunicación con el backend en Azure debe ir por HTTPS; el cliente debe tolerar el balanceador (sin pinning rígido, health check) | RNF-01, RNF-29 |
| R-26 | Ante fallos transitorios (timeout, 5xx, corte de red) el cliente debe reintentar con backoff y sin duplicar efectos; los 4xx no se reintentan | RNF-15, RNF-16 |
| R-27 | Ningún contenido aportado por el usuario (texto, imagen, video) puede persistirse ni enviarse sin pasar por el punto de intercepción de moderación | RNF-01, constitución P13 |
| R-28 | Los permisos del sistema se piden en el momento de uso, con explicación previa, y la app debe seguir siendo usable si se niegan | RNF-17, constitución P10 |
| R-29 | Las escrituras hechas sin conexión deben encolarse de forma persistente y reconciliarse al recuperar la red, sin pérdidas ni duplicados | RNF-28, constitución P4 |
| R-30 | El token de push debe registrarse tras el login y eliminarse en el logout; la recepción no debe depender de que la app esté abierta | RNF-32, constitución P9 |
| R-31 | El pipeline debe ejecutar pruebas de seguridad que fallen ante tráfico en claro, componentes exportados sin justificar o secretos | RNF-33, constitución P7 |

---

## 5. Criterios de éxito

Cada criterio es verificable por alguien que no escribió esta spec.

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | Existe un documento de matriz de versionamiento y el `versionName` de la app coincide con él | Inspección del documento y de la salida del build |
| C-02 | Las tres capas existen y sus dependencias van en una sola dirección | La skill `revisar-capas` corre sin violaciones |
| C-03 | Ningún archivo de la capa de dominio importa Android, Room, Retrofit ni Compose | `grep` sobre `domain/` no devuelve resultados |
| C-04 | La app arranca con el contenedor de dependencias activo y ninguna dependencia construida a mano | La app abre; no existen llamadas a constructores de repositorios en `ui` |
| C-05 | La base de datos local se crea, escribe, lee y emite cambios | Prueba instrumentada de la base en memoria, en verde |
| C-06 | El cliente de red consulta el endpoint de salud del backend y deserializa su respuesta | Prueba de integración contra un servidor local de prueba, en verde |
| C-07 | Un error de red se convierte en un mensaje en pantalla y la app no se cierra | Prueba del ViewModel con el doble de red fallando, en verde |
| C-08 | Un token expirado se renueva automáticamente y la petición original se reintenta una sola vez | Prueba del interceptor de renovación, en verde |
| C-09 | El cierre de sesión deja el almacenamiento de credenciales vacío | Prueba del repositorio de sesión, en verde |
| C-10 | Las credenciales no son legibles en texto claro en el almacenamiento del dispositivo | Inspección del almacenamiento en un dispositivo de depuración |
| C-11 | Un dato cacheado se sirve desde la caché antes de su expiración y se renueva después | Prueba de la política de caché, en verde |
| C-12 | La app abre y muestra datos guardados con el modo avión activado | Comprobación manual en dispositivo |
| C-13 | La navegación entre dos destinos conserva el estado al rotar la pantalla | Prueba instrumentada de navegación, en verde |
| C-14 | La app se ve correctamente en modo claro y en modo oscuro | Comprobación manual y capturas |
| C-15 | `./gradlew ktlintCheck detekt test` termina sin errores ni advertencias | Ejecución del comando |
| C-16 | El pipeline de CI corre en un cambio propuesto y todas sus etapas quedan en verde | Ejecución visible en GitHub Actions |
| C-17 | El pipeline falla a propósito si se introduce una prueba que no pasa | Prueba del pipeline con un cambio deliberado |
| C-18 | El `release` compila, está minificado y no tiene depuración activa | `./gradlew bundleRelease` y revisión de la configuración |
| C-19 | No hay secretos en el repositorio ni en su historial | Revisión del agente `seguridad` |
| C-20 | Ninguna versión literal de dependencia aparece fuera del catálogo | `grep` sobre los archivos de build |
| C-21 | La pantalla de verificación recorre pantalla → ViewModel → caso de uso → repositorio → base local y red | Comprobación manual en dispositivo |
| C-22 | Las coberturas mínimas de la constitución §5 se cumplen | Reporte de cobertura |
| C-23 | El pipeline genera un AAB firmado con la versión de la matriz y lo sube al track interno; la app se instala desde Google Play | Ejecución del workflow y descarga desde el track |
| C-24 | La app solo habla con el backend de Azure por HTTPS y rechaza el tráfico en claro | Prueba del cliente e inspección de `network_security_config` |
| C-25 | Con un doble de moderación que rechaza, ningún contenido se persiste ni se envía | Prueba del punto de intercepción, en verde |
| C-26 | Con un permiso denegado, la app explica y sigue funcionando en modo degradado | Prueba instrumentada de permiso denegado |
| C-27 | Un permiso se solicita al usar la función, no al arrancar la app | Prueba instrumentada y revisión del manifiesto |
| C-28 | Una escritura sin conexión se envía al recuperar la red y no se duplica | Prueba del outbox con conectividad simulada |
| C-29 | El token FCM se registra tras el login y se borra en el logout | Prueba del registro y del cierre de sesión |
| C-30 | El cliente reintenta con backoff ante timeout o 5xx y no reintenta 4xx | Prueba con `MockWebServer` |

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato de la API no está cerrado y la capa de red se diseña a ciegas | Alto | Esta unidad solo valida el mecanismo (endpoint de salud + dobles). Los modelos de negocio llegan con sus specs. Se acuerda el contrato con el responsable del backend antes de la primera feature |
| G-02 | Sobre-ingeniería: construir abstracciones que ninguna feature usará | Medio | El alcance está limitado a las 15 capacidades listadas. Toda pieza que no sirva a un criterio de éxito se elimina |
| G-03 | Conflicto entre `minSdk` 24 del andamio y el Android 8+ de los lineamientos | Medio | Pregunta abierta Q-01; se resuelve antes de ejecutar la tarea T-002 |
| G-04 | La renovación automática de token con varias peticiones en paralelo provoca renovaciones duplicadas o un ciclo infinito | Alto | Renovación serializada con un solo reintento por petición; prueba específica (C-08) |
| G-05 | Las versiones muy recientes del catálogo (AGP 9.x, Compose BOM 2026.02) pueden ser incompatibles entre sí o con Hilt | Medio | Verificar la compatibilidad en la tarea T-003 antes de agregar el resto; fijar versiones estables |
| G-06 | El crédito y los límites de la cuenta de Google Play retrasan la verificación del despliegue | Medio | El AAB firmado se valida en local aunque Play tarde; el track interno se usa primero y la promoción queda en T-041 |
| G-07 | Varios integrantes tocan el andamio al mismo tiempo y se pisan | Medio | El andamio lo construye una sola persona; las features entran después, en ramas separadas |
| G-08 | Cifrar credenciales con la API estándar falla en algunos dispositivos y deja la app sin sesión | Medio | Degradación controlada y registrada; prueba en dispositivo físico |
| G-09 | El balanceador de Azure rota certificados y un pinning rígido deja la app sin red | Alto | TLS estándar sin pinning, health check y prueba contra el entorno real (D-15) |
| G-10 | La subida a Play falla por credenciales, track mal configurado o rechazo de revisión | Alto | Track interno primero, service account y checklist de política antes del cierre (T-040, T-041) |
| G-11 | El outbox duplica escrituras al reconectar | Medio | Claves de idempotencia por operación y prueba de reconexión (C-28) |
| G-12 | Firebase mal configurado impide arrancar la app | Medio | `google-services.json` por CI y arranque tolerante si falta push (T-039) |

---

## 7. Dependencias

**Externas a esta unidad:**
- Acuerdo con el responsable del backend sobre la URL base, el endpoint de
  salud, el formato de error y el flujo de renovación de token.
- Repositorio creado en la organización **petmateMMAE** con ramas `main` y
  `develop` protegidas.
- Secretos de CI/CD configurados en GitHub (credenciales de firma).
- Cuenta de Google Play Console, track interno/cerrado y service account de
  publicación.
- Proyecto de Firebase con `google-services.json` para desarrollo y para CI.
- URL base y health check del backend en Azure (Q-02) y certificado del
  balanceador.

**Internas (esta unidad habilita a):**
- Todas las specs de funcionalidad posteriores dependen de que 001 esté
  terminada y verificada.

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿`minSdk` se queda en 24 o sube a 26 por los lineamientos? | Profesor | T-002 |
| Q-02 | ¿Cuál es la URL base del backend en desarrollo y la ruta exacta del endpoint de salud? | Miguel (backend) | T-005 |
| Q-03 | ¿Cuál es el formato de la respuesta de error del backend (campos y códigos)? | Miguel (backend) | T-013 |
| Q-04 | ¿El refresh token rotativo se envía en el cuerpo o en una cabecera, y en qué ruta? | Miguel (backend) | T-017 |
| Q-05 | ¿Se cambia el `applicationId` de `com.example.petmateedel` a uno definitivo antes de publicar? | Equipo | T-002 |
| Q-06 | ¿Qué duración de caché se quiere por tipo de dato? (la arquitectura propone valores por omisión) | Equipo | T-018 |
| Q-07 | ¿La matriz de versionamiento es un documento compartido para los tres repositorios o uno por repositorio? | Profesor / equipo | T-001 |
| Q-08 | ¿Qué proyecto de Firebase se usa y quién administra `google-services.json`? | Equipo | T-039 |
| Q-09 | ¿Qué cuenta de Play Console, track y service account se usan para publicar? | Equipo / profesor | T-040, T-041 |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El profesor / responsable revisó el alcance y el fuera de alcance.
- [ ] Los supuestos están corregidos o confirmados.
- [ ] Las preguntas abiertas tienen dueño.
- [ ] **Aprobada para pasar a `plan.md`.**

Siguiente: [`plan.md`](plan.md) → [`task.md`](task.md)
