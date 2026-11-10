package mx.petmate.app.domain.usecase

import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.VaccineDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Reglas de mascotas y carnet, copiadas del contrato del backend (pm-040, pm-096 a pm-103) para avisar
 * antes de enviar. El backend las vuelve a validar: estas solo ahorran un viaje y dan mensajes claros.
 *
 * Fechas `yyyy-MM-dd`. [today] se inyecta para poder probar con un dia fijo (spec 007, D-01).
 */
object PetRules {
    const val NAME_MAX = 50
    const val BREED_MAX = 60
    const val MAX_AGE_YEARS = 50
    const val MAX_KILOGRAMS = 200.0

    enum class Field { Name, Species, Breed, BirthDate, AppliedOn, NextDate, Product, Kilograms }

    enum class Problem { Required, TooLong, Future, TooOld, NotAfterApplied, NotFuture, OutOfRange }

    data class Issue(val field: Field, val problem: Problem)

    fun validatePet(draft: PetDraft, today: String): List<Issue> = buildList {
        val name = draft.name.trim()
        when {
            name.isEmpty() -> add(Issue(Field.Name, Problem.Required))
            name.length > NAME_MAX -> add(Issue(Field.Name, Problem.TooLong))
        }
        if (draft.species == null) add(Issue(Field.Species, Problem.Required))
        if (draft.breed.trim().length > BREED_MAX) add(Issue(Field.Breed, Problem.TooLong))
        draft.birthDate?.let { birth ->
            when {
                birth > today -> add(Issue(Field.BirthDate, Problem.Future))
                birth < PetDates.minusYears(today, MAX_AGE_YEARS) -> add(Issue(Field.BirthDate, Problem.TooOld))
            }
        }
    }

    fun validateVaccine(draft: VaccineDraft, today: String): List<Issue> = buildList {
        if (draft.name.isBlank()) add(Issue(Field.Name, Problem.Required))
        if (draft.appliedOn > today) add(Issue(Field.AppliedOn, Problem.Future))
        draft.nextDoseOn?.let { if (it <= draft.appliedOn) add(Issue(Field.NextDate, Problem.NotAfterApplied)) }
    }

    fun validateDeworming(draft: DewormingDraft, today: String): List<Issue> = buildList {
        if (draft.product.isBlank()) add(Issue(Field.Product, Problem.Required))
        if (draft.appliedOn > today) add(Issue(Field.AppliedOn, Problem.Future))
        draft.nextOn?.let { if (it <= today) add(Issue(Field.NextDate, Problem.NotFuture)) }
    }

    fun validateWeight(kilograms: Double): List<Issue> =
        if (kilograms <= 0.0 || kilograms > MAX_KILOGRAMS) listOf(Issue(Field.Kilograms, Problem.OutOfRange)) else emptyList()

    fun validateAllergy(draft: AllergyDraft): List<Issue> =
        if (draft.name.isBlank()) listOf(Issue(Field.Name, Problem.Required)) else emptyList()

    /** El backend acepta un decimal en el peso. */
    fun roundKilograms(value: Double): Double = Math.round(value * 10) / 10.0
}

/** Calculos de fechas `yyyy-MM-dd` sin java.time (minSdk 24). */
object PetDates {
    private fun parser() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    /** Fecha de hoy en la zona del telefono. */
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

    fun fromMillisUtc(millis: Long): String = parser().format(millis)

    fun toMillisUtc(iso: String): Long? = runCatching { parser().parse(iso)?.time }.getOrNull()

    fun minusYears(iso: String, years: Int): String {
        val millis = toMillisUtc(iso) ?: return iso
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
            add(Calendar.YEAR, -years)
        }
        return parser().format(cal.time)
    }

    /** Dias de [from] a [to] (negativo si [to] ya paso). */
    fun daysBetween(from: String, to: String): Int? {
        val a = toMillisUtc(from) ?: return null
        val b = toMillisUtc(to) ?: return null
        return ((b - a) / DAY_MILLIS).toInt()
    }

    /** Edad en anos y meses cumplidos a [today]. */
    fun age(birthDate: String, today: String): Pair<Int, Int>? {
        val birth = birthDate.split("-").mapNotNull { it.toIntOrNull() }
        val now = today.split("-").mapNotNull { it.toIntOrNull() }
        if (birth.size != 3 || now.size != 3) return null
        var months = (now[0] - birth[0]) * 12 + (now[1] - birth[1])
        if (now[2] < birth[2]) months -= 1
        if (months < 0) return null
        return months / 12 to months % 12
    }

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
