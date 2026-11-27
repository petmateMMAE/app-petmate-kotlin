package mx.petmate.app.domain.health.usecase

import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyKind
import mx.petmate.app.domain.model.Carnet

/**
 * Alergias criticas de una mascota (spec 008, T-003, R-06 y C-06).
 *
 * No acepta filtros a proposito: las alergias activas se muestran y se comparten
 * siempre, sin importar que secciones eligio el dueno (R-06; riesgo G-02 de la
 * spec, que la 009 consume al compartir el historial).
 *
 * Las condiciones (`AllergyKind.Condition`) viven en su propia seccion del
 * carnet y no entran aqui: lo critico son las alergias.
 */
class GetCriticalAllergiesUseCase {

    /** Alergias activas del carnet completo. */
    operator fun invoke(carnet: Carnet): List<Allergy> = invoke(carnet.allergies)

    /** Alergias activas de una lista ya cargada. */
    operator fun invoke(allergies: List<Allergy>): List<Allergy> =
        allergies.filter { it.kind == AllergyKind.Allergy && it.active }
}
