package mx.petmate.app.domain.model

/*
 * Carnet de salud (spec 008; contrato del backend pm-096 a pm-103).
 * Fechas como texto `yyyy-MM-dd`. Lo que registra un negocio es de solo lectura para el dueno.
 */

/** Quien registro el dato. */
data class RecordOrigin(
    val byBusiness: Boolean,
    val businessName: String? = null,
) {
    companion object {
        val Owner = RecordOrigin(byBusiness = false)
    }
}

data class Vaccine(
    val id: String,
    val name: String,
    val appliedOn: String,
    val batch: String?,
    val appliedBy: String?,
    val nextDoseOn: String?,
    val origin: RecordOrigin,
)

data class VaccineDraft(
    val name: String,
    val appliedOn: String,
    val batch: String,
    val appliedBy: String,
    val nextDoseOn: String?,
)

enum class DewormingType(val apiValue: String) {
    Internal("Internal"),
    External("External"),
    ;

    companion object {
        fun fromApi(value: String?): DewormingType? = entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

data class Deworming(
    val id: String,
    val product: String,
    val appliedOn: String,
    val type: DewormingType?,
    val appliedBy: String?,
    val nextOn: String?,
    val origin: RecordOrigin,
)

data class DewormingDraft(
    val product: String,
    val appliedOn: String,
    val type: DewormingType,
    val appliedBy: String,
    val nextOn: String?,
)

data class WeightRecord(
    val id: String,
    val measuredOn: String,
    val kilograms: Double,
    val origin: RecordOrigin,
)

enum class AllergyKind(val apiValue: String) {
    Allergy("Allergy"),
    Condition("Condition"),
    ;

    companion object {
        fun fromApi(value: String?): AllergyKind = entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: Allergy
    }
}

data class Allergy(
    val id: String,
    val name: String,
    val kind: AllergyKind,
    val notes: String?,
    val active: Boolean,
    val origin: RecordOrigin,
)

data class AllergyDraft(
    val name: String,
    val kind: AllergyKind,
    val notes: String,
    val active: Boolean,
)

/** Todo el carnet de una mascota, para la pantalla resumen. */
data class Carnet(
    val vaccines: List<Vaccine> = emptyList(),
    val dewormings: List<Deworming> = emptyList(),
    val weights: List<WeightRecord> = emptyList(),
    val allergies: List<Allergy> = emptyList(),
)
