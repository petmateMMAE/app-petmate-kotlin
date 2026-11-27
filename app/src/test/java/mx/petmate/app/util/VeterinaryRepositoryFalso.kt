package mx.petmate.app.util

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.Consultation
import mx.petmate.app.domain.model.ConsultationDraft
import mx.petmate.app.domain.model.ConsultationSummary
import mx.petmate.app.domain.model.Deworming
import mx.petmate.app.domain.model.DewormingType
import mx.petmate.app.domain.model.Followup
import mx.petmate.app.domain.model.HistoryItem
import mx.petmate.app.domain.model.MedicationDraft
import mx.petmate.app.domain.model.Vaccine
import mx.petmate.app.domain.repository.VeterinaryRepository

/**
 * Doble reutilizable de [VeterinaryRepository] (001/T-023).
 *
 * Mismo criterio que los otros dobles: lo que la prueba no prepara **falla a proposito** con
 * [SIN_CONFIGURAR], y cada llamada queda apuntada en [llamadas] con sus argumentos. Un doble que
 * devuelve historial vacio por omision deja pasar en verde una pantalla que nunca pidio nada.
 */
class VeterinaryRepositoryFalso : VeterinaryRepository {

    val llamadas = mutableListOf<String>()

    // ---- Respuestas que la prueba prepara ----

    var historial: AppResult<List<HistoryItem>> = sinConfigurar()
    var consulta: AppResult<Consultation> = sinConfigurar()
    var consultas: AppResult<List<ConsultationSummary>> = sinConfigurar()
    var seguimientoDeConsulta: AppResult<Followup?> = AppResult.Success(null)
    var seguimientos: AppResult<List<Followup>> = sinConfigurar()
    var altaDeConsulta: AppResult<Consultation> = sinConfigurar()
    var altaDeMedicamento: AppResult<Consultation> = sinConfigurar()
    var altaDeVacuna: AppResult<Vaccine> = sinConfigurar()
    var altaDeDesparasitacion: AppResult<Deworming> = sinConfigurar()
    var altaDeSeguimiento: AppResult<Followup> = sinConfigurar()
    var cambioDeSeguimiento: AppResult<Followup> = sinConfigurar()
    var notaDeSeguimiento: AppResult<Followup> = sinConfigurar()

    // ---- Argumentos con que se llamo ----

    var ultimoIdDeMascota: String? = null
        private set
    var ultimosTipos: String? = null
        private set
    var ultimoIdDeConsulta: String? = null
        private set
    var ultimoBorrador: ConsultationDraft? = null
        private set
    var ultimaNota: String? = null
        private set
    var ultimoCambioDeSeguimiento: CambioDeSeguimiento? = null
        private set

    data class CambioDeSeguimiento(val id: String, val status: String, val nextReviewInDays: Int?)

    fun seLlamo(metodo: String): Boolean = metodo in llamadas

    fun vecesQueSeLlamo(metodo: String): Int = llamadas.count { it == metodo }

    // ---- Consultas ----

    override suspend fun createConsultation(draft: ConsultationDraft): AppResult<Consultation> {
        llamadas += "createConsultation"
        ultimoBorrador = draft
        return altaDeConsulta
    }

    override suspend fun addMedication(consultationId: String, medication: MedicationDraft): AppResult<Consultation> {
        llamadas += "addMedication"
        ultimoIdDeConsulta = consultationId
        return altaDeMedicamento
    }

    override suspend fun getConsultations(petId: String): AppResult<List<ConsultationSummary>> {
        llamadas += "getConsultations"
        ultimoIdDeMascota = petId
        return consultas
    }

    override suspend fun getConsultationById(id: String): AppResult<Consultation> {
        llamadas += "getConsultationById"
        ultimoIdDeConsulta = id
        return consulta
    }

    // ---- Lo que registra el negocio en el carnet ----

    override suspend fun createVaccineByBusiness(
        petId: String,
        name: String,
        appliedOn: String,
        batch: String?,
        nextDoseOn: String?,
    ): AppResult<Vaccine> {
        llamadas += "createVaccineByBusiness"
        ultimoIdDeMascota = petId
        return altaDeVacuna
    }

    override suspend fun createDewormingByBusiness(
        petId: String,
        product: String,
        appliedOn: String,
        type: DewormingType?,
        nextOn: String?,
    ): AppResult<Deworming> {
        llamadas += "createDewormingByBusiness"
        ultimoIdDeMascota = petId
        return altaDeDesparasitacion
    }

    // ---- Historial unificado ----

    override suspend fun getHistory(petId: String, types: String?): AppResult<List<HistoryItem>> {
        llamadas += "getHistory"
        ultimoIdDeMascota = petId
        ultimosTipos = types
        return historial
    }

    // ---- Seguimiento ----

    override suspend fun createFollowup(consultationId: String, nextReviewInDays: Int?): AppResult<Followup> {
        llamadas += "createFollowup"
        ultimoIdDeConsulta = consultationId
        return altaDeSeguimiento
    }

    override suspend fun updateFollowup(id: String, status: String, nextReviewInDays: Int?): AppResult<Followup> {
        llamadas += "updateFollowup"
        ultimoCambioDeSeguimiento = CambioDeSeguimiento(id, status, nextReviewInDays)
        return cambioDeSeguimiento
    }

    override suspend fun addFollowupNote(followupId: String, text: String): AppResult<Followup> {
        llamadas += "addFollowupNote"
        ultimaNota = text
        return notaDeSeguimiento
    }

    override suspend fun getFollowupByConsultation(consultationId: String): AppResult<Followup?> {
        llamadas += "getFollowupByConsultation"
        ultimoIdDeConsulta = consultationId
        return seguimientoDeConsulta
    }

    override suspend fun getFollowups(onlyActive: Boolean): AppResult<List<Followup>> {
        llamadas += "getFollowups"
        return seguimientos
    }

    companion object {
        const val SIN_CONFIGURAR = "Este metodo del doble no se configuro en la prueba"

        private fun <T> sinConfigurar(): AppResult<T> =
            AppResult.Failure(AppError.Unknown(IllegalStateException(SIN_CONFIGURAR)))
    }
}
