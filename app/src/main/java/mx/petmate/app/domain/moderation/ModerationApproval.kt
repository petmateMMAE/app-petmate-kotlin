package mx.petmate.app.domain.moderation

/**
 * Comprobante de que un contenido paso por moderacion y fue aprobado (001/T-038, C-25).
 *
 * Para que sirve: cualquier metodo que persista o envie contenido del usuario lo pide como
 * parametro. Como no hay forma de conseguir uno sin pasar por `ModeratedContent.publish`, una
 * feature que intente guardar sin moderar **no compila**: le falta el argumento y no tiene de
 * donde sacarlo. Eso es lo que pide T-038 con "ninguna feature puede saltarse el camino sin
 * romper la compilacion", y es mas fuerte que confiar en que cada quien se acuerde de llamar al
 * gateway.
 *
 * El constructor es `internal`, asi que nada fuera del modulo puede fabricarlo.
 *
 * El limite, dicho sin adornos: con un solo modulo de Gradle, `internal` es lo mas fuerte que da
 * el lenguaje. Alguien dentro del modulo puede escribir `ModerationApproval()` a mano. Lo que si
 * garantiza el compilador es que no se puede hacer *por descuido*: hay que escribirlo a proposito,
 * y eso salta a la vista en una revision. Si algun dia la app se parte en modulos, este
 * constructor deja de ser alcanzable y la garantia pasa a ser total.
 */
class ModerationApproval internal constructor()
