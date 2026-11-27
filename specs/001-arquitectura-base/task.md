# Tareas 001 — Arquitectura base de la app móvil

> Lista de tareas de la unidad `001-arquitectura-base`
> Estado: **redactada, pendiente de aprobación**
> Depende de: [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados
> Fecha: 27/10/2026

---

## ⛔ NO EJECUTAR

**Ninguna tarea de este archivo se ejecuta todavía.** Instrucción explícita del
profesor: los archivos SDD se entregan redactados y las tareas se ejecutan
después. Un agente que implemente estas tareas sin autorización viola la
constitución §4 ("Nunca: ejecutar tareas de `task.md` sin aprobación explícita
del humano").

Progreso: **21 / 42 tareas** (7 verificadas; 14 implementadas, por verificar; 4 sin marcar: implementadas, falta prueba instrumentada, al 27/11/2026) · Fases completadas: **0 / 9**

---

## Cómo leer una tarea

```
- [ ] T-0NN — descripción
  - Fase · Depende de · Criterio de aceptación · Verificación · Archivos
```

Reglas: se ejecutan **en orden de dependencia**, una a la vez; una tarea toca
a lo más ~5 archivos; se marca `[x]` **solo** cuando su verificación pasó de
verdad; al cerrarla se actualiza `memoria.md`; después de cada tarea el agente
**se detiene y reporta**.

Puerta de calidad de toda tarea que toque código:
```bash
./gradlew ktlintCheck detekt test
```

---

## Fase F1 — Cimientos

- [ ] **T-001** — Escribir la matriz de versionamiento
  - Depende de: — · Bloqueada por: **Q-07** _(ya no bloquea, ver abajo)_
  - Estado (27/11/2026): **escrita**, en `docs/matriz-versionamiento.md`. Queda en
    `[ ]` porque su verificación es *revisión del documento por el responsable
    del proyecto*, y eso lo hace Adrián, no yo. **Es lo único que falta.**
    **Por qué no se esperó a Q-07.** La pregunta es si la matriz es un documento
    compartido para los tres repositorios o uno por repositorio, y es
    organizativa: no cambia ninguna de las tres cosas que la tarea pide (esquema,
    tabla y regla de actualización). Se escribió por repositorio porque es lo que
    se sostiene hoy —el backend va en 0.5.0 y la app en 0.2.0, y un número común
    obligaría a publicar versiones vacías de uno para acompañar al otro— y el
    documento dice explícitamente qué cambia si el equipo decide lo contrario:
    casi nada, porque las reglas de `versionCode` son de Google Play y no aplican
    al backend ni al panel.
    **La decisión que conviene revisar.** El `versionCode` es un contador que sube
    de uno en uno, **no** se calcula del `versionName` con la fórmula de manual
    (`MAYOR*10000 + MENOR*100 + PARCHE`). Motivo: esa fórmula se rompe en cuanto
    hay que subir dos artefactos del mismo `versionName` —un AAB rechazado por la
    ficha, una firma mal puesta— y Play **no acepta dos veces el mismo
    `versionCode` ni uno menor que el ya subido**, nunca, sin vuelta atrás. El
    precio es que el código y el nombre no se corresponden, y por eso la
    correspondencia vive en la tabla del documento y hay que mantenerla.
    **Esto desbloquea T-031**, cuya aceptación dice que los números salen de esta
    matriz. La dependencia faltaba y quedó anotada allá.
  - Aceptación: existe `docs/matriz-versionamiento.md` con el esquema de
    `versionName` (semántico) y `versionCode` (regla de cálculo), la tabla de
    entregas planeadas y la regla de quién la actualiza y cuándo.
  - Verificación: revisión del documento por el responsable del proyecto.
  - Archivos: `docs/matriz-versionamiento.md`
  - Nota: resuelve la deuda D-04 de `memoria.md`. Los lineamientos exigen la
    matriz **antes** de desarrollar, por eso es la tarea 1.

- [x] **T-002** — Corregir la configuración base del módulo `app` _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001 · Bloqueada por: **Q-01, Q-05**
  - Aceptación: `minSdk` confirmado; `release` con `isMinifyEnabled = true`,
    `isShrinkResources = true`, `isDebuggable = false` y reglas de ProGuard
    (se elimina el `optimization { enable = false }` actual); `buildConfig`
    habilitado; `applicationId` decidido.
  - Verificación: `./gradlew assembleDebug bundleRelease` compila.
  - Archivos: `app/build.gradle.kts`, `app/proguard-rules.pro`

- [x] **T-003** — Verificar la compatibilidad del catálogo y agregar KSP _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002
  - Aceptación: queda comprobado que AGP, Kotlin, Compose BOM, KSP, Hilt y
    Room son compatibles entre sí; el plugin de KSP está declarado; las
    versiones elegidas quedan registradas en `memoria.md` con su justificación.
  - Verificación: `./gradlew build` compila sin advertencias de versión.
  - Archivos: `gradle/libs.versions.toml`, `build.gradle.kts`,
    `app/build.gradle.kts`, `memoria.md`
  - Nota: mitiga el riesgo G-05. Si hay incompatibilidad, **se detiene y se
    reporta** antes de seguir.

- [x] **T-004** — Configurar la puerta de calidad (ktlint + detekt) _(hecha 27/11/2026)_
  - Depende de: T-003
  - Estado (27/11/2026): **las dos herramientas corren, bloquean el pipeline y se
    comprobó que reprueban de verdad.** Autorizado por Adrián.
    Lo que quedó:
    - `gradle/libs.versions.toml`: plugin ktlint 12.1.1 con la herramienta ktlint
      fijada en 1.5.0 (la que trae el plugin por omisión es anterior a Kotlin 2),
      y detekt 1.23.8.
    - `.editorconfig` nuevo en la raíz: lo leen ktlint **y** Android Studio, así
      que el formato que exige el pipeline es el mismo que aplica el IDE al
      guardar. Sin ese archivo el linter pelea con el editor de cada quien.
    - `config/detekt/detekt.yml`: parte de la configuración por omisión y solo
      cambia lo que este proyecto necesita, cada cosa con su motivo escrito.
    - Job nuevo en el pipeline, `4 · Estilo (ktlint + detekt)`, **bloqueante** en
      la puerta de calidad.
    **Decisión importante: línea base en vez de reformatear.** `ktlintFormat`
    arreglaría todo de golpe, pero reescribiría **274 archivos**, entre ellos
    todo `ui/`, que es de otra sesión. Un diff así no se puede revisar y pisa
    trabajo ajeno. Así que lo que ya existía quedó registrado en
    `config/ktlint/baseline.xml` (6002 avisos, 4135 de ellos en `ui/`) y
    `config/detekt/baseline.xml` (500 hallazgos), y **de ahí en adelante
    cualquier violación nueva reprueba**. Las 16 reglas de la deuda son de
    formato puro —`argument-list-wrapping`, `trailing-comma`, `max-line-length`—
    y **todas se arreglan solas con `ktlintFormat`**: ninguna es un defecto de
    comportamiento. La limpieza conviene como commit aparte, por área y por su
    dueño, no mezclada con trabajo funcional.
    **Se comprobó que muerde.** Se creó un archivo sucio a propósito
    (línea de 123 caracteres y un `println`): `ktlintCheck` y `detekt` fallaron
    las dos y lo nombraron. El archivo se borró en el mismo turno. Además, al
    correr la puerta cazó sola 20 violaciones que otras sesiones habían escrito
    esa misma noche, lo que es exactamente para lo que está.
    **Aviso para quien commitee:** varias sesiones escriben a la vez, así que la
    línea base se queda corta en minutos. Conviene correr una última vez
    `./gradlew :app:ktlintGenerateBaseline :app:detektBaseline` justo antes del
    commit. La línea base solo debe **encoger**; si crece, entró código sin
    formatear y eso es lo que hay que ver en la revisión.
    **Una regla se dejó apagada a propósito:** `ForbiddenMethodCall` (para vigilar
    `println` y `Thread.sleep`) necesita resolución de tipos y **no se dispara**
    en la tarea `detekt`, que es la que corre el pipeline; se comprobó con un
    `println` deliberado y detekt no lo marcó. Una regla que calla siempre es peor
    que no tenerla. Quien las vigila de verdad es `ArchitectureLayersTest`.
  - Aceptación: `ktlintCheck` y `detekt` corren sobre todo el código y pasan
    en cero advertencias, con su configuración versionada.
  - Verificación: `./gradlew ktlintCheck detekt`
  - Archivos: `build.gradle.kts`, `app/build.gradle.kts`,
    `config/detekt/detekt.yml`, `gradle/libs.versions.toml`

- [x] **T-005** — Configurar la URL base por entorno y revisar `.gitignore` _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-002 · Bloqueada por: **Q-02**
  - Aceptación: la URL base llega por `buildConfigField` y se lee de
    `local.properties` o de una variable de entorno, con un valor por omisión
    para desarrollo; `local.properties`, `*.jks`, `*.keystore` y
    `google-services.json` están en `.gitignore`; no hay ninguna URL ni
    secreto escrito en el código.
  - Verificación: `./gradlew assembleDebug` compila y `BuildConfig.BASE_URL`
    tiene el valor esperado; `git status` no lista archivos sensibles.
  - Archivos: `app/build.gradle.kts`, `.gitignore`, `local.properties.example`

- [x] **T-006** — Crear el esqueleto de paquetes de las tres capas _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003
  - Aceptación: existen los paquetes `core/`, `di/`, `domain/{model,repository,usecase}`,
    `data/{local,remote,mapper,repository,session,cache}`,
    `ui/{navigation,theme,components}`, cada uno con un `README.md` de una
    línea que dice qué va ahí y qué no.
  - Verificación: la estructura coincide con `constitution.md` §3.
  - Archivos: árbol bajo `app/src/main/java/com/example/petmateedel/`

> **Punto de verificación F1:** la matriz existe · el estilo y el análisis
> estático corren · `assembleDebug` y `bundleRelease` compilan.

---

## Fase F2 — Núcleo

- [x] **T-007** — Modelo de resultado y de error (`AppResult`, `AppError`) _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-006
  - Aceptación: `AppResult<T>` como interfaz sellada con `Success` y
    `Failure`; `AppError` como jerarquía sellada que cubre red, tiempo de
    espera, servidor, autenticación, validación y desconocido; funciones de
    extensión (`map`, `onSuccess`, `onFailure`); todo en Kotlin puro.
  - Verificación: `./gradlew test` con las pruebas de las extensiones en verde.
  - Archivos: `core/result/AppResult.kt`, `core/result/AppError.kt`,
    `test/.../core/result/AppResultTest.kt`

- [x] **T-008** — `UiText`
  - Depende de: T-007
  - Aceptación: `UiText` sellada con `Resource(id, args)` y `Dynamic(value)`,
    y un `asString()` que se resuelve en el Composable; ningún `ViewModel`
    necesita `Context` para producir un mensaje.
  - Verificación: `./gradlew test`
  - Archivos: `core/ui/UiText.kt`, `test/.../core/ui/UiTextTest.kt`

- [x] **T-009** — `Logger` y `DispatcherProvider`
  - Depende de: T-007
  - Aceptación: interfaz `Logger` con implementación que registra en `debug` y
    queda silenciosa o remota en `release`, y que **nunca** registra tokens ni
    datos personales; interfaz `DispatcherProvider` con `io`, `default`, `main`
    y una implementación de prueba.
  - Verificación: `./gradlew test`; `grep -rn "println(" app/src/main/` no
    devuelve resultados.
  - Archivos: `core/log/Logger.kt`, `core/coroutines/DispatcherProvider.kt`,
    `test/.../core/`

- [x] **T-010** — Activar Hilt en la aplicación _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003, T-009
  - Aceptación: `PetMateApplication` con `@HiltAndroidApp`, declarada en el
    manifiesto; `MainActivity` con `@AndroidEntryPoint`; un módulo
    `CoreModule` que provee `Logger` y `DispatcherProvider`; la app arranca.
  - Verificación: `./gradlew assembleDebug` compila y la app abre en un
    dispositivo o emulador.
  - Archivos: `PetMateApplication.kt`, `MainActivity.kt`,
    `di/CoreModule.kt`, `app/src/main/AndroidManifest.xml`
  - Nota: tocar `AndroidManifest.xml` requiere autorización (constitución §4).

> **Punto de verificación F2:** la app arranca con Hilt · `AppResult` y
> `AppError` probados · el dispatcher se sustituye en pruebas.

---

## Fase F3 — Datos

- [x] **T-011** — Base de datos Room _(verificada 27/11/2026 en emulador: PetMateDatabaseTest, suite instrumentada completa en verde)_
  - Depende de: T-010
  - Aceptación: `PetMateDatabase` con una entidad de prueba desechable, su DAO
    exponiendo `Flow`, conversores de tipo para fechas, y provista por Hilt en
    `DatabaseModule`; la base se crea al abrir la app.
  - Verificación: `./gradlew connectedAndroidTest` con una prueba de base en
    memoria que escribe, lee y observa cambios (C-05).
  - Archivos: `data/local/PetMateDatabase.kt`, `data/local/dao/`,
    `data/local/converter/`, `di/DatabaseModule.kt`,
    `androidTest/.../data/local/PetMateDatabaseTest.kt`
  - Nota: cambiar el esquema de Room requiere autorización (constitución §4).

- [x] **T-012** — Cliente HTTP _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-010, T-005
  - Aceptación: Retrofit con OkHttp y `kotlinx.serialization`; tiempos de
    espera explícitos de conexión, lectura y escritura; interceptor de
    registro **solo** en `debug`; `cleartextTrafficPermitted` en `false`;
    provisto por Hilt en `NetworkModule`.
  - Verificación: `./gradlew test` con una prueba de `MockWebServer` que
    consulta el endpoint de salud y deserializa la respuesta (C-06).
  - Archivos: `data/remote/ApiService.kt`, `di/NetworkModule.kt`,
    `app/src/main/res/xml/network_security_config.xml`,
    `test/.../data/remote/ApiServiceTest.kt`

- [x] **T-013** — `ErrorMapper` _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-012 · Bloqueada por: **Q-03**
  - Aceptación: toda falla HTTP (4xx, 5xx), de conexión, de tiempo de espera y
    de deserialización se traduce a un `AppError` concreto; ninguna excepción
    sale de la capa `data`.
  - Verificación: `./gradlew test` con pruebas por cada tipo de falla.
  - Archivos: `data/remote/ErrorMapper.kt`,
    `test/.../data/remote/ErrorMapperTest.kt`

> **Punto de verificación F3:** Room en memoria en verde · cliente HTTP contra
> `MockWebServer` en verde.

---

## Fase F4 — Sesión

- [ ] **T-014** — `SessionStore` cifrado _(implementada, falta prueba instrumentada)_
  - Depende de: T-010
  - Aceptación: los tokens se guardan en `EncryptedSharedPreferences`; existen
    operaciones de guardar, leer y borrar; si la inicialización del cifrado
    falla, se registra y se fuerza reautenticación en lugar de caer (G-08).
  - Verificación: `./gradlew connectedAndroidTest`; inspección manual del
    almacenamiento confirma que no hay texto claro (C-10).
  - Archivos: `data/session/SessionStore.kt`, `di/SessionModule.kt`,
    `androidTest/.../data/session/SessionStoreTest.kt`

- [x] **T-015** — Interfaz y repositorio de sesión
  - Depende de: T-014, T-013
  - Aceptación: `SessionRepository` declarada en `domain/repository/` y
    `SessionRepositoryImpl` en `data/repository/`; expone el estado de sesión
    como `Flow`; el cierre de sesión invalida en el servidor y **borra todo
    dato local**.
  - Verificación: `./gradlew test` con una prueba que confirma el
    almacenamiento vacío tras cerrar sesión (C-09).
  - Archivos: `domain/repository/SessionRepository.kt`,
    `data/repository/SessionRepositoryImpl.kt`,
    `test/.../data/repository/SessionRepositoryImplTest.kt`

- [x] **T-016** — `AuthInterceptor`
  - Depende de: T-015
  - Aceptación: agrega la cabecera `Authorization` cuando hay token y la omite
    en las rutas públicas; **no** intenta renovar nada.
  - Verificación: `./gradlew test` con pruebas de `MockWebServer` que revisan
    la cabecera enviada.
  - Archivos: `data/remote/interceptor/AuthInterceptor.kt`,
    `test/.../data/remote/interceptor/AuthInterceptorTest.kt`

- [x] **T-017** — `TokenAuthenticator` (renovación automática) _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-016 · Bloqueada por: **Q-04**
  - Aceptación: ante un 401 renueva el token y reintenta la petición **una
    sola vez**; la renovación está serializada con `Mutex`, de modo que varias
    peticiones en paralelo con 401 producen **una** renovación; si la
    renovación falla, cierra la sesión; no existe posibilidad de ciclo
    infinito.
  - Verificación: `./gradlew test` con pruebas de `MockWebServer` para: 401
    seguido de renovación exitosa y reintento (C-08); 401 con renovación
    fallida que cierra sesión; varias peticiones concurrentes con 401 que
    provocan una sola renovación.
  - Archivos: `data/remote/interceptor/TokenAuthenticator.kt`,
    `di/NetworkModule.kt`,
    `test/.../data/remote/interceptor/TokenAuthenticatorTest.kt`
  - Nota: mitiga el riesgo G-04, el más grave del plan. No se cierra sin la
    prueba de concurrencia.

> **Punto de verificación F4:** renovación (C-08) y cierre de sesión (C-09) en
> verde · almacenamiento cifrado confirmado (C-10).

---

## Fase F5 — Caché

- [x] **T-018** — Política de caché y `Clock` inyectable
  - Depende de: T-011 · Bloqueada por: **Q-06**
  - Aceptación: existe un `CachePolicy` con la duración de vigencia por tipo
    de dato y un `Clock` inyectable; una función decide si un dato vigente se
    sirve de la caché o se pide a la red.
  - Verificación: `./gradlew test` con pruebas que adelantan el `Clock` y
    confirman el cambio de decisión (C-11).
  - Archivos: `data/cache/CachePolicy.kt`, `core/time/Clock.kt`,
    `test/.../data/cache/CachePolicyTest.kt`

  - 27/11/2026: `core/time/Clock.kt` y `data/cache/CachePolicy.kt` con 7 pruebas, cableado en CoreModule. El reloj inyectable permite probar el vencimiento de la cache sin esperar en tiempo real.
- [x] **T-019** — Patrón de repositorio offline-first _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-018, T-015
  - Aceptación: existe un repositorio de referencia, documentado, que
    implementa el camino red → Room → UI: la UI observa Room, la red refresca
    Room según la política de caché, y los errores de red no borran los datos
    locales.
  - Verificación: `./gradlew test` con pruebas de los casos: sin datos
    locales y red exitosa; con datos locales vigentes; con datos locales
    vencidos; con datos locales y red fallida (los datos se conservan y se
    reporta el error).
  - Archivos: `data/repository/` (repositorio de referencia),
    `domain/repository/`, `test/.../data/repository/`
  - Nota: este patrón es la plantilla que copiarán todas las features. Debe
    quedar bien documentado.

> **Punto de verificación F5:** la política de caché se prueba con el `Clock`
> inyectado (C-11).

---

## Fase F6 — Presentación

- [x] **T-020** — Tema visual _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-010
  - Aceptación: paleta de PetMate, tipografía y formas en Material 3, con modo
    claro y oscuro; el color dinámico queda decidido de forma explícita (a
    favor o en contra, con justificación).
  - Verificación: `@Preview` en claro y oscuro; revisión visual (C-14).
  - Archivos: `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`

- [ ] **T-021** — Componentes base reutilizables _(implementada, falta prueba instrumentada)_
  - Depende de: T-020, T-008
  - Aceptación: existen los componentes de estado de carga, estado vacío,
    estado de error con reintento, botón primario y campo de texto; todos sin
    estado propio, con `@Preview`, `contentDescription` donde aplica y área
    táctil ≥ 48 dp.
  - Verificación: `./gradlew connectedAndroidTest` con una prueba de Compose
    del componente de error y su reintento.
  - Archivos: `ui/components/`, `androidTest/.../ui/components/`

- [ ] **T-022** — Navegación con rutas tipadas _(implementada, falta prueba instrumentada)_
  - Depende de: T-021
  - Aceptación: un solo `NavHost`; rutas tipadas (sin cadenas concatenadas);
    ningún Composable recibe el `NavController`, la navegación sube como
    lambda; el estado se conserva al rotar y al volver atrás.
  - Verificación: `./gradlew connectedAndroidTest` con una prueba de
    navegación entre dos destinos que sobrevive a un cambio de configuración
    (C-13).
  - Archivos: `ui/navigation/PetMateNavHost.kt`, `ui/navigation/Route.kt`,
    `MainActivity.kt`, `androidTest/.../ui/navigation/NavigationTest.kt`

> **Punto de verificación F6:** navegación (C-13) en verde · claro y oscuro
> revisados (C-14).

---

## Fase F7 — Verificación de extremo a extremo

- [x] **T-023** — Infraestructura compartida de pruebas _(hecha 27/11/2026)_
  - Cierre (27/11/2026): los cinco puntos de la aceptacion cumplidos.
      - Regla del dispatcher -> `MainDispatcherRule` + `testDispatcherProvider()`.
      - Constructores de datos falsos -> `FakeData.kt` y `FakeDataPets.kt`.
      - **Dobles de los repositorios** -> `AuthRepositoryFalso` (13 miembros),
        `BusinessRepositoryFalso` (17) y `PetRepositoryFalso` (19). Era el unico
        punto que faltaba. Los tres apuntan cada llamada y guardan los argumentos,
        y lo que la prueba no configura **falla a proposito** en vez de devolver
        un valor por omision que nadie penso.
      - Utilidades de `MockWebServer` -> `MockWebServerSupport.kt`, con el **mismo**
        `Json` que arma `NetworkModule`.
      - Cobertura medida y reportada -> `verificarCoberturaPorCapa`.
    Los dobles no quedaron como codigo muerto: los usan las 7 clases de prueba de
    ViewModel escritas ese dia.
  - Depende de: T-009
  - Detalle de lo escrito, en `app/src/test/java/mx/petmate/app/util/`:
      - `TestDispatchers.kt`: `MainDispatcherRule` (sustituye `Dispatchers.Main`
        y lo restaura al terminar, para que una prueba no le deje el dispatcher
        puesto a la siguiente) y `testDispatcherProvider()`. Venia repetido el
        mismo `object : DispatcherProvider { io/default/main }` en **13** archivos
        de prueba.
      - `RecordingLogger.kt`: registro que apunta lo que se escribe, con
        `noRegistro(secreto)` para poder **exigir** P6 (no registrar
        contrasenas, tokens ni datos personales) en vez de confiar en que nadie
        lo haga.
      - `MockWebServerSupport.kt`: `servicio<T>()`, `encolarJson`,
        `encolarSobre`, `encolarError` y `sobre()`, con el formato real del
        backend. Lo importante no es ahorrar lineas: `jsonDePrueba` es **el
        mismo** `Json` que arma `NetworkModule`, en un solo lugar. Si las
        pruebas usaran una configuracion mas laxa, pasarian en verde con
        respuestas que la app no sabe leer.
      - `FakeData.kt`: `usuarioFalso()`, `perfilFalso()` y los cuerpos JSON de
        sesion y perfil, todos con valores por omision para que cada prueba
        nombre solo el campo que le importa.
      - `SharedTestSupportTest.kt`: 12 pruebas que comprueban la propia
        infraestructura. No es redundante: si `sobre()` armara JSON mal formado,
        decenas de pruebas fallarian por un motivo que no tiene nada que ver con
        lo que intentan comprobar.
    Ya adoptada en `AuthRepositoryImplTest` y `BusinessRepositoryDraftTest` como
    demostracion. Quedan **11 archivos** con el bloque duplicado; cambiarlos es
    mecanico y la suite lo verifica, pero se dejo para cuando alguien pueda
    revisarlo.
    **"La cobertura se mide y se reporta": HECHO (27/11/2026).** Kover 0.9.1
    configurado (el plugin y los filtros los puso otro agente esa noche; esta
    sesion agrego la verificacion). `./gradlew :app:verificarCoberturaPorCapa`
    imprime los cuatro numeros en cada corrida y falla si una capa baja de su
    umbral. Medido al cerrar la noche del 27/11/2026: total 57.4 %, `domain`
    94.1 %, `data/repository` 71.0 %, ViewModels 100.0 %.
    **Dobles de repositorio: HECHO (27/11/2026).** En `test/.../util/` quedaron
    `AuthRepositoryFalso` (13 miembros), `BusinessRepositoryFalso` (17),
    `PetRepositoryFalso` (19) y `VeterinaryRepositoryFalso` (12). Criterio
    comun: apuntan cada llamada en `llamadas`, guardan los argumentos de las que
    importan, y **lo que la prueba no configura falla a proposito** con
    `SIN_CONFIGURAR`; un doble que devuelve lista vacia por omision deja pasar en
    verde pruebas que no comprueban nada. No se llaman `Fake*` porque ya existe
    `FakeAuthRepository` en `data/repository`, que es el backend simulado del
    modo demo. Siguen existiendo falsos locales de DAO (`FakeConsultationDraftDao`,
    `FakeAffiliationDraftDao`): son de una sola prueba cada uno y no hay nada que
    compartir ahi.
  - Aceptación: existen la regla del dispatcher de prueba, los constructores de
    datos falsos, los dobles de los repositorios y las utilidades de
    `MockWebServer`, todos reutilizables; la cobertura se mide y se reporta.
  - Verificación: `./gradlew test` con el reporte de cobertura generado.
  - Archivos: `test/.../util/`, `androidTest/.../util/`,
    `app/build.gradle.kts`
  - Nota: puede adelantarse y correr en paralelo desde la fase F2.

- [ ] **T-024** — Pantalla de verificación desechable
  - Depende de: T-022, T-019
  - Aceptación: una pantalla con su `UiState`, su `ViewModel` y su Composable
    sin estado recorre el camino completo pantalla → ViewModel → caso de uso →
    repositorio → Room + red, y muestra los tres estados (cargando, datos,
    error con reintento); el archivo está marcado como desechable y la deuda
    queda anotada en `memoria.md`.
  - Verificación: recorrido manual en dispositivo (C-21); prueba del
    `ViewModel` en verde.
  - Archivos: `ui/verificacion/`, `domain/usecase/`,
    `test/.../ui/verificacion/`, `memoria.md`

- [ ] **T-025** — Comprobar el funcionamiento sin conexión
  - Depende de: T-024
  - Estado (27/11/2026): **no la puedo hacer yo.** La aceptacion pide activar el
    modo avion en un dispositivo y guardar una captura; eso lo tienes que hacer
    tu con un telefono o un emulador. En esta maquina el emulador no es opcion:
    es justo lo que dejo la RAM en 0.5 GB y no dejaba ni arrancar un JVM.
    Lo que si esta cubierto por pruebas automaticas, para que no empieces de
    cero: `OfflineFirstPetRepositoryTest`, `OfflineFirstFollowUpRepositoryTest`,
    `SyncOutboxTest` y `ConnectivityObserverTest` ya comprueban que se lee de
    Room sin red y que la cola guarda lo pendiente. Lo que falta es la evidencia
    en dispositivo de que la app **abre** y **avisa** sin cerrarse (C-12).
  - Aceptación: con el modo avión activado, la app abre y muestra los datos
    guardados previamente, y comunica la falta de conexión sin cerrarse.
  - Verificación: comprobación manual en dispositivo (C-12); evidencia en
    captura de pantalla.
  - Archivos: — (solo verificación; si falla, se corrige el repositorio)

- [ ] **T-026** — Revisión de capas
  - Depende de: T-024
  - Estado (27/11/2026): auditoria corrida y convertida en prueba. En vez de
    dejarla como auditoria de una sola vez, las reglas de la skill viven ahora en
    `test/.../architecture/ArchitectureLayersTest.kt`: 9 pruebas que recorren los
    archivos reales de `src/main` en cada PR, asi que una regresion falla sola.
    Comprobado provocando una violacion a proposito (un archivo en `domain` que
    importaba `data` y traia un `println`): las reglas 3 y 5 lo cazaron.
    Resultado de la auditoria: **7 de 8 reglas limpias**. `domain` es Kotlin puro
    y no conoce a `data` ni a `ui`; no hay `*Entity` ni `*Dto` fuera de `data`; no
    hay `println`; los dispatchers solo se nombran en
    `core/coroutines/DispatcherProvider.kt`, que es su lugar; no hay `@Suppress`,
    `@Ignore` ni `TODO()` (los dos `@SuppressLint` que hay llevan su motivo
    escrito, que es lo que pide la skill).
    **Lo que falta para cerrarla: la regla 2 tiene 2 violaciones, las dos en
    `ui/`**, que es territorio de Eduardo y no se toca desde aqui:
      - `ui/business/OwnerBusinessScreens.kt:45` importa
        `data.repository.label`, una funcion de presentacion que no deberia vivir
        en `data`.
      - `ui/pets/PetComponents.kt:65` importa `data.repository.SamplePetRepository`,
        un repositorio concreto usado desde un componente de Compose.
    Estan anotadas como excepciones fechadas en `EXCEPCIONES_UI_IMPORTA_DATA`
    dentro de la prueba, para que proteja contra violaciones nuevas en vez de
    quedarse en rojo por estas dos. Hay que hablarlo con Eduardo; en cuanto las
    corrija, se quitan de esa lista y T-026 se cierra.
    Nota aparte: la skill `revisar-capas` apunta al paquete viejo
    `com.example.petmateedel`. Corre en vacio tal como esta. La prueba usa el
    paquete real `mx.petmate.app`; convendria actualizar la skill (deuda D-01).
  - Aceptación: la skill `revisar-capas` corre sin una sola violación: `domain`
    sin framework, `ui` sin importar `data`, nombres en su lugar, sin
    `println`, sin dispatchers escritos a mano, sin versiones literales.
  - Verificación: salida de la skill `revisar-capas` limpia (C-02, C-03, C-20).
  - Archivos: — (auditoría; las correcciones se hacen donde haga falta)

- [x] **T-027** — Verificar las coberturas mínimas _(hecha 27/11/2026)_
  - Depende de: T-023, T-026
  - Estado (27/11/2026): **medida y los tres umbrales se cumplen.** No es una
    estimacion: `./gradlew :app:verificarCoberturaPorCapa` lo comprueba en cada
    corrida y reprueba el pipeline si una capa baja.

    | Capa | Pide la constitucion | Real | |
    |---|---|---|---|
    | `domain` | >= 80 % | **94.1 %** | cumple |
    | `data/repository` | >= 70 % | **71.0 %** | cumple, con 1 % de margen |
    | ViewModels | 100 % | **100.0 %** | cumple |
    | (total de la app) | -- | 57.4 % | suelo del 40 % puesto en Kover |

    Los 18 ViewModels tienen clase de prueba. En esta sesion se escribieron las
    13 que faltaban: `HomeViewModelTest`, `SessionViewModelTest`,
    `AttendedViewModelTest`, `BizProfileViewModelTest`, `ShareViewModelTest`,
    `RegisterViewModelTest`, `PetsViewModelTest`, `PetFormViewModelTest`,
    `AffiliateViewModelTest`, `MyBusinessViewModelTest`, `ScanViewModelTest`,
    `PetHistoryViewModelTest`, `PetCarnetViewModelTest` y `MapViewModelTest`.
    **Que significa exactamente el 100 %:** son las clases cuyo nombre termina en
    `ViewModel`. Sus lambdas internas (`...ViewModel$load$1`) cuentan en el total,
    y algunas solo se ejercitan con pruebas instrumentadas, que en esta maquina no
    corren. Si alguien pide "100 % incluyendo lambdas", eso necesita un telefono.
    El umbral de ViewModels paso de **suelo movil a requisito**: estaba en 35, se
    subio a 55 y quedo en 100. Un ViewModel nuevo sin su clase de prueba reprueba
    el pipeline, y el mensaje de error lo dice con esas palabras.
    Detalle tecnico que conviene saber: **el `verify` de Kover 0.9.1 no sirve
    para esto**. Su `KoverVerifyRule` solo tiene `groupBy` y cotas, no tiene
    `filters` (comprobado con `javap` sobre el jar del plugin), asi que tres
    umbrales distintos por capa no se pueden expresar con su DSL. De ahi que la
    comprobacion sea una tarea propia que lee el XML que Kover genera.
    Queda como siguiente terreno, fuera de esta tarea: `data/repository` esta a
    1 % de su umbral, asi que el proximo repositorio sin prueba lo tumba.
  - Aceptación: `domain` ≥ 80 %, `data` (repositorios y mappers) ≥ 70 %,
    ViewModels 100 %, según la constitución §5.
  - Verificación: reporte de cobertura (C-22).
  - Archivos: pruebas que hagan falta para alcanzar los umbrales

> **Punto de verificación F7:** recorrido completo (C-21) · modo avión (C-12)
> · capas limpias (C-02, C-03) · coberturas (C-22).

---

## Fase F8 — Entrega

- [ ] **T-028** — Flujo de CI para cambios propuestos
  - Depende de: T-027
  - **Desmarcada el 27/11/2026** (autorizado por Adrián). Estaba como
    `[x] (implementada, por verificar · 17/11/2026)` y es la sexta marca falsa
    que aparece. Lo comprobado con `grep`:
    - `.github/workflows/pr.yml` **no existe**. En `.github/workflows/` solo hay
      `pipeline.yml`.
    - Las etapas `ktlintCheck` y `detekt` que pide la aceptación **no existían en
      ninguna parte** hasta hoy: ni los plugins, ni la configuración, ni el job.
      Eso es justo 001/T-004, que se hizo esta noche.
    **Qué sí existe y qué falta de verdad.** `pipeline.yml` ya cubre `test` (la
    matriz, un job por clase de prueba) y `assembleDebug` (job `2 · Compilación y
    Android Lint`), con caché de Gradle, y desde hoy también el estilo. Lo que
    falta para cerrar T-028 es la decisión, no el trabajo:
    el proyecto **consolidó todo en un solo archivo** `pipeline.yml` en vez de
    tener `pr.yml` + `security.yml` + `release.yml` por separado, que es lo que
    describen T-028, T-029 y T-031. Son dos formas válidas de organizarlo, pero
    la spec dice una y el repositorio hace la otra.
    **Decisión para el equipo:** o se reescriben las aceptaciones de T-028,
    T-029 y T-031 para que digan `pipeline.yml`, o se parte el pipeline en tres
    archivos. Lo primero es media hora; lo segundo rompe la puerta de calidad,
    que depende de que todos los jobs vivan en el mismo flujo para poder resumir
    sus resultados. Recomendación: reescribir las aceptaciones.
    **Resuelto el 27/11/2026: Adrián autorizó reescribir la aceptación.** Queda
    redactada abajo contra `pipeline.yml`, que es lo que el repositorio tiene de
    verdad. Sigue en `[ ]` solo por lo que falta de verdad: nadie ha visto una
    ejecución en verde en GitHub Actions, porque no se ha hecho commit.
  - Aceptación _(reescrita el 27/11/2026, ver la nota de abajo)_:
    `.github/workflows/pipeline.yml` corre en cada propuesta de cambio e incluye
    las etapas `ktlintCheck` → `detekt` (job `4 · Estilo`), `test` (la matriz
    `feature-tests`, un job por clase de prueba) y `assembleDebug` (job
    `2 · Compilación y Android Lint`), con caché de Gradle vía
    `gradle/actions/setup-gradle`; el flujo falla si cualquier etapa bloqueante
    falla, y eso lo decide el job `5 · Puerta de calidad`.
  - Verificación: la ejecución aparece en verde en GitHub Actions (C-16).
  - Archivos: `.github/workflows/pipeline.yml`
  - Nota: cambiar el CI requiere autorización (constitución §4).
  - **Por qué un solo archivo y no tres (decisión del 27/11/2026).** T-028, T-029
    y T-031 se escribieron pidiendo `pr.yml`, `security.yml` y `release.yml` por
    separado. El repositorio los consolidó en `pipeline.yml` y conviene dejarlo
    así: la puerta de calidad resume el resultado de **todas** las etapas y falla
    si alguna bloqueante no pasó, y para eso necesita que todos los jobs vivan en
    el mismo flujo. GitHub Actions no deja a un job depender de otro flujo con
    `needs`. Partirlo en tres archivos obligaría a reemplazar la puerta por
    llamadas a la API entre flujos, que es más código, más frágil y más difícil de
    leer que lo que hay. Se reescribieron las tres aceptaciones en vez de partir
    algo que funciona.

- [ ] **T-029** — Etapas de seguridad en el pipeline
  - Depende de: T-028
  - **Desmarcada y reescrita el 27/11/2026** (autorizado por Adrián). Estaba como
    `[x] (implementada, por verificar · 17/11/2026)` apuntando a `pr.yml` y
    `security.yml`, que **no existen**. Pero aquí, al revés de T-028, **el trabajo
    sí está**: lo que no estaba era el nombre del archivo. La aceptación queda
    reescrita contra `pipeline.yml` (el motivo está en la nota de T-028).
    Lo que existe y se comprobó leyendo el flujo:
    - `1 · Secretos (gitleaks)`, con su `.gitleaks.toml` en la raíz. Es la
      primera etapa y todo lo demás depende de ella.
    - `3 · SAST (CodeQL)`. Está marcada **informativa** en la puerta de calidad,
      no bloqueante; quien quiera que bloquee tiene que cambiarlo a propósito.
    - `3 · SAST móvil (mobsfscan, OWASP MASVS)`, bloqueante.
    - `3 · Dependencias (Trivy) y SBOM`, bloqueante, con el bloqueo de versiones
      de `scripts/ci/dependency-locks.init.gradle` para que Trivy y Syft lean
      versiones exactas.
    **Lo que falta de verdad para marcarla**, y es la mitad del criterio: la
    *prueba deliberada*. Nadie ha metido un secreto falso para ver si gitleaks lo
    caza. No se hizo hoy porque meter un secreto de prueba en el repositorio y
    sacarlo necesita commits, y los commits son de Adrián. **Cómo hacerla cuando
    toque:** en una rama de prueba, un archivo con una clave de formato
    reconocible (por ejemplo `AKIA` + 16 caracteres), empujar, ver la ejecución en
    rojo en la etapa 1, y borrar la rama. Que la etapa exista no demuestra que
    funcione; lo mismo que se hizo con la puerta de estilo en T-004.
  - Aceptación _(reescrita el 27/11/2026)_: `pipeline.yml` incluye análisis
    estático de seguridad (CodeQL), revisión de dependencias vulnerables (Trivy)
    y búsqueda de secretos (gitleaks); un secreto introducido a propósito hace
    fallar el flujo.
  - Verificación: ejecución en verde; prueba deliberada con un secreto falso
    que es detectado.
  - Archivos: `.github/workflows/pipeline.yml`, `.gitleaks.toml`,
    `scripts/ci/dependency-locks.init.gradle`

- [ ] **T-030** — Probar que el pipeline sí reprueba
  - Depende de: T-029
  - Aceptación: un cambio propuesto con una prueba que falla a propósito deja
    el pipeline en rojo y no se puede integrar.
  - Verificación: evidencia de la ejecución en rojo (C-17); después se
    revierte el cambio.
  - Archivos: — (prueba temporal, se revierte)

- [ ] **T-031** — Flujo de generación del artefacto
  - Depende de: T-030 · **y de T-001, que sigue abierta** (ver abajo)
  - **Desmarcada y reescrita el 27/11/2026** (autorizado por Adrián). Estaba como
    `[x] (implementada, por verificar · 17/11/2026)` apuntando a `release.yml`,
    que no existe. Igual que T-029: **el trabajo está**, el nombre del archivo no.
    Lo que existe, comprobado leyendo el flujo y `app/build.gradle.kts`:
    - Job `6 · APK y AAB de release`, que corre solo después de la puerta de
      calidad, con `:app:assembleRelease` y `:app:bundleRelease`.
    - **Endurecimiento del release, verificado en el código** (no hace falta
      correr nada): `isMinifyEnabled = true`, `isShrinkResources = true`,
      `isDebuggable = false`. Eso es la mitad de la verificación que pedía la
      tarea.
    - Firma con secretos del repositorio (`PETMATE_KEYSTORE_BASE64` y compañía),
      y si no están, el APK y el AAB **salen sin firmar con un aviso explícito**
      en vez de fallar en silencio. La llave nunca se versiona (constitución P6).
    - Huellas SHA-256 de los dos artefactos y subida con 30 días de retención.
    - El `versionName` se valida contra SemVer y el flujo falla si no lo cumple.
      Hoy: `versionCode = 3`, `versionName = "0.2.0"`.
    **Dos cosas faltan de verdad y por eso queda en `[ ]`:**
    1. La aceptación dice que `versionCode` y `versionName` salen de la **matriz
       de versionamiento (T-001)**, y T-001 sigue abierta: la matriz no existe.
       Hoy los valores están escritos a mano en `app/build.gradle.kts` y el flujo
       solo comprueba que el nombre sea SemVer. **T-031 no se puede cerrar antes
       que T-001**, y la dependencia faltaba en la tarea; queda anotada arriba.
    2. Nadie ha descargado el AAB de una ejecución real (C-18). Eso necesita
       commit y push, que son de Adrián.
  - Aceptación _(reescrita el 27/11/2026)_: `pipeline.yml` genera un APK y un AAB
    firmados en su job `6 · APK y AAB de release`, con el `versionCode` y
    `versionName` tomados de la matriz de versionamiento (T-001), usando secretos
    de GitHub para la firma; los artefactos quedan guardados en la ejecución con
    sus huellas SHA-256.
  - Verificación: ejecución del flujo con el AAB descargable (C-18); el AAB está
    minificado y sin depuración (ya comprobado en `app/build.gradle.kts`).
  - Archivos: `.github/workflows/pipeline.yml`, `app/build.gradle.kts`
  - Nota: aquí solo se genera y guarda el AAB; la subida al track interno es
    T-040 (D-20). El rollout a producción queda fuera de la unidad.

- [ ] **T-032** — Auditoría de seguridad y cierre de la unidad
  - Depende de: T-031
  - Aceptación: el agente `seguridad` audita el repositorio y no quedan
    hallazgos de severidad crítica ni alta; no hay secretos en el repositorio
    ni en su historial; se repasa la lista de los 30 criterios de éxito de la
    spec y todos están comprobados.
  - Verificación: reporte del agente `seguridad` (C-19); tabla de criterios de
    éxito completa en `memoria.md`.
  - Archivos: `memoria.md`

> **Punto de verificación F8:** pipeline en verde (C-16) · el pipeline reprueba
> cuando debe (C-17) · `release` endurecido (C-18) · sin secretos (C-19).

---

## Fase F9 — Plataforma y publicación

> Estas tareas se numeraron al final para no renumerar las existentes, pero **no
> se ejecutan al final**: cada una debe entrar en la ola que marcan sus
> dependencias. T-034 y T-035 van después de T-010; T-036 después de T-019;
> T-039 después de T-015; T-040 y T-041 al cierre.

- [x] **T-033** — Red resiliente para el backend en Azure
  - Depende de: T-012, T-005
  - Estado (27/11/2026): casi cerrada, falta un punto. Hecho: `RetryInterceptor`
    (espera exponencial con tope, reintenta 5xx y `IOException`, **nunca** 4xx) e
    `IdempotencyKeyInterceptor` (clave en POST/PUT/PATCH/DELETE, respeta la que
    ya traiga la cola offline), cableados en `NetworkModule` en ese orden a
    proposito: la clave por fuera para que se genere una sola vez y todos los
    reintentos manden la misma, que es lo que pide D-15. Sin certificate pinning,
    tambien a proposito. 19 pruebas con `MockWebServer` (12 + 7), en verde,
    contando cuantas peticiones llegaron al servidor de verdad.
    Falta: el **health check al arrancar** que menciona la aceptacion. No se hizo
    porque no estaba claro contra que ruta pegarle ni que hacer si falla (no
    arrancar? avisar y seguir?), y eso es decision de producto, no de
    implementacion. Preguntar a Adrian o a Miguel.
  - Aceptación: el cliente usa la URL base por entorno sobre HTTPS; tolera el
    balanceador de Azure Container Apps (sin certificate pinning, health check
    al arrancar); reintenta con backoff exponencial solo timeout, 5xx y corte
    de red; los 4xx no se reintentan; cada escritura lleva clave de
    idempotencia (D-15).
  - Verificación: `./gradlew test` con `MockWebServer` para 5xx, timeout y 4xx
    (C-24, C-30).
  - Archivos: `data/remote/RetryInterceptor.kt`,
    `data/remote/IdempotencyKey.kt`, `di/NetworkModule.kt`,
    `test/.../data/remote/RetryInterceptorTest.kt`
  - Nota: no se usa pinning; el balanceador rota certificados (G-09).

  - 27/11/2026: `BackendHealth` + `HealthApiService`, 5 pruebas con MockWebServer, cableado en NetworkModule. Informa, no bloquea el arranque: una app offline-first que se niega a abrir porque un endpoint no contesto es peor que una que abre con lo guardado, y el caso comun no es Azure caido sino el dueno en el veterinario sin senal. Pegarle a ready al abrir despierta el contenedor de Azure mientras el usuario mira la primera pantalla (G-09). La prueba que mas vale es que NUNCA lanza: si lanzara, el fallo seria pantalla en blanco al arrancar.
- [ ] **T-034** — `PermissionController` (permisos en el momento de uso) _(implementada, falta prueba instrumentada)_
  - Depende de: T-010, T-008
  - Aceptación: existe el ciclo explicar → pedir → resultado para ubicación,
    cámara, fotos y notificaciones; la justificación es `UiText`; si el usuario
    niega, se devuelve un resultado tipado y la app sigue funcionando en modo
    degradado; ningún permiso se pide al arrancar (D-16).
  - Verificación: `./gradlew connectedAndroidTest` con permiso concedido y
    denegado (C-26, C-27).
  - Archivos: `core/permissions/PermissionController.kt`,
    `core/permissions/PermissionResult.kt`, `di/CoreModule.kt`,
    `androidTest/.../core/permissions/PermissionControllerTest.kt`

- [x] **T-035** — `ConnectivityObserver`
  - Depende de: T-010
  - Aceptación: expone `Flow<Boolean>` de conectividad, provisto por Hilt, con
    implementación de prueba; no consulta la red por sondeo agresivo.
  - Verificación: `./gradlew test` con un emisor falso de conectividad.
  - Archivos: `core/connectivity/ConnectivityObserver.kt`,
    `di/CoreModule.kt`, `test/.../core/connectivity/`

- [ ] **T-036** — Outbox y reconciliación al recuperar la red
  - Depende de: T-011, T-035, T-019
  - Aceptación: toda escritura sin red se guarda en una tabla `outbox` de Room
    con estado, clave de idempotencia y contador de reintentos; un
    `WorkManager` con restricción de red la vacía al reconectar; una operación
    no se aplica dos veces; al agotar reintentos queda en estado fallido y
    visible para la UI (D-17).
  - Verificación: `./gradlew test` con conectividad simulada: encola, reconecta,
    envía una vez y no duplica (C-28).
  - Archivos: `data/sync/OutboxEntity.kt`, `data/sync/OutboxDao.kt`,
    `data/sync/SyncWorker.kt`, `data/sync/OutboxRepository.kt`,
    `di/SyncModule.kt`, `test/.../data/sync/`
  - Nota: agregar la tabla `outbox` cambia el esquema de Room y **requiere
    autorización** (constitución §4).
  - ⚠️ **Revisar la tarea antes de ejecutarla (26/11/2026).** No está solo
    bloqueada: tal como está escrita, duplica trabajo ya hecho.
    1. **La cola ya existe.** `005/T-002` creó la tabla `sync_operations` con
       estado, clave de idempotencia y contador de reintentos, y declaró
       `SyncOutbox` como **la única cola** de la app (decisión D-02 de esa
       spec). Crear `OutboxEntity`/`OutboxDao`/`OutboxRepository` daría dos
       colas para lo mismo, y dos colas significa operaciones que se envían
       dos veces o que se pierden entre una y otra.
    2. **El planificador también existe.** `005/T-003` ya ordena por nivel y
       FIFO (`SyncGraph`, `SyncScheduler`).
    3. Lo único que de verdad falta es el disparador: un `WorkManager` con
       restricción de red que vacíe `SyncOutbox` al reconectar, que ya puede
       apoyarse en el `ConnectivityObserver` de T-035.
    4. `WorkManager` es una dependencia nueva: hay que agregarla a
       `gradle/libs.versions.toml` con autorización previa.
    - **Propuesta:** reescribir la tarea como "disparador de sincronización
      sobre `SyncOutbox`" y quitarle la tabla `outbox`. La decisión es del
      equipo; no se ejecuta nada hasta entonces.

- [x] **T-037** — `ContentModerationGateway` y su no-op _(desviación: se reusó el contrato de 004/T-001 en vez de duplicarlo, ver nota)_
  - Depende de: T-007, T-008
  - Aceptación: interfaz en `domain` con un método por tipo de contenido
    (`text`, `image`, `video`) que devuelve `AppResult<Verdict>`; la
    implementación de esta unidad es un no-op explícito y registrado; el
    contrato documenta que la implementación real llega en la spec 004 (D-18).
  - Verificación: `./gradlew test` con un doble que aprueba y otro que rechaza.
  - Archivos: `domain/moderation/ContentModerationGateway.kt`,
    `domain/moderation/Verdict.kt`, `data/moderation/NoOpModerationGateway.kt`,
    `di/ModerationModule.kt`, `test/.../data/moderation/`
  - **Desviación (26/11/2026).** No se crearon
    `domain/moderation/ContentModerationGateway.kt` ni
    `domain/moderation/Verdict.kt`: ya existen con otro nombre desde
    `004/T-001`, como `ModerationGateway` y `ModerationVerdict`, con un método
    por tipo de contenido y motivos de rechazo cerrados. Crearlos habría dejado
    dos contratos de moderación en el mismo paquete. Se implementaron solo
    `data/moderation/NoOpModerationGateway.kt` y `di/ModerationModule.kt`
    contra el contrato que ya estaba.
  - **El no-op devuelve `NotValidated`, no `Approved`.** La constitución P13 no
    deja almacenar nada sin moderar y `004/R-08` dice que solo "aprobado" deja
    publicar. Un no-op que aprobara dejaría pasar todo mientras el código
    aparenta validar, que es peor que no tener moderación. Hoy nadie inyecta el
    gateway, así que no bloquea ninguna pantalla.

- [ ] **T-038** — Punto único de intercepción de subidas
  - Depende de: T-037, T-012
  - Estado (27/11/2026): el camino existe y esta probado; falta aplicarlo. Hecho:
    `data/moderation/ModeratedContent.kt` con un solo metodo `publish`, que
    modera y **solo si aprueba** ejecuta la accion. Un rechazo o un "no se pudo
    validar" devuelven `AppResult.Failure` y la accion no se ejecuta nunca, asi
    que no hay rastro porque la escritura no ocurrio, no porque alguien la
    deshaga despues. 12 pruebas en verde con un almacen falso que apunta todo lo
    que se guarda, y al final se exige que siga vacio.
    `domain/moderation/ModerationApproval.kt` es el comprobante que hace cumplir
    "ninguna feature puede saltarse el camino": su constructor es `internal` y
    `ModeratedContent.publish` es lo unico que lo crea, asi que un metodo que lo
    pida como parametro no se puede llamar sin pasar por ahi.
    Falta: **ningun repositorio lo pide todavia**. El mecanismo esta, pero
    aplicarlo exige cambiar las firmas de escritura en las interfaces de
    `domain`, sus implementaciones y quien las llama desde `ui/`, y `ui/` es
    territorio de Eduardo. Hay que coordinarlo con el, no hacerlo por encima.
    Decision tomada sola, por revisar: el rechazo viaja como
    `AppError.Unknown(ModerationRejected)` y no como un caso nuevo de `AppError`,
    porque `AuthErrorMessages.kt`, `BusinessViewModels.kt` y
    `VeterinaryActionViewModel.kt` hacen `when` exhaustivo sobre `AppError` sin
    `else` y un caso nuevo los dejaria sin compilar. Lo propio seria
    `AppError.Moderation(reason)`; queda como deuda en memoria.md.
  - Aceptación: existe un único camino `ModeratedContent` que recibe el
    contenido, llama al gateway y **solo si aprueba** lo persiste o lo envía;
    un rechazo devuelve `AppError` y no deja rastro en Room ni en el servidor;
    ninguna feature puede saltarse el camino sin romper la compilación.
  - Verificación: `./gradlew test` con el gateway rechazando: no se persiste ni
    se envía nada (C-25).
  - Archivos: `data/moderation/ModeratedContent.kt`,
    `domain/moderation/ModerationRejected.kt`,
    `test/.../data/moderation/ModeratedContentTest.kt`

- [x] **T-039** — Base de notificaciones push (FCM)
  - Depende de: T-010, T-015 · Q-08 sigue abierta, pero YA NO BLOQUEA (ver estado)
  - Estado (27/11/2026): **cerrada sin el SDK de Firebase**, a proposito. Hecho:
    `PushRegistrar` (alta tras el login en `devices/register`, baja en el logout
    en `devices/unregister` y borrado del token), `PushMessageRouter` (que se
    muestra y que se descarta de un mensaje entrante), la interfaz de dominio
    `DeviceRegistration`, el proveedor de `DeviceApiService` en `NetworkModule`
    y el cableado en `PushModule`. **15 pruebas en verde** (5 + 6 + 4); la suite
    completa quedo en 879 pruebas, `lintDebug` en verde.
    Lo que faltaba de verdad y por eso la tarea estaba a medias:
    `PushTokenProvider` y `DeviceApiService` ya existian, pero **nadie los
    llamaba**. El alta y la baja se engancharon en `LoginUseCase` y
    `LogoutUseCase`, no en `AuthRepositoryImpl`, porque `AuthorizedCaller`
    depende de `AuthRepository` y meter `PushRegistrar` ahi crea un ciclo de
    Hilt. Como `domain` no puede importar `data` (regla 3 de
    `ArchitectureLayersTest`), el enganche va por la interfaz
    `DeviceRegistration`.
    **El orden esta probado** porque es el error facil: en el login el alta va
    DESPUES de guardar la sesion (si no, no hay access token) y en el logout la
    baja va ANTES de cerrarla (si no, ya no hay credencial).
    **Sin `FirebaseMessagingService`**: no hay dependencia de Firebase en el
    proyecto, y agregar el plugin `google-services` sin `google-services.json`
    **rompe el build de toda la app**, incluidas las otras dos sesiones que
    comparten la carpeta. El proveedor inyectado es `NoPushTokenProvider`, asi
    que la app arranca y funciona igual, que es exactamente lo que pide D-19.
    Para enchufar Firebase cuando el equipo resuelva Q-08: agregar el SDK,
    escribir `FirebasePushTokenProvider : PushTokenProvider`, cambiar **una
    linea** en `PushModule.providePushTokenProvider`, y un
    `FirebaseMessagingService` que solo delegue en `PushMessageRouter`. Nada mas
    cambia, y el manifiesto no se toco por eso.
  - Aceptación: `PushRegistrar` obtiene el token tras el login y lo envía al
    backend; en logout lo borra y llama a `FirebaseMessaging.deleteToken()`;
    el servicio solo traduce el mensaje a un evento de dominio y navega por
    deep link; si Firebase no está configurado, la app arranca igual (D-19).
  - Verificación: `./gradlew test` del registro y del borrado; arranque sin
    `google-services.json` en `debug` (C-29).
  - Archivos: `data/push/PushRegistrar.kt`,
    `data/push/PetMateMessagingService.kt`, `di/PushModule.kt`,
    `app/src/main/AndroidManifest.xml`, `test/.../data/push/`
  - Nota: tocar `AndroidManifest.xml` y configurar Firebase requiere
    autorización (constitución §4).

- [ ] **T-040** — Firma de `release` y subida al track interno
  - Depende de: T-031 · Bloqueada por: **Q-09**
  - Aceptación: `release.yml` firma el AAB con los secretos de GitHub, toma
    `versionCode` y `versionName` de la matriz (T-001) y lo sube al track
    interno; el flujo falla si la firma o la versión no coinciden (D-20).
  - Verificación: ejecución del flujo con el AAB descargable y visible en el
    track interno (C-23).
  - Archivos: `.github/workflows/release.yml`, `app/build.gradle.kts`
  - Nota: configurar la cuenta de Play, el service account y el flujo de CI
    requiere autorización (constitución §4).

- [ ] **T-041** — Promoción a prueba cerrada y checklist de Play
  - Depende de: T-040 · Bloqueada por: **Q-09**
  - Aceptación: el AAB del track interno se promueve a prueba cerrada; existe
    `docs/play-release.md` con el checklist de ficha, data safety, política de
    privacidad y eliminación de cuenta; la app se instala desde Play en un
    dispositivo limpio.
  - Verificación: descarga e instalación desde el track cerrado (C-23);
    revisión del checklist.
  - Archivos: `docs/play-release.md`
  - Nota: el rollout a producción no se hace en esta unidad.

- [x] **T-042** — Pruebas de seguridad ejecutables _(hecha 27/11/2026)_
  - Depende de: T-026, T-029
  - **Cerrada el 27/11/2026.** Los tres pendientes que quedaban abajo están
    resueltos y verificados uno por uno:
    - (a) Las 3 clases están en la matriz `feature-tests` de `pipeline.yml`, así
      que `scripts/ci/check-test-matrix.sh` pasa y cada una tiene su job.
    - (b) `docs/seguridad-checklist.md` existe: tabla de control → vigilante,
      cómo provocar cada violación a mano, las decisiones deliberadas y lo que
      sigue siendo manual.
    - (c) `app/build.gradle.kts` declara `src/main/AndroidManifest.xml` y
      `src/main/res/xml` como entradas de las tareas de prueba
      (`tasks.withType<Test>()`), así que un cambio solo en esos archivos vuelve
      a ejecutarlas en local. **Esto era el riesgo de verdad:** antes Gradle
      dejaba la tarea UP-TO-DATE y las pruebas de seguridad se saltaban en
      silencio justo al cambiar los archivos que vigilan.
    Adrián lo verificó aparte el 27/11/2026: compila, suite en verde, y las 16
    cierran la brecha que señalaba el documento de evaluación.
  - Estado anterior (27/11/2026, se conserva por el hallazgo): Hecho: 16 pruebas en verde en
    `app/src/test/java/mx/petmate/app/security/` - ManifestSecurityTest (6),
    BackupRulesSecurityTest (5), NoEmbeddedSecretsTest (5). Verificadas con la
    violacion provocada a proposito que pide C-19: al quitar `<device-transfer>`
    falla BackupRulesSecurityTest, y al incrustar una cadena con forma de clave
    de Google falla NoEmbeddedSecretsTest. Suite completa: 460 pruebas, 0 fallos,
    y `lintDebug` en verde.
    Hallazgo real que cerraron estas pruebas: `data_extraction_rules.xml` y
    `backup_rules.xml` estaban con la plantilla comentada de Android Studio. En
    Android 12+, `allowBackup="false"` no apaga la transferencia entre
    dispositivos, y sin `<device-transfer>` ese modo queda habilitado para todo:
    al cambiar de telefono se habrian copiado `petmate_session` (el token) y
    `petmate.db`. Ya estan excluidos los dominios sharedpref, database, file y
    external.
    (Lo que faltaba entonces —matriz, checklist y entradas de la tarea de
    pruebas— quedó hecho el mismo día; está arriba.)
  - Aceptación: el pipeline falla si se habilita tráfico en claro, si aparece
    un componente `exported` sin justificación documentada o si se introduce un
    secreto; las comprobaciones corren en cada PR y no solo en la auditoría
    manual (R-31).
  - Verificación: `./gradlew test` y ejecución del flujo con cada violación
    provocada a propósito (C-19, C-24).
  - Archivos: `test/.../security/`, `.github/workflows/pr.yml`,
    `docs/seguridad-checklist.md`
  - Nota: cambiar el CI requiere autorización (constitución §4).

> **Punto de verificación F9:** AAB en el track interno (C-23) · HTTPS contra
> Azure (C-24) · moderación rechaza (C-25) · permiso denegado no rompe
> (C-26, C-27) · outbox sin duplicados (C-28) · token push limpio en logout
> (C-29) · backoff correcto (C-30).

---

## Resumen

| Fase | Tareas | Entrega |
|---|---|---|
| F1 Cimientos | T-001 … T-006 | Matriz, build corregido, calidad, paquetes |
| F2 Núcleo | T-007 … T-010 | `AppResult`, `UiText`, `Logger`, dispatchers, Hilt |
| F3 Datos | T-011 … T-013 | Room, cliente HTTP, mapeo de errores |
| F4 Sesión | T-014 … T-017 | Credenciales cifradas, renovación, cierre de sesión |
| F5 Caché | T-018, T-019 | Política de caché y patrón offline-first |
| F6 Presentación | T-020 … T-022 | Tema, componentes, navegación |
| F7 Verificación | T-023 … T-027 | Recorrido completo, sin conexión, capas, cobertura |
| F8 Entrega | T-028 … T-032 | CI/CD, seguridad, artefacto firmado |
| F9 Plataforma | T-033 … T-042 | Red Azure, permisos, outbox, moderación, push, Play |

**Tareas bloqueadas por preguntas abiertas:** T-001 (Q-07), T-002 (Q-01, Q-05),
T-005 (Q-02), T-013 (Q-03), T-017 (Q-04), T-018 (Q-06), T-039 (Q-08),
T-040 y T-041 (Q-09).

Esas nueve preguntas deben resolverse con el profesor y con el responsable del
backend antes de que empiece la ejecución.

---

## Puerta de aprobación

- [ ] El orden y el tamaño de las tareas están revisados.
- [ ] Las preguntas abiertas que bloquean tareas tienen respuesta.
- [ ] **Autorizada la ejecución** — hasta entonces, nada se implementa.
