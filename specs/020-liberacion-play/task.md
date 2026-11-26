# Tareas 020 — Liberación en Google Play

> Estado: **redactada, pendiente de aprobación**
> Depende de [`spec.md`](spec.md) y [`plan.md`](plan.md) aprobados

---

## ⛔ NO EJECUTAR sin autorización

Ninguna tarea se ejecuta sin autorización explícita del humano
(`constitution.md` §4).

Progreso: **4 / 14 tareas** (4 implementadas, por verificar, al 26/11/2026)

---

## ⏰ ESTA UNIDAD TIENE FECHAS, NO SOLO DEPENDENCIAS

A diferencia de las demás, aquí el orden lo manda el calendario. La fecha
crítica es el **08/11/2026**: inicio de la prueba cerrada. Si se pasa, no se
llega a la evaluación del 02/12/2026 con la app publicada.

**T-001 a T-008 deberían arrancar aunque el resto del producto esté a medias.**

---

## Fase F1 — Cuenta y firma · límite 29/10/2026

- [x] **T-001** — Crear la cuenta de desarrollador y verificar identidad _(implementada, por verificar · 17/11/2026)_
  - Depende de: — · Bloqueada por: **Q-02** (quién paga la cuota)
  - Aceptación: la cuenta existe, se pagó la cuota y la verificación de
    identidad está enviada; se confirman los requisitos vigentes de prueba
    cerrada (Q-01).
  - Verificación: acceso a la consola y estado de verificación visible (C-01).
  - Archivos: `memoria.md`
  - Nota: **primera tarea de todas**. La verificación puede tardar días y
    bloquea todo lo demás (RT-01).

- [ ] **T-002** — Generar y resguardar la llave de subida
  - Depende de: T-001 · Bloqueada por: **Q-06** (quién resguarda)
  - Aceptación: la llave existe; está en los secretos de GitHub; hay una copia
    de seguridad fuera del repositorio con responsable nombrado (D-02); **no
    aparece en el repositorio ni en su historial**.
  - Verificación: búsqueda en el repositorio y en el historial que no
    devuelva ningún `.jks` ni `.keystore` (C-02).
  - Archivos: `.gitignore`, `memoria.md`
  - Nota: perder esta llave es el único error irreversible del proyecto.

- [x] **T-003** — Verificar el nombre y reservar la ficha _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-001 · Bloqueada por: **Q-05** (disponibilidad del nombre)
  - Aceptación: el nombre está disponible y la aplicación está creada en la
    consola con su identificador definitivo, el mismo del `applicationId` de
    001/T-002.
  - Verificación: la app aparece en la consola con el identificador correcto.
  - Archivos: `memoria.md`

> **Verificación F1 (29/10):** cuenta en verificación, llave resguardada,
> nombre reservado.

---

## Fase F2 — Ficha y declaraciones · límite 03/11/2026

- [ ] **T-004** — Ficha de la tienda
  - Depende de: T-003 · Bloqueada por: **Q-07** (alcance geográfico)
  - Aceptación: título, descripción corta y larga, icono, gráfico de
    presentación y al menos dos capturas por tamaño de pantalla requerido;
    categoría y datos de contacto.
  - Verificación: la ficha no muestra elementos pendientes en la consola
    (C-05).
  - Archivos: `docs/ficha-play.md` (copia de los textos para control de
    versiones)

- [ ] **T-005** — Formulario de seguridad de datos
  - Depende de: T-004
  - Aceptación: se recorren las specs **002, 007, 008, 014 y 019** y se
    declara cada dato recolectado, incluidos los **datos de salud** de la
    mascota y el **envío de información al proveedor de IA** de 019 (D-04);
    la declaración es veraz y completa.
  - Verificación: revisión cruzada del formulario contra esas cinco specs
    (C-06).
  - Archivos: `docs/seguridad-datos-play.md`
  - Nota: una declaración incompleta sobre datos de salud puede suspender la
    cuenta, no solo rechazar la app (G-04).

- [ ] **T-006** — Clasificación de contenido y política de privacidad
  - Depende de: T-005 · Bloqueada por: **Q-04** (URL de la landing)
  - Aceptación: cuestionario de clasificación respondido; la URL de la
    política de privacidad está capturada y **la página existe y abre**; la
    misma URL es la que usa la app en 017.
  - Verificación: apertura del enlace desde la ficha y desde la app (C-07).
  - Archivos: `docs/ficha-play.md`

> **Verificación F2 (03/11):** ficha completa; datos declarados; privacidad
> publicada.

---

## Fase F3 — Automatización · límite 05/11/2026

- [ ] **T-007** — Cuenta de servicio para subir desde GitHub
  - Depende de: T-002
  - Aceptación: existe una cuenta de servicio con permiso para publicar en la
    app; sus credenciales están en secretos de GitHub, nunca en el
    repositorio.
  - Verificación: una subida de prueba al track interno termina en verde.
  - Archivos: `.github/workflows/release.yml`

- [ ] **T-008** — Flujo de subida automática
  - Depende de: T-007
  - Aceptación: `release.yml` construye el artefacto firmado, toma el
    `versionCode` de la matriz de versionamiento (001/T-001) y lo sube al
    track configurado; cada subida queda registrada en la bitácora de
    versiones.
  - Verificación: un cambio en la rama principal produce una subida visible en
    la consola con la versión correcta (C-03, C-04).
  - Archivos: `.github/workflows/release.yml`, `docs/bitacora-versiones.md`
  - Nota: cambiar el CI requiere autorización (constitución §4).

> **Verificación F3 (05/11):** subida automática funcionando de punta a punta.

---

## Fase F4 — Prueba cerrada · **límite 08/11/2026**

- [x] **T-009** — Reclutar testers _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-003 · Bloqueada por: **Q-03** (quiénes son)
  - Aceptación: lista de **al menos 15 personas** con cuenta de Google que
    aceptaron participar, para cubrir el mínimo de 12 con margen (D-06).
  - Verificación: lista con los correos confirmados.
  - Archivos: `docs/testers-prueba-cerrada.md`
  - Nota: puede y debe avanzar en paralelo desde ahora. No depende de nada
    técnico.

- [ ] **T-010** — Recorrido mínimo verificado antes de subir
  - Depende de: T-008
  - Aceptación: la app instalada desde el artefacto firmado **abre, permite
    registrarse y no se cierra sola**; no necesita estar completa, pero sí ser
    usable (RT-05).
  - Verificación: instalación manual del artefacto en un dispositivo limpio y
    recorrido básico.
  - Archivos: `docs/bitacora-versiones.md`

- [x] **T-011** — **Iniciar la prueba cerrada** _(implementada, por verificar · 17/11/2026)_
  - Depende de: T-009, T-010, T-006
  - Aceptación: el track de prueba cerrada está activo con los 12 o más
    testers inscritos, que recibieron la invitación e instalaron la app; la
    fecha de inicio queda registrada.
  - Verificación: estado en la consola con los testers activos (C-09) y fecha
    de inicio anotada.
  - Archivos: `memoria.md`, `docs/bitacora-versiones.md`
  - Nota: **tarea más crítica del proyecto.** Desde aquí corren 14 días
    continuos. Si inicia después del 08/11, no se llega al 02/12.

> **Verificación F4 (08/11):** prueba cerrada iniciada con 12+ testers.

---

## Fase F5 — Iteración · 08/11 a 22/11/2026

- [ ] **T-012** — Subidas sucesivas durante la prueba
  - Depende de: T-011
  - Aceptación: conforme se completan unidades (002 a 019), se suben
    actualizaciones al mismo track; **se verifica que actualizar no reinicie
    el contador de 14 días** (RT-06); cada subida queda en la bitácora.
  - Verificación: historial de versiones en la consola y bitácora al día
    (C-12).
  - Archivos: `docs/bitacora-versiones.md`

- [ ] **T-013** — Ensayar la reversión
  - Depende de: T-012
  - Aceptación: se ejecuta una reversión real en el track de prueba y se
    documenta el procedimiento con los pasos exactos (D-07).
  - Verificación: reversión ejecutada y documentada (C-13).
  - Archivos: `docs/procedimiento-reversion.md`

> **Verificación F5 (22/11):** 14 días completados; app usable; reversión
> probada.

---

## Fase F6 — Producción · 22/11 a 29/11/2026

- [ ] **T-014** — Enviar a producción y publicar
  - Depende de: T-013
  - Aceptación: se solicita el acceso a producción tras completar los 14 días;
    se atienden las observaciones si Google rechaza (se contemplan hasta dos
    ciclos, D-05); la app queda **descargable desde Google Play**; se verifica
    que la ruta de eliminar cuenta de 017 funciona en la app publicada;
    C-01 a C-14 comprobados; `memoria.md` actualizada.
  - Verificación: descarga desde un dispositivo ajeno al proyecto, instalación
    y recorrido básico (C-08, C-11, C-12, C-14).
  - Archivos: `memoria.md`, `docs/bitacora-versiones.md`

> **Verificación F6 (29/11):** app en producción, descargable, con 3 días de
> margen antes de la evaluación.

---

## Resumen

| Fase | Límite | Tareas | Entrega |
|---|---|---|---|
| F1 Cuenta | 29/10 | T-001 … T-003 | Cuenta, llave y nombre |
| F2 Ficha | 03/11 | T-004 … T-006 | Ficha, datos y privacidad |
| F3 Automatización | 05/11 | T-007, T-008 | Subida desde GitHub |
| F4 Prueba cerrada | **08/11** | T-009 … T-011 | **12 testers, prueba iniciada** |
| F5 Iteración | 22/11 | T-012, T-013 | Actualizaciones y reversión |
| F6 Producción | 29/11 | T-014 | App publicada |

**Bloqueadas:** T-001 (Q-02), T-002 (Q-06), T-003 (Q-05), T-004 (Q-07),
T-006 (Q-04), T-009 (Q-03).

---

## Advertencia final

Las fechas de este documento no son estimaciones: son **límites derivados del
plazo de Google**, que no se puede acelerar. T-001, T-002, T-003 y T-009 no
dependen de ninguna línea de código y pueden empezar hoy mismo.

Si para el **08/11/2026** la prueba cerrada no ha iniciado, el proyecto no
llega publicado a la evaluación del 02/12/2026, sin importar cuánto código
esté terminado.
