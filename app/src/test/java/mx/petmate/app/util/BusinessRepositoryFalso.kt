package mx.petmate.app.util

import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.PublicBusiness
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.repository.BusinessRepository

/**
 * Doble reutilizable de [BusinessRepository] (001/T-023).
 *
 * El contrato tiene 17 metodos y una prueba de pantalla usa dos o tres. Los que no se configuran
 * **fallan a proposito** con [SIN_CONFIGURAR] en lugar de devolver una lista vacia o un exito
 * inventado: si una prueba llega a uno sin haberlo preparado, se entera en el momento en vez de
 * pasar en verde comprobando un comportamiento que nadie escribio.
 *
 * Cada llamada queda apuntada en [llamadas], y los argumentos de las que importan se guardan para
 * poder exigir **con que** se llamo, no solo que se llamo.
 */
class BusinessRepositoryFalso : BusinessRepository {

    val llamadas = mutableListOf<String>()

    // ---- Respuestas que la prueba prepara ----

    var cercanos: AppResult<List<NearbyBusiness>> = AppResult.Success(emptyList())
    var perfilPublico: AppResult<PublicBusiness> = sinConfigurar()
    var atendidas: AppResult<List<AttendedPet>> = AppResult.Success(emptyList())
    var comparticion: AppResult<AccessGrant> = sinConfigurar()
    var servicios: AppResult<List<BusinessService>> = AppResult.Success(emptyList())
    var negocioEnCache: OwnBusiness? = null
    var ficha: AppResult<ScanFicha> = sinConfigurar()
    var altaDeNegocio: AppResult<OwnBusiness> = sinConfigurar()
    var edicionDeNegocio: AppResult<OwnBusiness> = sinConfigurar()
    var cambioDeUbicacion: AppResult<OwnBusiness> = sinConfigurar()
    var bajaDeNegocio: AppResult<Unit> = sinConfigurar()
    var altaDeServicio: AppResult<BusinessService> = sinConfigurar()
    var edicionDeServicio: AppResult<BusinessService> = sinConfigurar()
    var bajaDeServicio: AppResult<Unit> = sinConfigurar()

    // ---- Argumentos con que se llamo ----

    var ultimoIdDePerfil: String? = null
        private set
    var ultimoPeriodo: String? = null
        private set
    var ultimaComparticion: Comparticion? = null
        private set
    var ultimasCoordenadas: Pair<Double, Double>? = null
        private set
    var ultimoCodigo: String? = null
        private set
    var ultimaAlta: Alta? = null
        private set
    var ultimoBorrador: BusinessDraft? = null
        private set
    var ultimaUbicacion: Pair<Double, Double>? = null
        private set

    /**
     * Cada servicio que se dio de alta, en orden.
     *
     * Es una lista y no un "ultimo": la afiliacion crea varios de un tiron y lo que hay que poder
     * exigir es **cuales** y en que orden, no solo el ultimo.
     */
    val serviciosCreados = mutableListOf<ServicioEnviado>()

    val serviciosEditados = mutableListOf<ServicioEnviado>()

    val serviciosBorrados = mutableListOf<String>()

    /** El negocio con que se llamo a `createBusiness`, con su pin. */
    data class Alta(val draft: BusinessDraft, val latitude: Double, val longitude: Double)

    /** Lo que se mando al crear o editar un servicio. [id] null = alta. */
    data class ServicioEnviado(
        val id: String?,
        val name: String,
        val description: String?,
        val price: Double?,
    )

    data class Comparticion(
        val petId: String,
        val businessId: String?,
        val data: Set<SharedData>,
        val duration: ShareDuration,
    )

    fun seLlamo(metodo: String): Boolean = metodo in llamadas

    fun vecesQueSeLlamo(metodo: String): Int = llamadas.count { it == metodo }

    // ---- Lectura publica ----

    override suspend fun getNearby(latitude: Double, longitude: Double): AppResult<List<NearbyBusiness>> {
        llamadas += "getNearby"
        ultimasCoordenadas = latitude to longitude
        return cercanos
    }

    override suspend fun getPublicBusiness(id: String): AppResult<PublicBusiness> {
        llamadas += "getPublicBusiness"
        ultimoIdDePerfil = id
        return perfilPublico
    }

    override fun cachedOwnBusiness(): OwnBusiness? {
        llamadas += "cachedOwnBusiness"
        return negocioEnCache
    }

    // ---- Compartir y escanear ----

    override suspend fun shareAccess(
        petId: String,
        businessId: String?,
        data: Set<SharedData>,
        duration: ShareDuration,
    ): AppResult<AccessGrant> {
        llamadas += "shareAccess"
        ultimaComparticion = Comparticion(petId, businessId, data, duration)
        return comparticion
    }

    override suspend fun resolveScan(code: String): AppResult<ScanFicha> {
        llamadas += "resolveScan"
        ultimoCodigo = code
        return ficha
    }

    override suspend fun getAttended(period: String): AppResult<List<AttendedPet>> {
        llamadas += "getAttended"
        ultimoPeriodo = period
        return atendidas
    }

    // ---- Servicios ----

    override suspend fun getServices(): AppResult<List<BusinessService>> {
        llamadas += "getServices"
        return servicios
    }

    override suspend fun createService(name: String, description: String?, price: Double?): AppResult<BusinessService> {
        llamadas += "createService"
        serviciosCreados += ServicioEnviado(null, name, description, price)
        return altaDeServicio
    }

    override suspend fun updateService(
        id: String,
        name: String,
        description: String?,
        price: Double?,
    ): AppResult<BusinessService> {
        llamadas += "updateService"
        serviciosEditados += ServicioEnviado(id, name, description, price)
        return edicionDeServicio
    }

    override suspend fun deleteService(id: String): AppResult<Unit> {
        llamadas += "deleteService"
        serviciosBorrados += id
        return bajaDeServicio
    }

    // ---- Negocio propio ----

    override suspend fun createBusiness(
        draft: BusinessDraft,
        latitude: Double,
        longitude: Double,
    ): AppResult<OwnBusiness> {
        llamadas += "createBusiness"
        ultimaAlta = Alta(draft, latitude, longitude)
        return altaDeNegocio
    }

    override suspend fun updateBusiness(draft: BusinessDraft): AppResult<OwnBusiness> {
        llamadas += "updateBusiness"
        ultimoBorrador = draft
        return edicionDeNegocio
    }

    override suspend fun updateLocation(latitude: Double, longitude: Double): AppResult<OwnBusiness> {
        llamadas += "updateLocation"
        ultimaUbicacion = latitude to longitude
        return cambioDeUbicacion
    }

    override suspend fun deleteBusiness(): AppResult<Unit> {
        llamadas += "deleteBusiness"
        return bajaDeNegocio
    }

    // ---- Borrador de afiliacion ----

    override suspend fun saveAffiliationDraft(draft: AffiliationDraft): AppResult<Unit> {
        llamadas += "saveAffiliationDraft"
        return AppResult.Success(Unit)
    }

    override suspend fun readAffiliationDraft(): AppResult<AffiliationDraft?> {
        llamadas += "readAffiliationDraft"
        return AppResult.Success(null)
    }

    override suspend fun clearAffiliationDraft(): AppResult<Unit> {
        llamadas += "clearAffiliationDraft"
        return AppResult.Success(Unit)
    }

    companion object {
        /** Lo que devuelve un metodo que la prueba no preparo: un fallo que se nota. */
        const val SIN_CONFIGURAR = "Este metodo del doble no se configuro en la prueba"

        private fun <T> sinConfigurar(): AppResult<T> =
            AppResult.Failure(AppError.Unknown(IllegalStateException(SIN_CONFIGURAR)))
    }
}
