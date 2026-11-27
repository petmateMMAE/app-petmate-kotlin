package mx.petmate.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dinero exacto en centavos (spec 011, T-001).
 *
 * La prueba de la suma es la que `memoria.md` reclamaba desde el 26/11: con
 * `Double` el total de varios servicios se desviaba centavos.
 */
class MoneyTest {

    // ---- Suma: lo que Double hacia mal ----

    @Test
    fun `sumar diez y veinte centavos da exactamente treinta`() {
        val total = Money(10) + Money(20)

        assertEquals(30L, total.cents)
        assertEquals("0.30", total.format())
    }

    @Test
    fun `la suma que Double arruinaba sale exacta`() {
        // 0.1 + 0.2 en Double da 0.30000000000000004.
        val total = Money.parse("0.1")!! + Money.parse("0.2")!!

        assertEquals(Money.parse("0.3"), total)
        assertEquals("0.30", total.format())
    }

    @Test
    fun `sumar diez veces diez centavos da un peso justo`() {
        val total = Money.sum(List(10) { Money(10) })

        assertEquals(Money.ofPesos(1), total)
        assertEquals("1.00", total.format())
    }

    @Test
    fun `el total de una lista de servicios cuadra al centavo`() {
        val servicios = listOf(
            Money.parse("350.00")!!,
            Money.parse("250.50")!!,
            Money.parse("150.25")!!,
            Money.parse("0.25")!!,
        )

        assertEquals("751.00", servicios.total().format())
    }

    @Test
    fun `sumar una lista vacia da cero`() {
        assertEquals(Money.Zero, emptyList<Money>().total())
        assertTrue(Money.sum(emptyList()).isZero)
    }

    @Test
    fun `la suma es asociativa y conmutativa`() {
        val a = Money.parse("33.33")!!
        val b = Money.parse("66.67")!!
        val c = Money.parse("0.01")!!

        assertEquals((a + b) + c, a + (b + c))
        assertEquals(a + b, b + a)
        assertEquals("100.01", (a + b + c).format())
    }

    @Test
    fun `cien sumas de un centavo dan un peso`() {
        var total = Money.Zero
        repeat(100) { total += Money(1) }

        assertEquals("1.00", total.format())
    }

    // ---- Resta y multiplicacion ----

    @Test
    fun `restar da el cambio exacto`() {
        assertEquals("49.50", (Money.ofPesos(100) - Money.parse("50.50")!!).format())
    }

    @Test
    fun `restar mas de lo que hay no se permite`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.ofPesos(10) - Money.ofPesos(20)
        }
    }

    @Test
    fun `multiplicar por una cantidad no pierde centavos`() {
        assertEquals("1051.50", (Money.parse("350.50")!! * 3).format())
    }

    @Test
    fun `multiplicar por cero da cero`() {
        assertTrue((Money.parse("350.50")!! * 0).isZero)
    }

    @Test
    fun `una cantidad negativa no se permite`() {
        assertThrows(IllegalArgumentException::class.java) { Money.ofPesos(10) * -1 }
    }

    // ---- No existe el dinero negativo ----

    @Test
    fun `no se puede construir dinero negativo`() {
        assertThrows(IllegalArgumentException::class.java) { Money(-1) }
    }

    // ---- Formato ----

    @Test
    fun `siempre muestra dos decimales`() {
        assertEquals("0.00", Money.Zero.format())
        assertEquals("0.05", Money(5).format())
        assertEquals("0.50", Money(50).format())
        assertEquals("350.00", Money.ofPesos(350).format())
        assertEquals("1250.75", Money(125_075).format())
    }

    @Test
    fun `parte pesos y centavos`() {
        val price = Money(125_075)

        assertEquals(1250L, price.pesos)
        assertEquals(75, price.remainingCents)
    }

    @Test
    fun `el formato no trae simbolo de moneda porque eso lo pone la interfaz`() {
        val text = Money.ofPesos(350).format()

        assertTrue(text.none { it == '$' })
        assertTrue("tampoco separador de miles", Money(125_075).format().none { it == ',' })
    }

    // ---- Leer lo que escribe el usuario ----

    @Test
    fun `lee precios escritos de varias formas`() {
        assertEquals(Money.ofPesos(350), Money.parse("350"))
        assertEquals(Money(35_050), Money.parse("350.5"))
        assertEquals(Money(35_050), Money.parse("350.50"))
        assertEquals(Money(125_075), Money.parse("1,250.75"))
        assertEquals(Money(125_075), Money.parse(" 1250.75 "))
        assertEquals(Money(50), Money.parse(".50"))
    }

    @Test
    fun `trunca mas de dos decimales en lugar de redondear`() {
        assertEquals("350.99", Money.parse("350.999")!!.format())
    }

    @Test
    fun `lo que no es un precio no se acepta`() {
        listOf(null, "", "   ", "abc", "-50", "35.0.5", "350 pesos", "$350").forEach {
            assertNull("texto: $it", Money.parse(it))
        }
    }

    // ---- Puente con el Double del backend ----

    @Test
    fun `convierte el Double del backend al centavo mas cercano`() {
        assertEquals(Money.ofPesos(350), Money.fromDouble(350.0))
        assertEquals(Money(35_050), Money.fromDouble(350.5))
        assertEquals(Money(35_050), Money.fromDouble(350.499999))
    }

    @Test
    fun `un Double invalido o negativo no se convierte`() {
        listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertNull("valor: $it", Money.fromDouble(it))
        }
    }

    @Test
    fun `el precio de un servicio se lee como dinero`() {
        val withPrice = BusinessService("s1", "Consulta", null, 350.0)
        val withoutPrice = BusinessService("s2", "Alimento", null, null)

        assertEquals(Money.ofPesos(350), withPrice.money)
        assertNull(withoutPrice.money)
    }

    // ---- Comparacion ----

    @Test
    fun `se compara por centavos`() {
        assertTrue(Money(100) > Money(99))
        assertTrue(Money.Zero < Money(1))
        assertEquals(Money(100), Money(100))
        assertEquals(
            listOf(Money(1), Money(50), Money(100)),
            listOf(Money(100), Money(1), Money(50)).sorted(),
        )
    }
}
