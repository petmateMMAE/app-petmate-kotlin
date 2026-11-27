package mx.petmate.app.util

import mx.petmate.app.core.log.Logger

/**
 * [Logger] que apunta lo que se registra, para poder exigirlo en una prueba (001/T-023).
 *
 * Para que sirve de verdad: la constitucion P6 prohibe registrar contrasenas, tokens y datos
 * personales, y [noRegistro] permite comprobarlo en vez de confiar en que nadie lo haga.
 * `mockk(relaxed = true)` sirve cuando al registro no se le pide nada; cuando hay que mirar lo que
 * se escribio, esto se lee mucho mejor en la prueba.
 */
class RecordingLogger : Logger {

    data class Entrada(
        val nivel: Nivel,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
    )

    enum class Nivel { DEBUG, ERROR }

    private val _entradas = mutableListOf<Entrada>()

    /** Todo lo registrado, en orden. */
    val entradas: List<Entrada> get() = _entradas.toList()

    /** Solo los mensajes, que es lo que casi siempre se quiere comprobar. */
    val mensajes: List<String> get() = _entradas.map { it.message }

    val errores: List<Entrada> get() = _entradas.filter { it.nivel == Nivel.ERROR }

    override fun debug(tag: String, message: String) {
        _entradas += Entrada(Nivel.DEBUG, tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        _entradas += Entrada(Nivel.ERROR, tag, message, throwable)
    }

    /** True si ningun mensaje contiene [secreto]. Para comprobar P6 sin adivinar. */
    fun noRegistro(secreto: String): Boolean = _entradas.none { it.message.contains(secreto) }

    fun limpiar() = _entradas.clear()
}
