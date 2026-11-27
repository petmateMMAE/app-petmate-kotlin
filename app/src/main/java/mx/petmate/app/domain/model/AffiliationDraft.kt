package mx.petmate.app.domain.model

/**
 * Avance del alta de un negocio (spec 011, T-002).
 *
 * El formulario son tres pasos: datos y tipo, servicios con precio, y pin en el
 * mapa. El borrador guarda lo que el usuario ya lleno para que no lo pierda si
 * sale a medias (R-06).
 */
enum class AffiliationStep {
    /** Paso 1: tipo de negocio, contacto, direccion, cedula y horario. */
    Data,

    /** Paso 2: servicios con precio. */
    Services,

    /** Paso 3: pin en el mapa. */
    Pin,
    ;

    val isFirst: Boolean get() = this == entries.first()

    /** Siguiente paso, o null si es el ultimo. */
    fun next(): AffiliationStep? = entries.getOrNull(ordinal + 1)

    /** Paso anterior, o null si es el primero. */
    fun previous(): AffiliationStep? = entries.getOrNull(ordinal - 1)
}

/** Servicio que el negocio ofrecera. [price] null es "sin precio publicado". */
data class DraftedService(val name: String, val price: Double? = null)

/**
 * Borrador de afiliacion: los tres pasos juntos.
 *
 * [latitude] y [longitude] son null hasta que el usuario pone el pin. Es un
 * modelo de dominio: Kotlin puro, sin Android, Room ni Retrofit.
 */
data class AffiliationDraft(
    val business: BusinessDraft = EMPTY_BUSINESS,
    val services: List<DraftedService> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val step: AffiliationStep = AffiliationStep.Data,
) {
    /** Si los tres pasos estan completos y se puede enviar. */
    val isComplete: Boolean get() = AffiliationRules.allIssues(this).isEmpty()

    companion object {
        val EMPTY_BUSINESS = BusinessDraft(
            kind = null,
            name = "",
            phone = "",
            whatsApp = "",
            address = "",
            description = "",
            licenseNumber = "",
            schedule = emptyMap(),
        )

        val Empty = AffiliationDraft()
    }
}

/**
 * Reglas de cada paso de la afiliacion (spec 011, T-003 y T-004).
 *
 * Salen del contrato del backend (pm-107): telefono de 10 digitos con +52
 * opcional, horario en formato 24 h y cedula obligatoria solo para
 * veterinarias. Devuelve problemas con nombre, no textos: el mensaje lo elige
 * la interfaz desde `strings.xml`.
 */
object AffiliationRules {

    const val NAME_MIN = 2
    const val ADDRESS_MIN = 5
    const val SERVICE_NAME_MAX = 60

    enum class Field { Kind, Name, Phone, WhatsApp, Address, License, Hours, Services, Pin }

    enum class Problem { Required, Invalid, TooShort, TooLong, NoneChosen, CloseBeforeOpen, NegativePrice }

    data class Issue(val field: Field, val problem: Problem)

    private val PHONE = Regex("""^(\+?52)?\d{10}$""")
    private val TIME = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    /** Telefono de 10 digitos; se admite el prefijo +52 (pm-107). */
    fun isValidPhone(value: String): Boolean =
        PHONE.matches(value.filter { !it.isWhitespace() && it != '-' })

    /** Problemas de un solo paso; lista vacia si ese paso esta bien. */
    fun issues(draft: AffiliationDraft, step: AffiliationStep): List<Issue> = when (step) {
        AffiliationStep.Data -> dataIssues(draft.business)
        AffiliationStep.Services -> serviceIssues(draft.services)
        AffiliationStep.Pin -> pinIssues(draft)
    }

    /** Problemas de los tres pasos juntos, en orden de paso. */
    fun allIssues(draft: AffiliationDraft): List<Issue> =
        AffiliationStep.entries.flatMap { issues(draft, it) }

    private fun dataIssues(business: BusinessDraft): List<Issue> = buildList {
        if (business.kind == null) add(Issue(Field.Kind, Problem.Required))
        val name = business.name.trim()
        when {
            name.isEmpty() -> add(Issue(Field.Name, Problem.Required))
            name.length < NAME_MIN -> add(Issue(Field.Name, Problem.TooShort))
        }
        if (!isValidPhone(business.phone)) add(Issue(Field.Phone, Problem.Invalid))
        // El WhatsApp es opcional: solo molesta si lo escribieron mal.
        if (business.whatsApp.isNotBlank() && !isValidPhone(business.whatsApp)) {
            add(Issue(Field.WhatsApp, Problem.Invalid))
        }
        val address = business.address.trim()
        when {
            address.isEmpty() -> add(Issue(Field.Address, Problem.Required))
            address.length < ADDRESS_MIN -> add(Issue(Field.Address, Problem.TooShort))
        }
        if (business.kind == BusinessKind.Veterinary && business.licenseNumber.isBlank()) {
            add(Issue(Field.License, Problem.Required))
        }
        addAll(scheduleIssues(business.schedule))
    }

    private fun scheduleIssues(schedule: Map<String, List<TimeRange>>): List<Issue> {
        val ranges = schedule.values.flatten()
        if (ranges.isEmpty()) return emptyList()
        if (ranges.any { !TIME.matches(it.open) || !TIME.matches(it.close) }) {
            return listOf(Issue(Field.Hours, Problem.Invalid))
        }
        // Comparacion de texto: con `HH:mm` equivale a comparar la hora.
        if (ranges.any { it.close <= it.open }) {
            return listOf(Issue(Field.Hours, Problem.CloseBeforeOpen))
        }
        return emptyList()
    }

    private fun serviceIssues(services: List<DraftedService>): List<Issue> = buildList {
        if (services.none { it.name.isNotBlank() }) {
            add(Issue(Field.Services, Problem.NoneChosen))
            return@buildList
        }
        if (services.any { it.name.trim().length > SERVICE_NAME_MAX }) {
            add(Issue(Field.Services, Problem.TooLong))
        }
        if (services.any { (it.price ?: 0.0) < 0.0 }) {
            add(Issue(Field.Services, Problem.NegativePrice))
        }
    }

    private fun pinIssues(draft: AffiliationDraft): List<Issue> =
        if (draft.latitude == null || draft.longitude == null) {
            listOf(Issue(Field.Pin, Problem.Required))
        } else {
            emptyList()
        }
}
