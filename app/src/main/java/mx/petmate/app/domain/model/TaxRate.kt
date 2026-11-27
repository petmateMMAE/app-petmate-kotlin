package mx.petmate.app.domain.model

/**
 * Tasa de impuesto, en puntos base (spec 014, T-001; D-03).
 *
 * **Por que puntos base y no un `Double`:** 1600 puntos base son 16 %. Guardar `0.16` en punto
 * flotante y multiplicarlo por el subtotal reintroduce justo el error que [Money] evita usando
 * centavos enteros. Con enteros, el impuesto de un cobro siempre sale igual.
 *
 * **Por que no hay una constante de 16 % aqui:** la decision D-03 lo prohibe a proposito. La tasa
 * llega desde la configuracion del negocio o del backend, porque una tasa escrita en el codigo
 * obliga a publicar una version nueva de la app el dia que cambie. Lo unico que se declara aqui es
 * [Exempt], que es cero y no es una tasa del pais sino la ausencia de impuesto.
 */
@JvmInline
value class TaxRate(val basisPoints: Int) {

    init {
        require(basisPoints in 0..MAX_BASIS_POINTS) {
            "La tasa debe estar entre 0 y $MAX_BASIS_POINTS puntos base, no $basisPoints"
        }
    }

    val isExempt: Boolean get() = basisPoints == 0

    /** La tasa como porcentaje, solo para mostrar: 1600 -> 16.0. */
    val percent: Double get() = basisPoints / 100.0

    /**
     * El impuesto que le toca a [amount], redondeado al centavo mas cercano.
     *
     * Se redondea hacia arriba en el medio exacto (la mitad de un centavo sube), que es lo que
     * hacen las facturas: asi el total de la app coincide con el del recibo.
     */
    fun on(amount: Money): Money {
        if (isExempt || amount.isZero) return Money.Zero
        val producto = amount.cents * basisPoints
        return Money((producto + MAX_BASIS_POINTS / 2) / MAX_BASIS_POINTS)
    }

    /** Formato para mostrar: `16%` o `16.5%`. Sin texto traducible: eso lo pone la interfaz. */
    fun format(): String =
        if (basisPoints % 100 == 0) "${basisPoints / 100}%" else "$percent%"

    override fun toString(): String = format()

    companion object {
        /** 10 000 puntos base = 100 %. */
        const val MAX_BASIS_POINTS = 10_000

        /** Sin impuesto. No es una tasa del pais: es la ausencia de una. */
        val Exempt = TaxRate(0)

        /**
         * Desde el porcentaje que manda la configuracion del negocio (`16`, `16.5`, `0`).
         *
         * Null si viene negativo, pasa de 100 o no es un numero: el formulario ensena el error en
         * vez de cobrar un impuesto inventado. Mas de dos decimales de porcentaje se truncan,
         * porque un punto base es el grano mas fino que se guarda.
         */
        fun ofPercent(percent: Double?): TaxRate? {
            if (percent == null || percent.isNaN() || percent.isInfinite()) return null
            if (percent < 0 || percent > 100) return null
            return TaxRate((percent * 100).toInt())
        }

        /** Desde puntos base, validando. Null en vez de excepcion, para datos que llegan de fuera. */
        fun ofBasisPoints(basisPoints: Int?): TaxRate? {
            if (basisPoints == null || basisPoints !in 0..MAX_BASIS_POINTS) return null
            return TaxRate(basisPoints)
        }
    }
}
