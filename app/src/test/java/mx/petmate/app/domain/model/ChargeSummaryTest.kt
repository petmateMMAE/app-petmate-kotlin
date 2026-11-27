package mx.petmate.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Renglon de cobro, tasa y resumen (spec 014, T-001; D-03).
 *
 * Lo que de verdad se comprueba aqui es que el total que ve el cliente cuadre con el del recibo.
 * Por eso hay pruebas de redondeo: son las que atrapan los errores de unos centavos, que son los
 * que hacen que nadie confie en el sistema.
 */
class ChargeSummaryTest {

    private val iva16 = TaxRate(1_600)

    private fun servicio(pesos: Long, cantidad: Int = 1) =
        ChargeLine("Consulta general", ChargeType.Service, Money.ofPesos(pesos), cantidad)

    // ---- TaxRate ----

    @Test
    fun `la tasa se guarda en puntos base y se muestra como porcentaje`() {
        assertEquals(16.0, iva16.percent, 0.0001)
        assertEquals("16%", iva16.format())
        assertEquals("16.5%", TaxRate(1_650).format())
    }

    @Test
    fun `no hay una tasa del pais escrita en el codigo`() {
        // D-03: la tasa llega de la configuracion del negocio. Lo unico declarado es la ausencia
        // de impuesto, que no es una tasa.
        assertTrue(TaxRate.Exempt.isExempt)
        assertEquals(0, TaxRate.Exempt.basisPoints)
    }

    @Test
    fun `la tasa se construye desde el porcentaje que manda la configuracion`() {
        assertEquals(TaxRate(1_600), TaxRate.ofPercent(16.0))
        assertEquals(TaxRate(1_650), TaxRate.ofPercent(16.5))
        assertEquals(TaxRate.Exempt, TaxRate.ofPercent(0.0))
    }

    @Test
    fun `una tasa imposible se rechaza en vez de cobrarse`() {
        assertNull(TaxRate.ofPercent(-1.0))
        assertNull(TaxRate.ofPercent(101.0))
        assertNull(TaxRate.ofPercent(Double.NaN))
        assertNull(TaxRate.ofPercent(null))
        assertNull(TaxRate.ofBasisPoints(10_001))
        assertNull(TaxRate.ofBasisPoints(null))
    }

    @Test
    fun `el impuesto sobre cero es cero`() {
        assertEquals(Money.Zero, iva16.on(Money.Zero))
        assertEquals(Money.Zero, TaxRate.Exempt.on(Money.ofPesos(1_000)))
    }

    @Test
    fun `el impuesto de una cifra redonda sale exacto`() {
        // 1000.00 al 16 % son 160.00 justos.
        assertEquals(Money.ofPesos(160), iva16.on(Money.ofPesos(1_000)))
    }

    @Test
    fun `medio centavo se redondea hacia arriba, como en la factura`() {
        // 1 centavo al 50 % es medio centavo: sube a 1, no baja a 0.
        assertEquals(Money(1), TaxRate(5_000).on(Money(1)))
        // 2 centavos al 25 % tambien es medio centavo.
        assertEquals(Money(1), TaxRate(2_500).on(Money(2)))
    }

    // ---- ChargeLine ----

    @Test
    fun `el renglon multiplica precio por cantidad`() {
        assertEquals(Money.ofPesos(750), servicio(250, cantidad = 3).total)
    }

    @Test
    fun `un renglon sin concepto no se construye`() {
        val error = runCatching { ChargeLine("   ", ChargeType.Service, Money.ofPesos(100)) }
        assertTrue(error.isFailure)
    }

    @Test
    fun `una cantidad menor que uno no se construye`() {
        val error = runCatching { servicio(100, cantidad = 0) }
        assertTrue(error.isFailure)
    }

    @Test
    fun `desde el formulario se recorta el concepto y se entiende el precio`() {
        val linea = ChargeLine.from("  Vacuna triple  ", ChargeType.Vaccine, "1,250.75")

        assertEquals("Vacuna triple", linea?.concept)
        assertEquals(Money(125_075), linea?.unitPrice)
        assertEquals(ChargeType.Vaccine, linea?.type)
    }

    @Test
    fun `un precio que no se entiende no crea un renglon inventado`() {
        assertNull(ChargeLine.from("Consulta", ChargeType.Service, "abc"))
        assertNull(ChargeLine.from("Consulta", ChargeType.Service, "-50"))
        assertNull(ChargeLine.from("", ChargeType.Service, "100"))
        assertNull(ChargeLine.from("Consulta", ChargeType.Service, "100", quantity = 0))
    }

    @Test
    fun `un renglon de cortesia es gratis pero existe`() {
        val cortesia = ChargeLine("Revision de cortesia", ChargeType.Service, Money.Zero)

        assertTrue(cortesia.isFree)
        assertEquals(Money.Zero, cortesia.total)
    }

    // ---- ChargeSummary ----

    @Test
    fun `el resumen suma subtotal, impuesto y total`() {
        val resumen = ChargeSummary(
            lines = listOf(
                servicio(350),
                ChargeLine("Vacuna triple", ChargeType.Vaccine, Money.ofPesos(250)),
                ChargeLine("Desparasitante", ChargeType.Medicine, Money.ofPesos(120), quantity = 2),
            ),
            taxRate = iva16,
        )

        assertEquals(Money.ofPesos(840), resumen.subtotal)
        assertEquals(Money.ofPesos(134) + Money(40), resumen.tax) // 840.00 * 16 % = 134.40
        assertEquals(Money(97_440), resumen.total)
    }

    @Test
    fun `el impuesto se redondea una sola vez, sobre el subtotal`() {
        // Esta es la razon de calcularlo sobre el subtotal y no renglon por renglon. Tres
        // renglones de 3 centavos: cada uno al 16 % redondea a 0, y sumarlos daria 0 de impuesto.
        // Sobre el subtotal de 9 centavos da 1 centavo, que es lo que dice la factura.
        val lineas = List(3) { ChargeLine("Insumo", ChargeType.Medicine, Money(3)) }
        val resumen = ChargeSummary(lineas, iva16)

        val porRenglon = Money.sum(lineas.map { iva16.on(it.total) })

        assertEquals(Money(9), resumen.subtotal)
        assertEquals(Money(1), resumen.tax)
        assertEquals(Money.Zero, porRenglon)
        assertNotEquals("Si estos coincidieran, la prueba no estaria probando nada", resumen.tax, porRenglon)
    }

    @Test
    fun `un resumen vacio no cobra nada`() {
        val vacio = ChargeSummary.empty(iva16)

        assertTrue(vacio.isEmpty)
        assertTrue(vacio.isFree)
        assertEquals(Money.Zero, vacio.subtotal)
        assertEquals(Money.Zero, vacio.tax)
        assertEquals(Money.Zero, vacio.total)
    }

    @Test
    fun `un negocio exento no cobra impuesto pero si el subtotal`() {
        val resumen = ChargeSummary(listOf(servicio(350)), TaxRate.Exempt)

        assertEquals(Money.ofPesos(350), resumen.subtotal)
        assertEquals(Money.Zero, resumen.tax)
        assertEquals(Money.ofPesos(350), resumen.total)
        assertFalse(resumen.isFree)
    }

    @Test
    fun `el desglose por tipo separa servicios, vacunas y medicinas`() {
        val resumen = ChargeSummary(
            lines = listOf(
                servicio(350),
                ChargeLine("Vacuna", ChargeType.Vaccine, Money.ofPesos(250)),
                ChargeLine("Medicina", ChargeType.Medicine, Money.ofPesos(100), quantity = 2),
            ),
            taxRate = iva16,
        )

        assertEquals(Money.ofPesos(350), resumen.subtotalOf(ChargeType.Service))
        assertEquals(Money.ofPesos(250), resumen.subtotalOf(ChargeType.Vaccine))
        assertEquals(Money.ofPesos(200), resumen.subtotalOf(ChargeType.Medicine))
    }

    @Test
    fun `agregar un renglon no modifica el resumen anterior`() {
        val original = ChargeSummary.empty(iva16)

        val conUno = original + servicio(350)

        assertTrue("El original debe quedar intacto", original.isEmpty)
        assertEquals(1, conUno.lines.size)
        assertEquals(Money.ofPesos(350), conUno.subtotal)
    }

    @Test
    fun `quitar un renglon por indice`() {
        val resumen = ChargeSummary(listOf(servicio(350), servicio(250)), iva16)

        val sinElPrimero = resumen.without(0)

        assertEquals(1, sinElPrimero.lines.size)
        assertEquals(Money.ofPesos(250), sinElPrimero.subtotal)
    }

    @Test
    fun `quitar un renglon que ya no esta no truena`() {
        // La interfaz puede pedir borrar dos veces el mismo renglon con dos pulsaciones rapidas.
        val resumen = ChargeSummary(listOf(servicio(350)), iva16)

        assertEquals(resumen, resumen.without(5))
        assertEquals(resumen, resumen.without(-1))
    }
}
