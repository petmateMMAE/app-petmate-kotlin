package mx.petmate.app.domain.model

/**
 * Dinero en centavos (spec 011, T-001).
 *
 * **Por que no `Double`:** 0.1 + 0.2 no da 0.3 en punto flotante. Sumando
 * precios de servicios, el total de un cobro se desviaba unos centavos y no
 * cuadraba con el recibo. Un entero de centavos no tiene ese problema: toda la
 * aritmetica es exacta.
 *
 * Es un `value class`, asi que en tiempo de ejecucion es un `Long` y no cuesta
 * nada envolver cada precio.
 *
 * Solo hay una moneda (peso mexicano), asi que no se guarda: el dia que haya
 * otra, este es el unico lugar que cambia.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    init {
        require(cents >= 0) { "El dinero no puede ser negativo: $cents centavos" }
    }

    /** Parte entera en pesos, sin los centavos. */
    val pesos: Long get() = cents / CENTS_PER_PESO

    /** Centavos sueltos, de 0 a 99. */
    val remainingCents: Int get() = (cents % CENTS_PER_PESO).toInt()

    val isZero: Boolean get() = cents == 0L

    operator fun plus(other: Money) = Money(cents + other.cents)

    operator fun minus(other: Money): Money {
        require(cents >= other.cents) { "El resultado seria negativo" }
        return Money(cents - other.cents)
    }

    /** Multiplica por una cantidad (dos consultas, tres vacunas). */
    operator fun times(quantity: Int): Money {
        require(quantity >= 0) { "La cantidad no puede ser negativa: $quantity" }
        return Money(cents * quantity)
    }

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    /**
     * Formato para mostrar, siempre con dos decimales: `350.00`.
     *
     * No lleva simbolo de moneda ni separador de miles: eso lo pone la interfaz,
     * que es la que conoce el idioma del telefono. El dominio no da formato
     * local (constitucion P3).
     */
    fun format(): String = "$pesos.${remainingCents.toString().padStart(2, '0')}"

    override fun toString(): String = format()

    companion object {
        const val CENTS_PER_PESO = 100L

        val Zero = Money(0)

        /** Desde pesos enteros: `Money.ofPesos(350)` son 350.00. */
        fun ofPesos(pesos: Long) = Money(pesos * CENTS_PER_PESO)

        /**
         * Desde lo que escribio el usuario (`"350"`, `"350.5"`, `"1,250.75"`).
         *
         * Null si no se entiende o si es negativo: el formulario muestra el
         * error en lugar de guardar un precio inventado. Mas de dos decimales
         * se truncan, no se redondean: nadie cobra fracciones de centavo.
         */
        fun parse(text: String?): Money? {
            val clean = text?.replace(",", "")?.replace(" ", "")?.trim().orEmpty()
            if (clean.isEmpty() || clean.startsWith("-")) return null
            if (!clean.all { it.isDigit() || it == '.' }) return null

            val parts = clean.split(".")
            if (parts.size > 2) return null
            val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
            val fraction = parts.getOrNull(1).orEmpty().take(2).padEnd(2, '0')
            val fractionValue = if (fraction.isEmpty()) 0L else fraction.toLongOrNull() ?: return null

            return Money(whole * CENTS_PER_PESO + fractionValue)
        }

        /**
         * Desde el `Double` que todavia manda el backend.
         *
         * Es el puente mientras los DTOs sigan usando `Double`; se redondea al
         * centavo mas cercano, que es lo mejor que se puede hacer con un valor
         * que ya perdio precision. Null si viene negativo o no es un numero.
         */
        fun fromDouble(value: Double?): Money? {
            if (value == null || value.isNaN() || value.isInfinite() || value < 0) return null
            return Money(Math.round(value * CENTS_PER_PESO))
        }

        /** Suma una lista de precios. Vacia da [Zero]. */
        fun sum(amounts: List<Money>): Money = amounts.fold(Zero, Money::plus)
    }
}

/** Suma de precios, para encadenar sobre una lista. */
fun List<Money>.total(): Money = Money.sum(this)

/** El precio del servicio como [Money]; null cuando no tiene precio publicado. */
val BusinessService.money: Money? get() = Money.fromDouble(price)
