package mx.petmate.app.domain.model

/**
 * Estados del seguimiento (spec 015, T-001, R-02). [apiValue] es el texto que manda y recibe el backend.
 *
 * El modelo `Followup` ya existia (unidad 014, `Veterinary.kt`) con el estado como texto; no se duplica: se
 * agrega este enum y [Followup.statusKind] para trabajar con estados tipados.
 */
enum class FollowUpStatus(val apiValue: String) {
    Treating("Treating"),
    Improving("Improving"),
    Discharged("Discharged"),
    ;

    companion object {
        /** Estado tipado de un texto del backend, o `null` si el backend manda uno que la app no conoce. */
        fun fromApi(value: String): FollowUpStatus? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}

/** Estado tipado de este seguimiento. */
val Followup.statusKind: FollowUpStatus?
    get() = FollowUpStatus.fromApi(status)
