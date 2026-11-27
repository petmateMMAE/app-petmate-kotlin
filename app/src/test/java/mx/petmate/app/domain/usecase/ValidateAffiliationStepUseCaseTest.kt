package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationRules
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.DraftedService
import mx.petmate.app.domain.model.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validacion por paso (spec 011, T-003).
 *
 * Antes esta validacion vivia en `BusinessForm.validate()`, dentro de `ui`.
 * Estas pruebas cubren las mismas reglas, ahora en el dominio y sin textos.
 */
class ValidateAffiliationStepUseCaseTest {

    private val validate = ValidateAffiliationStepUseCase()

    private val schedule = mapOf("monday" to listOf(TimeRange("09:00", "18:00")))

    private val vet = AffiliationDraft.EMPTY_BUSINESS.copy(
        kind = BusinessKind.Veterinary,
        name = "Veterinaria Centro",
        phone = "5512345678",
        address = "Calle 1, Centro",
        licenseNumber = "CED-100",
        schedule = schedule,
    )

    private fun issues(business: mx.petmate.app.domain.model.BusinessDraft, editing: Boolean = false) =
        validate.data(business, editing)

    private fun fields(business: mx.petmate.app.domain.model.BusinessDraft, editing: Boolean = false) =
        issues(business, editing).map(AffiliationRules.Issue::field)

    // ---- Paso 1: datos ----

    @Test
    fun `un negocio bien llenado no tiene problemas`() {
        assertTrue(issues(vet).isEmpty())
    }

    @Test
    fun `un formulario vacio reclama tipo, nombre, telefono y direccion`() {
        val f = fields(AffiliationDraft.EMPTY_BUSINESS)

        assertTrue(AffiliationRules.Field.Kind in f)
        assertTrue(AffiliationRules.Field.Name in f)
        assertTrue(AffiliationRules.Field.Phone in f)
        assertTrue(AffiliationRules.Field.Address in f)
    }

    @Test
    fun `el nombre debe tener al menos dos letras`() {
        assertTrue(AffiliationRules.Field.Name in fields(vet.copy(name = "A")))
        assertTrue(AffiliationRules.Field.Name in fields(vet.copy(name = "   ")))
        assertFalse(AffiliationRules.Field.Name in fields(vet.copy(name = "Al")))
    }

    @Test
    fun `el telefono debe tener diez digitos y admite el prefijo cincuenta y dos`() {
        assertTrue(AffiliationRules.Field.Phone in fields(vet.copy(phone = "55123456")))
        assertTrue(AffiliationRules.Field.Phone in fields(vet.copy(phone = "no es un telefono")))
        assertFalse(AffiliationRules.Field.Phone in fields(vet.copy(phone = "5512345678")))
        assertFalse(AffiliationRules.Field.Phone in fields(vet.copy(phone = "+525512345678")))
    }

    @Test
    fun `el whatsapp vacio se acepta pero uno mal escrito no`() {
        assertFalse(AffiliationRules.Field.WhatsApp in fields(vet.copy(whatsApp = "")))
        assertFalse(AffiliationRules.Field.WhatsApp in fields(vet.copy(whatsApp = "5512345678")))
        assertTrue(AffiliationRules.Field.WhatsApp in fields(vet.copy(whatsApp = "123")))
    }

    @Test
    fun `la direccion debe traer algo mas que dos letras`() {
        assertTrue(AffiliationRules.Field.Address in fields(vet.copy(address = "Av")))
        assertFalse(AffiliationRules.Field.Address in fields(vet.copy(address = "Calle 1")))
    }

    @Test
    fun `solo las veterinarias necesitan cedula`() {
        assertTrue(AffiliationRules.Field.License in fields(vet.copy(licenseNumber = "")))
        assertFalse(
            AffiliationRules.Field.License in
                fields(vet.copy(kind = BusinessKind.Store, licenseNumber = "")),
        )
        assertFalse(
            AffiliationRules.Field.License in
                fields(vet.copy(kind = BusinessKind.Grooming, licenseNumber = "")),
        )
    }

    @Test
    fun `el horario debe venir en formato veinticuatro horas`() {
        val bad = vet.copy(schedule = mapOf("monday" to listOf(TimeRange("9am", "6pm"))))
        val issue = issues(bad).single { it.field == AffiliationRules.Field.Hours }

        assertEquals(AffiliationRules.Problem.Invalid, issue.problem)
    }

    @Test
    fun `el cierre debe ser despues de la apertura`() {
        val bad = vet.copy(schedule = mapOf("monday" to listOf(TimeRange("18:00", "09:00"))))
        val issue = issues(bad).single { it.field == AffiliationRules.Field.Hours }

        assertEquals(AffiliationRules.Problem.CloseBeforeOpen, issue.problem)
    }

    @Test
    fun `cerrar a la misma hora que se abre no vale`() {
        val bad = vet.copy(schedule = mapOf("monday" to listOf(TimeRange("09:00", "09:00"))))

        assertTrue(AffiliationRules.Field.Hours in fields(bad))
    }

    @Test
    fun `sin dias marcados no se revisa el horario`() {
        assertFalse(AffiliationRules.Field.Hours in fields(vet.copy(schedule = emptyMap())))
    }

    // ---- Editar un negocio ya afiliado ----

    @Test
    fun `al editar no se reclaman el tipo, la direccion ni la cedula`() {
        val incomplete = AffiliationDraft.EMPTY_BUSINESS.copy(
            name = "Veterinaria Centro",
            phone = "5512345678",
            schedule = schedule,
        )

        val f = fields(incomplete, editing = true)

        assertFalse(AffiliationRules.Field.Kind in f)
        assertFalse(AffiliationRules.Field.Address in f)
        assertFalse(AffiliationRules.Field.License in f)
        assertTrue("lo demas si se sigue revisando", f.isEmpty())
    }

    @Test
    fun `al editar el nombre y el telefono siguen siendo obligatorios`() {
        val f = fields(vet.copy(name = "", phone = "123"), editing = true)

        assertTrue(AffiliationRules.Field.Name in f)
        assertTrue(AffiliationRules.Field.Phone in f)
    }

    // ---- Paso 2: servicios ----

    @Test
    fun `sin servicios el paso dos no pasa`() {
        val issue = validate.services(emptyList()).single()

        assertEquals(AffiliationRules.Field.Services, issue.field)
        assertEquals(AffiliationRules.Problem.NoneChosen, issue.problem)
    }

    @Test
    fun `un servicio con nombre vacio no cuenta`() {
        assertTrue(validate.services(listOf(DraftedService("   ", 100.0))).isNotEmpty())
    }

    @Test
    fun `un servicio bien puesto pasa`() {
        assertTrue(validate.services(listOf(DraftedService("Consulta", 350.0))).isEmpty())
    }

    @Test
    fun `un servicio sin precio pasa porque el precio es opcional`() {
        assertTrue(validate.services(listOf(DraftedService("Alimento", null))).isEmpty())
    }

    @Test
    fun `un precio negativo no pasa`() {
        val issue = validate.services(listOf(DraftedService("Consulta", -1.0))).single()

        assertEquals(AffiliationRules.Problem.NegativePrice, issue.problem)
    }

    @Test
    fun `un nombre de servicio demasiado largo no pasa`() {
        val long = "a".repeat(AffiliationRules.SERVICE_NAME_MAX + 1)
        val issue = validate.services(listOf(DraftedService(long, 100.0))).single()

        assertEquals(AffiliationRules.Problem.TooLong, issue.problem)
    }

    // ---- Paso 3: pin ----

    @Test
    fun `sin pin el paso tres no pasa`() {
        val issue = validate(AffiliationDraft(business = vet), AffiliationStep.Pin).single()

        assertEquals(AffiliationRules.Field.Pin, issue.field)
    }

    @Test
    fun `con pin el paso tres pasa`() {
        val draft = AffiliationDraft(business = vet, latitude = 19.43, longitude = -99.13)

        assertTrue(validate.isValid(draft, AffiliationStep.Pin))
    }

    @Test
    fun `media coordenada no es un pin`() {
        val onlyLat = AffiliationDraft(business = vet, latitude = 19.43, longitude = null)

        assertFalse(validate.isValid(onlyLat, AffiliationStep.Pin))
    }

    // ---- Los tres pasos juntos ----

    @Test
    fun `los tres pasos completos no dejan ningun problema`() {
        val draft = AffiliationDraft(
            business = vet,
            services = listOf(DraftedService("Consulta", 350.0)),
            latitude = 19.43,
            longitude = -99.13,
        )

        assertTrue(validate.all(draft).isEmpty())
        assertTrue(draft.isComplete)
    }

    @Test
    fun `un borrador vacio reclama algo en cada paso`() {
        val byStep = AffiliationStep.entries.associateWith { validate(AffiliationDraft.Empty, it) }

        AffiliationStep.entries.forEach {
            assertTrue("el paso $it debe reclamar algo", byStep.getValue(it).isNotEmpty())
        }
    }

    @Test
    fun `no se duplican las reglas, son las mismas de AffiliationRules`() {
        val draft = AffiliationDraft.Empty

        assertEquals(AffiliationRules.allIssues(draft), validate.all(draft))
    }
}
