package mx.petmate.app.domain.model

/**
 * El cobro completo de una consulta: subtotal, impuesto y total (spec 014, T-001).
 *
 * El impuesto **se suma** al subtotal, no viene incluido en los precios. Es una decision y hay que
 * saberla: si el negocio captura precios con IVA incluido, el total saldria cobrado dos veces. La
 * spec pide los tres numeros por separado (subtotal, IVA y total), que es como se lee un recibo, y
 * eso solo tiene sentido si el impuesto va encima. **Confirmar con Adrian o con el negocio** antes
 * de ensenar esto en una pantalla de cobro real.
 *
 * El impuesto se calcula sobre el subtotal y no renglon por renglon, a proposito: redondear cada
 * renglon por separado y luego sumar da un total distinto (unos centavos) al de la factura, que
 * redondea una sola vez al final.
 */
data class ChargeSummary(
    val lines: List<ChargeLine>,
    val taxRate: TaxRate,
) {

    /** Suma de los renglones, sin impuesto. */
    val subtotal: Money get() = Money.sum(lines.map { it.total })

    /** El impuesto que le toca al subtotal, redondeado una sola vez. */
    val tax: Money get() = taxRate.on(subtotal)

    /** Lo que paga el cliente. */
    val total: Money get() = subtotal + tax

    val isEmpty: Boolean get() = lines.isEmpty()

    /** True cuando no hay nada que cobrar, aunque haya renglones de cortesia. */
    val isFree: Boolean get() = total.isZero

    /** El subtotal de un solo tipo de concepto, para desglosar el recibo. */
    fun subtotalOf(type: ChargeType): Money =
        Money.sum(lines.filter { it.type == type }.map { it.total })

    /** Agrega un renglon y devuelve un resumen nuevo: el original no se toca. */
    operator fun plus(line: ChargeLine): ChargeSummary = copy(lines = lines + line)

    /**
     * Quita el renglon que esta en [index].
     *
     * Devuelve el mismo resumen si el indice no existe, en vez de tronar: la interfaz puede pedir
     * borrar un renglon que ya se borro en otra pulsacion y eso no deberia cerrar la app.
     */
    fun without(index: Int): ChargeSummary =
        if (index !in lines.indices) this else copy(lines = lines.filterIndexed { i, _ -> i != index })

    companion object {

        /** Un cobro vacio con la tasa del negocio, para empezar a capturar. */
        fun empty(taxRate: TaxRate) = ChargeSummary(emptyList(), taxRate)
    }
}
