package mx.petmate.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

/** Fronteras de la fecha de revision con reloj fijo (spec 015, T-003, C-08, C-09). */
class ClassifyReviewDateUseCaseTest {

    private val classify = ClassifyReviewDateUseCase(today = { "2026-11-17" })

    @Test
    fun `vencida`() {
        assertEquals(ReviewDateClass.Overdue, classify("2026-11-16"))
    }

    @Test
    fun `hoy cuenta como urgente`() {
        assertEquals(ReviewDateClass.Urgent, classify("2026-11-17"))
    }

    @Test
    fun `a 3 dias es urgente y a 4 ya no`() {
        assertEquals(ReviewDateClass.Urgent, classify("2026-11-20"))
        assertEquals(ReviewDateClass.Soon, classify("2026-11-21"))
    }

    @Test
    fun `a 14 dias es pronto y a 15 ya es normal`() {
        assertEquals(ReviewDateClass.Soon, classify("2026-12-01"))
        assertEquals(ReviewDateClass.Normal, classify("2026-12-02"))
    }

    @Test
    fun `sin fecha o con fecha ilegible no se clasifica`() {
        assertEquals(ReviewDateClass.None, classify(null))
        assertEquals(ReviewDateClass.None, classify(""))
        assertEquals(ReviewDateClass.None, classify("17/11/2026"))
    }

    @Test
    fun `los umbrales salen de la constante compartida`() {
        assertEquals(3, DueThresholds.URGENT_DAYS)
        assertEquals(14, DueThresholds.SOON_DAYS)
    }
}
