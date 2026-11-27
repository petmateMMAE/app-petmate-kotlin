package mx.petmate.app.domain.health

import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.RecordOrigin

/**
 * Un registro del carnet, ya sin importar de que seccion viene (spec 008, T-006, R-08).
 *
 * [date] es `yyyy-MM-dd`, o null cuando el registro no tiene fecha: el contrato
 * del backend (pm-096 a pm-103) no le da fecha a las alergias ni a las
 * condiciones. Esos se muestran siempre y no se ordenan por fecha (R-06).
 */
data class HealthEvent(
    val id: String,
    val type: HealthEventType,
    val date: String?,
    val title: String,
    val origin: RecordOrigin,
)

/**
 * Une las seis secciones del carnet en una sola lista ordenada (spec 008, T-006, R-08 y R-09).
 *
 * Orden: lo mas reciente primero; a igual fecha, por id, para que el orden no
 * cambie entre dos lecturas (C-03). Los registros sin fecha (alergias y
 * condiciones) van al principio, porque son los que siempre deben verse.
 *
 * Es logica pura: no toca red ni base, y el carnet que recibe ya salio de Room
 * (C-02). El formato local de la fecha lo pone la interfaz.
 */
object HealthTimeline {

    /**
     * Linea de tiempo del carnet.
     *
     * [consultations] son el historial medico (spec 014); el `Carnet` local no
     * las trae todavia, asi que por omision va vacio y la linea de tiempo
     * funciona igual con las cinco secciones que si estan guardadas.
     */
    fun of(
        carnet: Carnet,
        consultations: List<ConsultationSummary> = emptyList(),
        filter: HealthFilter = HealthFilter.All,
    ): List<HealthEvent> = events(carnet, consultations)
        .filter(filter::accepts)
        .sortedWith(ORDER)

    /** Todos los eventos, sin filtrar ni ordenar. */
    private fun events(carnet: Carnet, consultations: List<ConsultationSummary>): List<HealthEvent> = buildList {
        carnet.vaccines.forEach {
            add(HealthEvent(it.id, HealthEventType.Vaccine, it.appliedOn, it.name, it.origin))
        }
        carnet.dewormings.forEach {
            add(HealthEvent(it.id, HealthEventType.Deworming, it.appliedOn, it.product, it.origin))
        }
        carnet.weights.forEach {
            add(HealthEvent(it.id, HealthEventType.Weight, it.measuredOn, "${it.kilograms} kg", it.origin))
        }
        carnet.allergies.forEach {
            val type = when (it.kind) {
                AllergyKind.Allergy -> HealthEventType.Allergy
                AllergyKind.Condition -> HealthEventType.Condition
            }
            add(HealthEvent(it.id, type, null, it.name, it.origin))
        }
        consultations.forEach {
            add(
                HealthEvent(
                    id = it.id,
                    type = HealthEventType.Consultation,
                    date = it.consultedOn,
                    title = it.reason,
                    origin = RecordOrigin(byBusiness = true, businessName = it.businessName),
                ),
            )
        }
    }

    /** Lo mismo, partiendo del historial unificado que ya devuelve el backend (pm-142). */
    fun of(history: List<HistoryItem>, filter: HealthFilter = HealthFilter.All): List<HealthEvent> =
        history.mapNotNull { item ->
            HealthEventType.fromApi(item.kind)?.let {
                HealthEvent(item.id, it, item.date, item.title, item.origin)
            }
        }.filter(filter::accepts).sortedWith(ORDER)

    /** Sin fecha primero; luego la fecha mas reciente; a igual fecha, por id. */
    private val ORDER: Comparator<HealthEvent> =
        compareBy<HealthEvent> { it.date != null }
            .thenByDescending { it.date.orEmpty() }
            .thenBy { it.id }
}
