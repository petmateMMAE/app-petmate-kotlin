# Traspaso de la sesión de `ui/` — 27/11/2026 (segunda entrega, tras subir la RAM)

Rama: `feature/eduardo-008-011-012`. **Sin commitear, a propósito.**
Territorio de esta sesión: `ui/` únicamente.

La corrida del emulador que esperaba esta nota **ya se hizo y salió completa**.
Lo que sigue es el resultado real, no el plan.

---

## 1. La corrida: 83 pruebas instrumentadas, 0 fallos

Con 19.8 GB la secuencia funcionó sin un solo problema de memoria. Se corrió
cada clase en su propia invocación de `am instrument`, no en lote: así no
arrastran el teclado abierto que causaba los falsos fallos de la tanda
anterior.

| Clase | Resultado |
|---|---|
| `ui/pets/PetFormTest` | **13/13** — nunca había corrido; los 6 fallos viejos quedaron cerrados |
| `ui/pets/PetsScreenTest` | **14/14** — nunca había corrido |
| `ui/business/ScanScreensTest` | 24/24 |
| `ui/map/MapContentTest` | 19/19 |
| `ui/ficha/AllergyBannerTest` | 5/5 |
| `ui/pets/CarnetScreenTest` | 4/4 |
| `ui/pets/HealthOriginTest` | 4/4 |

Unitarias confirmadas en esta sesión: `PetsNavigationTest` 10/10,
`MapViewModelTest` 19/19, `FakeMapViewTest` 26/26.

**No hubo que depurar nada.** Las dos clases que faltaban pasaron a la primera.

---

## 2. Las 11 tareas marcadas

| Spec | Tarea | Evidencia |
|---|---|---|
| 007 | T-007 | `PetsScreenTest` 14/14 + Q-09 resuelta |
| 007 | T-008 | `PetFormTest` 13/13 + Q-02, Q-06, Q-08 resueltas |
| 007 | T-009 | `PetFormTest` 13/13 + Q-04 resuelta |
| 007 | T-010 | `PetsNavigationTest` 10/10 |
| 007 | T-011 | `PetsScreenTest` + `PetFormTest` |
| 008 | T-007 | `CarnetScreenTest` 4/4 |
| 008 | T-011 | `HealthOriginTest` 4/4 |
| 013 | T-008 | `ScanScreensTest` 24/24 |
| 013 | T-010 | `AllergyBannerTest` 5/5 |
| 013 | T-011 | `ScanScreensTest` 24/24 |
| 010 | T-011 | `MapViewModelTest` 19/19, `FakeMapViewTest` 26/26, `MapContentTest` 19/19 |

---

## 3. Lo que la nota anterior tenía mal, y hay que saber

La nota anterior daba por marcables 12 tareas contando 007/T-007, T-008, T-009
y 010/T-008, T-009. **Cinco de esas estaban bloqueadas por preguntas abiertas
en su propio `task.md`**, y la verificación verde no basta para marcarlas: la
spec dice que una pregunta abierta bloquea su tarea y que no se responde
inventando. Se resolvió así:

**007 — desbloqueadas leyendo el backend ya terminado** (mismo camino que Q-01).
Las cinco respuestas quedaron escritas en `specs/007-mis-mascotas/spec.md` con
su evidencia en código:

- **Q-02**: no hay catálogo de razas. La especie sí es lista cerrada de dos
  (`"Perro"`, `"Gato"`); la raza es texto libre opcional de ≤60 caracteres.
- **Q-04**: el borrado **no es lógico, es físico y en cascada**
  (`DeleteMyPetHandler`). El carnet se borra con la mascota y el QR muere con
  ella. **Esto corrige el riesgo G-03 de la spec**, que suponía lo contrario.
- **Q-06**: el campo «esterilizado» **no existe en el contrato** — ni una
  coincidencia en todo `src/` del backend. El formulario no debe ofrecerlo.
- **Q-08**: obligatorios nombre (1–50) y especie; sexo obligatorio pero con
  `Unknown` por omisión; opcionales raza (≤60), fecha de nacimiento (ni futura
  ni de >50 años) y foto (HTTPS, ≤2048).
- **Q-09**: el orden lo fija el servidor (`CreatedAtUtc` desc, `Id` desc), la
  más reciente primero. El cliente **no debe reordenar**, y verificado que no
  lo hace.

`PetRules` del cliente ya copiaba ese contrato al pie (50, 60, 50 años, especie
cerrada), así que no hubo que cambiar código para desbloquear.

**010/T-008 y T-009 — NO se marcaron, y no por el emulador.** Aquí la nota
anterior se equivocaba al mapearlas a `MapContentTest`: la spec nombra
`HomeScreenTest.kt` y `BusinessCarouselTest.kt`, que no existen. Al medir los
criterios uno por uno aparecieron huecos **de alcance, no de pruebas**:

- **T-008**: el «estado de desactualizado» que pide el criterio **no existe**.
  No hay ni `stale` ni `desactualizado` en `ui/map/` ni en el modelo. Hay que
  implementarlo antes de marcar.
- **T-009**: **no hay carrusel.** Se implementó una hoja inferior de un solo
  negocio, y la sincronía «en ambos sentidos» no está probada: la prueba
  verifica un estado fijo donde mapa y tarjeta coinciden, no que mover uno
  mueva el otro. Decidir con el equipo si el criterio baja a «hoja inferior» o
  si hay que escribir el carrusel.

De sus preguntas se cerró la mitad factual, escrita en el `spec.md` de 010:
**Q-03** los tipos de negocio son exactamente tres y cerrados por el backend
(`Veterinary`, `Store`, `Grooming`) — solo falta el icono y color de cada uno,
que es maqueta; **Q-04** `get-nearby` **no trae dirección ni horario completo**
(viven en otra llamada), así que la tarjeta resumida no puede mostrarlos sin
una petición extra.

---

## 4. El «defecto de layout» de 010/T-011 sigue sin existir

Confirmado otra vez: `MapContentTest` pasa 19/19 con `assertIsDisplayed` y
`performClick` sobre «Activar». Ya está escrito en el `task.md` de 010 para que
nadie vuelva a perseguirlo. No hizo falta otro volcado de semántica.

---

## 5. `Pet.photoUrl`: el dato ya llega, la foto todavía no

`Pet.photoUrl` existe en el dominio y el mapper lo llena, y `PetSummary` lleva
el `Pet` entero, así que **el dato llega hasta `ui/`**. Pero la tarjeta sigue
pintando imágenes locales, y el eslabón que falta no es el dato: **no hay
cargador de imágenes remotas**. `coil-compose` **no está en el catálogo** y
007/T-005 exige su autorización registrada (y sigue bloqueada por Q-05).

**No se añadió la dependencia por cuenta propia.** Eso lo decide quien lleve la
spec. En cuanto se autorice es un cambio de una línea en `PetAvatar`. Queda
anotado en `ui/pets/PetComponents.kt` y bajo 007/T-007.

---

## 6. Único cambio de código de esta sesión

Un comentario: el KDoc de `petPhoto()` en `ui/pets/PetComponents.kt` decía «el
backend aún no guarda fotos (pm-079)», que ya es falso. Nada más. Todo lo demás
fue correr pruebas y escribir specs.

**Puerta: VERDE, confirmada.** `:app:testDebugUnitTest` con **89 clases, 879
pruebas, 0 fallos**, y `:app:lintDebug` sin errores.

Ojo con la cifra: la nota anterior decía «827 pruebas». El número real es
**879**. Y hubo que llegar a él de rebote, lo que deja la lección más útil del
día:

### Una corrida «BUILD SUCCESSFUL» puede omitir clases en silencio

El primer intento murió con `Daemon compilation failed: null` (demonio de
Kotlin, colisión conocida de `app/build/`). Al reintentar dio **BUILD
SUCCESSFUL**... pero solo había corrido **61 clases y 627 pruebas**, no 89 y
879. Las 28 que faltaban eran justo las primeras en orden alfabético
(`architecture`, `classifier`, `core`, `data/*`): la compilación a medias había
dejado sus `.class` sin generar, y el task de pruebas simplemente no las
encontró. **No avisó. Salió en verde.** `--rerun` tampoco lo arregló, porque el
problema estaba en las clases compiladas, no en el estado del task.

Se detectó comparando el número de XML en `app/build/test-results/` contra
`find app/src/test -name '*Test.kt'` (89) y los `@Test` del código (879).

**Regla nueva: después de cualquier caída del demonio de Kotlin, no confiar en
el BUILD SUCCESSFUL siguiente. Contar las clases.**

```powershell
$t=0;$c=0
Get-ChildItem "appuild	est-results	estDebugUnitTest" -Filter "*.xml" | ForEach-Object {
  $d=[xml](Get-Content $_.FullName); $t+=[int]$d.testsuite.tests; $c++ }
"clases=$c tests=$t"   # tiene que dar 89 y 879
```

---

## 7. Siguiente trabajo de `ui/`

1. **010/T-008**: implementar el estado de desactualizado. Es el hueco más
   concreto que queda y cierra una tarea.
2. **010/T-009**: decisión de criterio antes de código (carrusel vs. hoja).
3. **013/T-009**: **ojo, ya no está bloqueada.** Q-04 se resolvió el 27/11: el
   código corto es el campo `qrCode` de `pets/get-qr/{id}`, el mismo que recibe
   `scans/resolve`. Falta implementarla.
4. **012-panel-afiliado**: sus 2 libres.
5. Nada de 016, 018, 019 ni 002.

## 8. Avisos de convivencia

Regla que se confirmó otra vez: **nunca compilar con el emulador arriba**, y
`./gradlew --stop` antes de levantarlo. Con eso y 20 GB, la corrida de 83
pruebas no tuvo un solo problema de memoria. Las colisiones de `app/build/`
entre sesiones siguen apareciendo y siguen arreglándose reintentando.
