package mx.petmate.app.domain.model

/** Que se esta cobrando en un renglon (spec 014, T-001). */
enum class ChargeType {
    /** Consulta, bano, curacion: lo que hace el negocio. */
    Service,

    /** Una vacuna aplicada, que ademas queda en el carnet de 008. */
    Vaccine,

    /** Un medicamento surtido o aplicado. */
    Medicine,
}

/**
 * Un renglon del cobro de una consulta (spec 014, T-001).
 *
 * El precio va en [Money], o sea en centavos enteros, no en `Double`: sumando precios en punto
 * flotante el total se desviaba unos centavos y no cuadraba con el recibo.
 *
 * [quantity] existe porque un mismo concepto se cobra varias veces en la misma consulta (dos
 * dosis, tres curaciones) y repetir el renglon obligaria a la interfaz a agruparlos para mostrar
 * algo legible.
 */
data class ChargeLine(
    val concept: String,
    val type: ChargeType,
    val unitPrice: Money,
    val quantity: Int = 1,
) {

    init {
        require(concept.isNotBlank()) { "El concepto del renglon no puede estar vacio" }
        require(quantity >= 1) { "La cantidad debe ser al menos 1, no $quantity" }
    }

    /** Lo que suma este renglon: precio unitario por cantidad. */
    val total: Money get() = unitPrice * quantity

    /** True cuando el renglon no cuesta nada (una revision de cortesia, por ejemplo). */
    val isFree: Boolean get() = total.isZero

    companion object {

        /**
         * Construye un renglon desde lo que escribio el usuario.
         *
         * Devuelve null cuando el concepto esta vacio, el precio no se entiende o la cantidad no
         * sirve: el formulario ensena el error en lugar de guardar un cobro inventado. El concepto
         * se recorta, porque un espacio al final no deberia crear un concepto distinto.
         */
        fun from(
            concept: String?,
            type: ChargeType,
            price: String?,
            quantity: Int = 1,
        ): ChargeLine? {
            val limpio = concept?.trim().orEmpty()
            if (limpio.isEmpty() || quantity < 1) return null
            val dinero = Money.parse(price) ?: return null
            return ChargeLine(limpio, type, dinero, quantity)
        }
    }
}
