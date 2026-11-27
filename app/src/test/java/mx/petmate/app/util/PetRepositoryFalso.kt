package mx.petmate.app.util

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.model.WeightRecord
import mx.petmate.app.domain.repository.PetRepository

/**
 * Doble reutilizable de [PetRepository] (001/T-023).
 *
 * Mismo criterio que [BusinessRepositoryFalso]: lo que la prueba no configura **falla a
 * proposito**, para que nadie pase en verde apoyandose en un valor por omision que nadie penso.
 *
 * [carnetPorMascota] permite dar un carnet distinto a cada mascota, que es lo que hace falta para
 * probar la lista de mascotas: cada etiqueta ("Vacuna en 3 dias", "1 alergia") sale del carnet de
 * esa mascota en particular.
 */
class PetRepositoryFalso : PetRepository {

    val llamadas = mutableListOf<String>()

    // ---- Respuestas que la prueba prepara ----

    var mascotas: AppResult<List<Pet>> = AppResult.Success(emptyList())
    var recarga: AppResult<Unit> = AppResult.Success(Unit)
    var carnetPorOmision: AppResult<Carnet> = AppResult.Success(Carnet())
    val carnetPorMascota = mutableMapOf<String, AppResult<Carnet>>()

    var alta: AppResult<Pet> = AppResult.Success(mascotaFalsa())
    var edicion: AppResult<Pet> = AppResult.Success(mascotaFalsa())
    var borrado: AppResult<Unit> = AppResult.Success(Unit)

    var qr: AppResult<PetQr> = sinConfigurar()
    var qrRegenerado: AppResult<PetQr> = sinConfigurar()
    var altaDeVacuna: AppResult<Vaccine> = sinConfigurar()
    var bajaDeVacuna: AppResult<Unit> = sinConfigurar()
    var altaDeDesparasitacion: AppResult<Deworming> = sinConfigurar()
    var bajaDeDesparasitacion: AppResult<Unit> = sinConfigurar()
    var altaDePeso: AppResult<WeightRecord> = sinConfigurar()
    var altaDeAlergia: AppResult<Allergy> = sinConfigurar()
    var edicionDeAlergia: AppResult<Allergy> = sinConfigurar()

    /** El borrador con que se llamo a `createPet` o `updatePet`, para exigir que viaje completo. */
    var ultimoBorrador: PetDraft? = null
        private set
    var ultimoIdEditado: String? = null
        private set
    var ultimoIdBorrado: String? = null
        private set

    // ---- Lo que se mando al carnet ----

    var ultimaVacuna: VaccineDraft? = null
        private set
    var ultimaDesparasitacion: DewormingDraft? = null
        private set

    /** Fecha y kilos con que se llamo a `addWeight`: el redondeo se comprueba aqui. */
    var ultimoPeso: Pair<String, Double>? = null
        private set
    var ultimaAlergia: AllergyDraft? = null
        private set

    /** Id de la alergia que se edito; null cuando fue un alta. */
    var ultimoIdDeAlergia: String? = null
        private set
    val vacunasBorradas = mutableListOf<String>()
    val desparasitacionesBorradas = mutableListOf<String>()

    fun seLlamo(metodo: String): Boolean = metodo in llamadas

    fun vecesQueSeLlamo(metodo: String): Int = llamadas.count { it == metodo }

    // ---- Lectura ----

    override suspend fun getPets(): AppResult<List<Pet>> {
        llamadas += "getPets"
        return mascotas
    }

    override suspend fun refreshPets(): AppResult<Unit> {
        llamadas += "refreshPets"
        return recarga
    }

    override suspend fun getPet(id: String): AppResult<Pet> {
        llamadas += "getPet"
        val encontrada = (mascotas as? AppResult.Success)?.data?.firstOrNull { it.id == id }
        return encontrada?.let { AppResult.Success(it) } ?: sinConfigurar()
    }

    override suspend fun getCarnet(petId: String): AppResult<Carnet> {
        llamadas += "getCarnet"
        return carnetPorMascota[petId] ?: carnetPorOmision
    }

    override suspend fun refreshCarnet(petId: String): AppResult<Unit> {
        llamadas += "refreshCarnet"
        return recarga
    }

    // ---- Escritura de la mascota ----

    override suspend fun createPet(draft: PetDraft): AppResult<Pet> {
        llamadas += "createPet"
        ultimoBorrador = draft
        return alta
    }

    override suspend fun updatePet(id: String, draft: PetDraft): AppResult<Pet> {
        llamadas += "updatePet"
        ultimoBorrador = draft
        ultimoIdEditado = id
        return edicion
    }

    override suspend fun deletePet(id: String): AppResult<Unit> {
        llamadas += "deletePet"
        ultimoIdBorrado = id
        return borrado
    }

    // ---- QR ----

    override suspend fun getQr(petId: String): AppResult<PetQr> {
        llamadas += "getQr"
        return qr
    }

    override suspend fun regenerateQr(petId: String): AppResult<PetQr> {
        llamadas += "regenerateQr"
        return qrRegenerado
    }

    // ---- Carnet: escritura ----

    override suspend fun addVaccine(petId: String, draft: VaccineDraft): AppResult<Vaccine> {
        llamadas += "addVaccine"
        ultimaVacuna = draft
        return altaDeVacuna
    }

    override suspend fun deleteVaccine(id: String): AppResult<Unit> {
        llamadas += "deleteVaccine"
        vacunasBorradas += id
        return bajaDeVacuna
    }

    override suspend fun addDeworming(petId: String, draft: DewormingDraft): AppResult<Deworming> {
        llamadas += "addDeworming"
        ultimaDesparasitacion = draft
        return altaDeDesparasitacion
    }

    override suspend fun deleteDeworming(id: String): AppResult<Unit> {
        llamadas += "deleteDeworming"
        desparasitacionesBorradas += id
        return bajaDeDesparasitacion
    }

    override suspend fun addWeight(petId: String, measuredOn: String, kilograms: Double): AppResult<WeightRecord> {
        llamadas += "addWeight"
        ultimoPeso = measuredOn to kilograms
        return altaDePeso
    }

    override suspend fun addAllergy(petId: String, draft: AllergyDraft): AppResult<Allergy> {
        llamadas += "addAllergy"
        ultimaAlergia = draft
        ultimoIdDeAlergia = null
        return altaDeAlergia
    }

    override suspend fun updateAllergy(id: String, draft: AllergyDraft): AppResult<Allergy> {
        llamadas += "updateAllergy"
        ultimaAlergia = draft
        ultimoIdDeAlergia = id
        return edicionDeAlergia
    }

    companion object {
        const val SIN_CONFIGURAR = "Este metodo del doble no se configuro en la prueba"

        private fun <T> sinConfigurar(): AppResult<T> =
            AppResult.Failure(AppError.Unknown(IllegalStateException(SIN_CONFIGURAR)))
    }
}
