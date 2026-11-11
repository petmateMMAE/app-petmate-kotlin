package mx.petmate.app.domain.repository

import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AccessGrant
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.PublicBusiness
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData

/** Mapa publico, negocio propio, catalogo, autorizaciones y escaneo (backend 0.4.0). */
interface BusinessRepository {
    // Mapa publico: funciona sin cuenta.
    suspend fun getNearby(latitude: Double, longitude: Double): AppResult<List<NearbyBusiness>>
    suspend fun getPublicBusiness(id: String): AppResult<PublicBusiness>

    // Negocio propio.
    /** Copia local del negocio propio (el backend no tiene "ver mi negocio"); null si no se conoce. */
    fun cachedOwnBusiness(): OwnBusiness?
    suspend fun createBusiness(draft: BusinessDraft, latitude: Double, longitude: Double): AppResult<OwnBusiness>
    suspend fun updateBusiness(draft: BusinessDraft): AppResult<OwnBusiness>
    suspend fun updateLocation(latitude: Double, longitude: Double): AppResult<OwnBusiness>
    suspend fun deleteBusiness(): AppResult<Unit>

    // Catalogo.
    suspend fun getServices(): AppResult<List<BusinessService>>
    suspend fun createService(name: String, description: String?, price: Double?): AppResult<BusinessService>
    suspend fun updateService(id: String, name: String, description: String?, price: Double?): AppResult<BusinessService>
    suspend fun deleteService(id: String): AppResult<Unit>

    // Dueno: autoriza a un negocio (o a cualquiera que escanee) por un tiempo.
    suspend fun shareAccess(petId: String, businessId: String?, data: Set<SharedData>, duration: ShareDuration): AppResult<AccessGrant>

    // Negocio: escanea el QR o escribe el codigo corto.
    suspend fun resolveScan(code: String): AppResult<ScanFicha>
    suspend fun getAttended(period: String): AppResult<List<AttendedPet>>
}
