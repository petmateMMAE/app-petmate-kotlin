# Spec 002 — Identidad y sesión

> Unidad de trabajo: `002-identidad-y-sesion`
> Estado: **redactada, pendiente de aprobación**
> Fecha: 29/10/2026 · Autor: Adrián Adonai García Jiménez
> Alineada con: [`constitution.md`](../../constitution.md) v1.1.0
> Depende de: [`001-arquitectura-base`](../001-arquitectura-base/spec.md)

---

## Supuestos

```
SUPUESTOS QUE ESTOY HACIENDO:

1. La unidad cubre identidad y sesión: bienvenida, alta de cuenta,
   verificación con código de 6 dígitos, inicio de sesión, sesión persistente,
   renovación automática del token, recuperación de acceso y cierre de sesión.

2. Solo se crea el rol Usuario. El modo Afiliado se activa después desde el
   perfil, en su propia spec; aquí no se elige rol al registrarse.

3. El alta usa correo + contraseña y verificación de 6 dígitos por CORREO
   electrónico. El SMS queda como alternativa futura y NO se especifica en
   esta unidad.

4. El inicio de sesión es con correo + contraseña. Si el backend solo permite
   entrar con código, la spec se ajusta cuando se confirme el contrato.

5. El esquema de tokens es el ya decidido en memoria §3: acceso de 15 min y
   refresh rotativo de 30 días. El transporte del refresh (cuerpo o cabecera)
   sigue siendo Q-04 de 001; la spec no fija uno.

6. La sesión se guarda cifrada en el dispositivo, sobrevive a la muerte del
   proceso y al reinicio, y no se cierra sola: solo el logout la termina.

7. La renovación es transparente y serializada: varias peticiones simultáneas
   no disparan renovaciones duplicadas y la petición original se reintenta una
   sola vez.

8. El logout borra siempre los tokens y datos de sesión locales e intenta
   invalidar en el servidor. Sin red, no bloquea al usuario; la política exacta
   de invalidación diferida va a preguntas abiertas.

9. Sin conexión no se puede registrar, verificar ni iniciar sesión. Una sesión
   ya abierta sigue funcionando con los datos locales.

10. La expiración del código, el máximo de intentos y el tiempo de reenvío los
    define el backend. La app muestra el temporizador que devuelve el servidor;
    no inventa valores.

11. "Probar demo" NO entra en esta unidad: abre el mapa con datos de ejemplo y
    muestra parte del carnet, así que depende de las specs de mapa y mascotas,
    que aún no existen. Se atenderá en la spec 010 (mapa e inicio).

12. En el alta se muestra la aceptación del aviso de privacidad con enlace a
    la landing. El texto legal y la página son de la spec 017.

13. Eliminar cuenta no entra aquí: es de la spec 017 (ajustes).

14. La base de push ya está en 001 (T-039). Esta unidad solo dispara el
    registro del token después del login y su borrado en el logout; no
    implementa la recepción ni el contenido de las notificaciones.

15. Dependencias: 001 aprobada e implementada, y contrato del backend para
    registro, verificación, login, refresh, logout y recuperación. Si ese
    contrato no está definido, la spec lo declara en preguntas abiertas y no
    supone rutas ni campos.

16. Los criterios de éxito se comprueban con dobles de prueba mientras el
    backend de desarrollo no esté desplegado. El umbral de tiempo contra el
    backend real queda como criterio diferido, marcado como tal.

→ Corrígeme ahora o avanzo con esto.
```

---

## 1. Objetivo

Permitir que una persona **cree su cuenta, verifique su correo, inicie sesión y
mantenga la sesión viva** sin volver a autenticarse, con un cierre de sesión
real que invalide el acceso y borre todo rastro local.

Esta unidad convierte la infraestructura de sesión que dejó 001 en un flujo de
producto completo. Sin ella, ninguna otra funcionalidad puede identificar al
dueño de las mascotas ni al afiliado que las atiende.

**Para quién:** el usuario dueño de mascotas, desde la pantalla de bienvenida
de la maqueta aprobada.

---

## 2. Alcance

Esta unidad sí entrega:

| ID | Capacidad |
|---|---|
| A-01 | Pantalla de bienvenida con acceso a iniciar sesión y a crear cuenta (la entrada al demo llega con la spec 010, mapa e inicio) |
| A-02 | Alta de cuenta con correo y contraseña, con aceptación del aviso de privacidad |
| A-03 | Verificación del correo con código de 6 dígitos y opción de reenvío |
| A-04 | Inicio de sesión con correo y contraseña |
| A-05 | Sesión persistente y cifrada que sobrevive al cierre y al reinicio |
| A-06 | Renovación automática y serializada del token de acceso |
| A-07 | Recuperación de acceso por correo |
| A-08 | Cierre de sesión local y remoto |
| A-09 | Manejo de errores de identidad sin revelar si una cuenta existe |
| A-10 | Modo degradado sin conexión para una sesión ya abierta |
| A-11 | Registro del token de push tras el login y borrado en el logout, sobre la base de 001 |

---

## 3. Fuera de alcance

Queda **explícitamente excluido** de esta unidad:

| Qué | Dónde se atenderá |
|---|---|
| "Probar demo" y sus datos de ejemplo | Spec 010 (mapa e inicio); necesita 007 (mascotas) y 008 (carnet) |
| Afiliación y modo negocio | Spec 011 (afiliación) |
| Eliminar cuenta | Spec 017 (ajustes) |
| Texto legal, aviso de privacidad completo y enlace a la landing | Spec 017 (ajustes) |
| Edición de perfil, foto y datos personales | Spec 017 (ajustes) |
| Recepción, contenido y segmentación de notificaciones push | Spec 006 (aquí solo el disparo del registro) |
| Moderación de contenido | Spec 004 |
| Autenticación con huella, rostro o biometría | No está en los lineamientos ni en la maqueta |
| Inicio de sesión con Google, Apple u otro proveedor externo | No está en la maqueta |
| Gestión de sesiones en varios dispositivos | No está en los lineamientos |
| SMS como canal de verificación | Alternativa futura; no se especifica ahora |

**Ninguna regla de negocio de mascotas, mapa o afiliación se escribe en esta
unidad.**

---

## 4. Requisitos

### Funcionales de identidad

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-01 | La app debe ofrecer una pantalla de bienvenida con acceso a iniciar sesión y a crear cuenta | Maqueta aprobada; lineamientos móvil |
| R-02 | El alta debe solicitar correo y contraseña, y exigir la aceptación del aviso de privacidad antes de continuar | Maqueta; lineamientos móvil |
| R-03 | El alta debe verificar el correo con un código de 6 dígitos enviado por correo electrónico, con opción de reenvío | Maqueta; lineamientos móvil |
| R-04 | El inicio de sesión debe autenticar con correo y contraseña y rechazar credenciales inválidas sin revelar si el correo existe | RNF-01 |
| R-05 | La sesión debe conservarse cifrada en el dispositivo y sobrevivir al cierre de la app y al reinicio | RF-57, RNF-31 |
| R-06 | El token de acceso debe renovarse automáticamente al expirar, sin intervención del usuario | RNF-31 |
| R-07 | Varias peticiones simultáneas con el token expirado deben compartir una sola renovación; la petición original se reintenta una sola vez | RNF-31 |
| R-08 | El cierre de sesión debe invalidar la sesión en el servidor y borrar todos los datos de sesión del dispositivo | RF-57, RNF-31 |
| R-09 | Sin conexión, el cierre de sesión debe borrar los datos locales y no bloquear al usuario; la invalidación remota pendiente debe quedar registrada | RF-57, RNF-31 |
| R-10 | La recuperación de acceso debe permitir recuperar la cuenta por correo sin revelar si el correo está registrado | RNF-01 |
| R-11 | El código de verificación debe expirar, limitar intentos y respetar el tiempo de reenvío que defina el servidor | RNF-01 |
| R-12 | La app debe respetar la política de contraseña del backend y no imponer reglas propias que la contradigan | RNF-01 |

### No funcionales de ingeniería

| ID | Requisito | Trazabilidad |
|---|---|---|
| R-13 | Toda comunicación de identidad debe ir por HTTPS | RNF-01, RNF-29 |
| R-14 | Ninguna credencial, contraseña ni token puede quedar en texto claro en el dispositivo ni en los registros | RNF-01, RNF-02, RNF-32 |
| R-15 | Los errores de identidad deben mostrarse con mensajes comprensibles y accionables, sin cerrar la app | RNF-16 |
| R-16 | Ninguna operación de red o de almacenamiento puede bloquear el hilo principal; la interfaz debe mostrar estado de carga de inmediato | RNF-15 |
| R-17 | Una sesión ya abierta debe permitir abrir la app y ver datos locales sin conexión; el alta, la verificación y el login requieren red | RF-53, RNF-28 |
| R-18 | Tras el login debe registrarse el token de push y en el logout debe borrarse, conforme a la base de 001 | RNF-32, 001 R-30 |

---

## 5. Criterios de éxito

Cada criterio es verificable por alguien que no escribió esta spec.

| # | Criterio | Cómo se comprueba |
|---|---|---|
| C-01 | La bienvenida muestra iniciar sesión y crear cuenta, y **no** muestra "probar demo" | Prueba de interfaz; la entrada al demo se agrega con la spec 010 (mapa e inicio) |
| C-02 | Un alta válida contra el doble de backend llega a la verificación y **no** abre sesión todavía | Prueba de la pantalla de alta con doble de backend |
| C-03 | Sin aceptar el aviso de privacidad, el alta no continúa y se explica por qué | Prueba de interfaz |
| C-04 | El código correcto activa la sesión; el incorrecto muestra error y no la activa | Prueba del flujo de verificación con doble de backend |
| C-05 | Un código expirado, un intento de más o un reenvío muestran el estado que devuelve el servidor y respetan su temporizador | Prueba con respuestas simuladas del servidor |
| C-06 | Credenciales inválidas y correo inexistente producen el mismo mensaje visible y el mismo tratamiento | Prueba con ambos casos y comparación de mensajes |
| C-07 | La sesión sobrevive a cerrar y reabrir la app, y al reinicio del proceso | Prueba instrumentada de persistencia de sesión |
| C-08 | Con el token expirado, la petición original se reintenta una sola vez tras renovar | Prueba del cliente con servidor local de prueba |
| C-09 | Diez peticiones simultáneas con el token expirado producen una sola renovación | Prueba de concurrencia con servidor local de prueba |
| C-10 | El logout con red invalida en el servidor y deja el almacenamiento de sesión vacío | Prueba del flujo de logout con doble de backend |
| C-11 | El logout sin red borra lo local, no bloquea al usuario y deja la invalidación pendiente registrada | Prueba con red simulada como ausente |
| C-12 | Las credenciales no son legibles en texto claro en el almacenamiento del dispositivo | Inspección del almacenamiento en un dispositivo de depuración |
| C-13 | Ni contraseñas ni tokens aparecen en los registros de la app | Búsqueda sobre la salida de registro de una sesión completa |
| C-14 | Con un servidor de prueba que responde lento, la pantalla muestra estado de carga antes de 300 ms y la comprobación de hilo principal no reporta operaciones de E/S | Prueba instrumentada con servidor lento y comprobación de hilo principal |
| C-15 | Sin red, el login muestra un error comprensible; una sesión ya abierta abre y muestra datos locales | Prueba con red simulada como ausente |
| C-16 | El token de push se registra tras el login y se borra en el logout | Prueba con doble del registro de push |
| C-17 | Una solicitud de recuperación con correo existente y con correo inexistente produce la misma respuesta visible | Prueba con ambos casos y comparación de mensajes |
| C-18 | **Diferido:** el login completo tarda menos de 3 s contra el backend real en 4G | Medición contra el entorno desplegado, cuando exista |

> C-18 queda **marcado como diferido**: no se puede comprobar mientras el
> backend de desarrollo no esté desplegado. No bloquea la aprobación de la
> unidad, pero debe verificarse antes de la liberación.

---

## 6. Riesgos

| # | Riesgo | Impacto | Mitigación |
|---|---|---|---|
| G-01 | El contrato del backend de identidad no está cerrado y la unidad se diseña a ciegas | Alto | Todo se prueba con dobles; las rutas y los campos van a Q-01 y no se inventan. El contrato se acuerda antes de ejecutar las tareas |
| G-02 | Varias peticiones simultáneas disparan renovaciones duplicadas o un ciclo infinito | Alto | Renovación serializada y un solo reintento por petición; pruebas C-08 y C-09 |
| G-03 | Los mensajes de error permiten descubrir qué correos están registrados | Medio | Mensajes idénticos para credenciales inválidas y correo inexistente; prueba C-06 y C-17 |
| G-04 | Fuerza bruta contra el código de 6 dígitos | Medio | Límite de intentos y expiración del servidor; la app solo muestra el resultado; prueba C-05 |
| G-05 | El almacenamiento cifrado falla en algún dispositivo y deja la app sin sesión | Medio | Degradación controlada y registrada, nunca respaldo en texto claro; prueba C-12 |
| G-06 | El logout sin red deja una sesión viva en el servidor | Medio | Invalidación pendiente registrada y expiración corta del token de acceso; se documenta en Q-06 |
| G-07 | El correo de verificación no llega o cae en spam | Medio | Reenvío con temporizador y mensaje de ayuda; el envío es responsabilidad del backend |
| G-08 | Una operación de red o de almacenamiento bloquea el hilo principal | Medio | Ejecución fuera del hilo principal y estado de carga inmediato; prueba C-14 |
| G-09 | Un cambio de formato de la sesión guardada deja a los usuarios sin poder entrar | Bajo | Si el formato cambia, la sesión se invalida y se pide login de nuevo, sin migraciones silenciosas |

---

## 7. Dependencias

**De esta unidad:**

- `001-arquitectura-base` aprobada e implementada: almacenamiento cifrado de
  sesión, cliente HTTP, modelo de errores, navegación e inyección de
  dependencias.

**Externas a esta unidad:**

- Contrato del backend de identidad: registro, verificación, reenvío, login,
  renovación, logout y recuperación (Q-01).
- Formato de error del backend (Q-03 de 001).
- Transporte del token de renovación (Q-04 de 001).
- Servicio de correo transaccional del backend para los códigos de
  verificación y recuperación.
- URL del aviso de privacidad o de la landing (Q-07).

**Unidades posteriores que dependen de esta:**

- 007 (mascotas), 008 (carnet), 009 (compartir historial), 010 (mapa e
  inicio), 011 (afiliación), 012 (panel del afiliado), 013 (escáner),
  014 (consulta), 015 (seguimiento), 016 (chat) y 017 (ajustes) necesitan saber
  quién es el usuario.
- El demo queda fuera de esta unidad y llegará con la spec 010 (mapa e inicio).

---

## 8. Preguntas abiertas

| # | Pregunta | Para quién | Bloquea |
|---|---|---|---|
| Q-01 | ¿Cuáles son las rutas, los cuerpos, los códigos de estado y las respuestas exactas de registro, verificación, reenvío, login, renovación, logout y recuperación? | Miguel (backend) | Todas las tareas de red |
| Q-02 | ¿Cuál es la política de contraseña (longitud mínima, complejidad, confirmación) y la define el backend o la app? | Miguel (backend) / equipo | Tareas de alta |
| Q-03 | ¿Cuánto dura el código, cuántos intentos permite y cuánto tarda el reenvío? ¿El servidor devuelve esos valores en la respuesta? | Miguel (backend) | Tareas de verificación |
| Q-04 | ¿El login acepta solo correo o también teléfono? ¿El correo se normaliza (mayúsculas, espacios)? | Miguel (backend) | Tareas de login |
| Q-05 | ¿El token de renovación viaja en el cuerpo o en una cabecera? (hereda Q-04 de 001) | Miguel (backend) | Tareas de renovación |
| Q-06 | Sin red, ¿el logout debe encolar la invalidación remota o se acepta que la sesión viva hasta que expire el refresh? | Equipo / backend | Tareas de logout |
| Q-07 | ¿Cuál es la URL del aviso de privacidad y la aceptación se guarda en el servidor o solo en el dispositivo? | Equipo / landing | Tareas de alta |
| Q-08 | La recuperación, ¿es por enlace mágico o por código de 6 dígitos? ¿Cambia la contraseña o solo restablece la sesión? | Miguel (backend) | Tareas de recuperación |
| Q-09 | ¿Qué datos personales se piden en el alta además de correo y contraseña (nombre, teléfono, fecha de nacimiento)? | Equipo / maqueta | Tareas de alta |
| Q-10 | ¿Qué códigos y mensajes de error devuelve identidad y cuáles debe traducir la app? (hereda Q-03 de 001) | Miguel (backend) | Tareas de error |

> Las preguntas abiertas **no se responden inventando**. Cada una bloquea la
> tarea indicada hasta que se resuelva.

---

## 9. Puerta de aprobación

- [ ] El alcance y el fuera de alcance están revisados.
- [ ] Los supuestos corregidos (demo fuera, solo correo, criterio diferido)
      están aceptados.
- [ ] Los requisitos y los criterios de éxito son suficientes para planificar.
- [ ] Las preguntas abiertas están identificadas y se sabe a quién preguntar.
- [ ] **Aprobada para pasar a `plan.md`.**

---

## Resoluciones con el backend real (06/11/2026)

El backend PetMate v0.1.0 quedó desplegado en Azure (Chile Central) y la app se conecta a él (`USE_FAKE_BACKEND=false` por defecto). Contrato fuente: `docs/api/endpoints.md` del repositorio `backend-petmate-sdd-aspnetcore`.

| Pregunta | Resolución |
|---|---|
| Q-01 | **Resuelta para registro, login, renovación y logout.** Base `/api/v1/`; todas las respuestas van envueltas en `{ statusCode, message, data }`. `POST auth/register` y `auth/login` con `{ email, password }` → `data` = `{ accessToken, tokenType, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt, user { id, email, role } }`. `POST auth/refresh` con `{ refreshToken }` (rota el token). `POST auth/logout` con `Authorization: Bearer <access>` y `{ refreshToken }`. Verificación por correo, reenvío y recuperación **no existen todavía** en el backend: siguen abiertas. |
| Q-02 | **Resuelta:** la define el backend, 12 a 128 caracteres. La app valida lo mismo antes de enviar. |
| Q-04 | Solo correo. La app recorta espacios; el backend normaliza. |
| Q-05 | **Resuelta:** el refresh token viaja en el cuerpo. El access token dura 15 min; si el logout recibe 401, la app renueva una vez y reintenta. |
| Q-06 | Sin red, el logout borra la sesión local y la remota vence sola (refresh 30 días de inactividad). No se encola. |
| Q-09 | El backend v0.1.0 **no acepta nombre** (rechaza miembros desconocidos con 400). La app conserva el nombre solo en el dispositivo, cifrado, hasta que existan `users/me` (pm-037/038). |
| Q-10 | 400 validación, 401 credenciales/sesión, 403 sin permiso, 409 correo registrado, 429 límite de 10/min por IP, 503 dependencia caída. La app muestra un mensaje propio para cada uno. |
| Rol | `role = Business` → `isAffiliate = true`; `Owner` → dueño. |