package mx.petmate.app.domain.sharing

import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.SharedData

/**
 * Secciones que el dueno puede elegir al compartir el historial (spec 009, T-002, R-04).
 *
 * Las alergias **no** estan aqui a proposito: no son elegibles porque siempre
 * van (R-04 y 008/R-06). El riesgo G-02 de la spec 008 es justo que una alergia
 * se pierda porque el dueno limito las secciones, y la forma de evitarlo es que
 * no se puedan desmarcar: ni siquiera aparecen como opcion.
 */
enum class ShareSection(val data: SharedData) {
    Vaccines(SharedData.Vaccines),
    Dewormings(SharedData.Dewormings),
    Weights(SharedData.Weights),
    Consultations(SharedData.Consultations),
    ;

    companion object {
        /** Las cuatro secciones elegibles, en el orden en que las pinta la pantalla. */
        val Selectable: List<ShareSection> = entries
    }
}

/**
 * Lo que el dueno decidio compartir, mas las alergias que van siempre
 * (spec 009, T-002, R-04 y C-03).
 *
 * [ShareSelection.sections] son solo las elegibles. Para armar lo que se manda
 * al servidor se usa [ShareSelection.toSharedData], que agrega las alergias
 * aunque la seleccion venga vacia.
 *
 * Las condiciones viajan con las alergias: en el carnet son el mismo registro
 * con `kind = Condition` (008), y el backend las manda en el mismo campo.
 */
data class ShareSelection(val sections: Set<ShareSection> = emptySet()) {

    val isEmpty: Boolean get() = sections.isEmpty()

    fun toggle(section: ShareSection): ShareSelection =
        copy(sections = if (section in sections) sections - section else sections + section)

    /**
     * Lo que se le manda al backend.
     *
     * Las alergias no estan en [SharedData] porque el backend las incluye
     * siempre (pm-123): no hay nada que pedirle. Esta funcion existe para que
     * quede escrito en un solo lugar que la seleccion del usuario **nunca**
     * decide sobre las alergias.
     */
    fun toSharedData(): Set<SharedData> = sections.map(ShareSection::data).toSet()

    companion object {
        val Empty = ShareSelection()

        /** Todo lo elegible marcado. */
        val All = ShareSelection(ShareSection.Selectable.toSet())
    }
}

/**
 * Arma el contenido que se comparte: lo elegido mas las alergias criticas
 * (spec 009, T-002, C-03).
 *
 * Recibe las alergias ya filtradas por `GetCriticalAllergiesUseCase` (008/T-003)
 * en lugar de volver a filtrarlas, para que la regla de "cuales son criticas"
 * siga viviendo en un solo lugar.
 */
object ShareSections {

    /**
     * Alergias que acompanan a cualquier comparticion.
     *
     * Son las activas, y van aunque [selection] este vacia: esa es la prueba
     * C-03 de la spec.
     */
    fun allergiesFor(criticalAllergies: List<Allergy>): List<Allergy> = criticalAllergies

    /** Si [section] se puede desmarcar. Las alergias nunca llegan aqui: no son una seccion elegible. */
    fun isSelectable(section: ShareSection): Boolean = section in ShareSection.Selectable
}

/**
 * Contenido final de una comparticion (spec 009, T-002).
 *
 * Se construye con [of] para que no haya forma de armarlo sin las alergias.
 */
data class ShareContent(
    val selection: ShareSelection,
    val allergies: List<Allergy>,
) {
    /** Lo que viaja al backend, con las alergias implicitas. */
    fun toSharedData(): Set<SharedData> = selection.toSharedData()

    /** Si no se comparte nada mas que las alergias. */
    val isOnlyAllergies: Boolean get() = selection.isEmpty

    companion object {
        /**
         * Arma el contenido a partir de lo que eligio el dueno y del carnet.
         *
         * [criticalAllergies] sale de `GetCriticalAllergiesUseCase`: aqui no se
         * vuelve a decidir que es critico.
         */
        fun of(selection: ShareSelection, criticalAllergies: List<Allergy>) =
            ShareContent(selection = selection, allergies = criticalAllergies)
    }
}
