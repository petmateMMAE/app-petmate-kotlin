package mx.petmate.app.ui.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import mx.petmate.app.domain.model.SessionState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessDraft
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.PublicBusiness
import mx.petmate.app.domain.model.SCHEDULE_DAYS
import mx.petmate.app.domain.model.ScanFicha
import mx.petmate.app.domain.model.ShareDuration
import mx.petmate.app.domain.model.SharedData
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.repository.AuthRepository
import mx.petmate.app.domain.repository.BusinessRepository
import mx.petmate.app.ui.map.MapViewModel
import javax.inject.Inject

/** Mensaje para el usuario segun el error del backend (docs/api/endpoints.md). */
fun AppError.businessMessage(): String = when (this) {
    AppError.Network -> "Sin conexión. Revisa tu internet e intenta de nuevo."
    AppError.Timeout -> "El servidor tardó en responder. Intenta de nuevo."
    AppError.Unauthorized -> "Tu sesión expiró. Vuelve a iniciar sesión."
    is AppError.Validation -> "Revisa el campo $field."
    is AppError.Server -> when (code) {
        400 -> "Revisa los datos: alguno no es válido."
        403 -> "Tu cuenta no tiene permiso para esto."
        404 -> "No lo encontramos. Puede que ya no exista."
        409 -> "Tu cuenta ya tiene un negocio afiliado."
        413 -> "Los datos son demasiado grandes."
        429 -> "Demasiados intentos. Espera unos minutos."
        else -> "Algo salió mal en el servidor ($code). Intenta más tarde."
    }
    is AppError.Unknown -> "Algo salió mal. Intenta de nuevo."
}

private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
private val PHONE = Regex("^(\\+?52)?\\d{10}$")

/** Valida telefono de 10 digitos (se admite +52, pm-107). */
fun isValidPhone(value: String): Boolean = PHONE.matches(value.filter { !it.isWhitespace() && it != '-' })

/** Formulario de datos del negocio: el mismo para afiliar y para editar. */
data class BusinessForm(
    val kind: BusinessKind? = null,
    val name: String = "",
    val phone: String = "",
    val whatsApp: String = "",
    val address: String = "",
    val description: String = "",
    val licenseNumber: String = "",
    val days: Set<String> = setOf("monday", "tuesday", "wednesday", "thursday", "friday"),
    val open: String = "09:00",
    val close: String = "18:00",
    val errors: Map<String, String> = emptyMap(),
) {
    fun schedule(): Map<String, List<TimeRange>> = days.associateWith { listOf(TimeRange(open, close)) }

    fun toDraft() = BusinessDraft(kind, name, cleanPhone(phone), cleanPhone(whatsApp), address, description, licenseNumber, schedule())

    /** Errores por campo; vacio si todo esta bien. [editing] = el tipo y la direccion no se editan. */
    fun validate(editing: Boolean = false): Map<String, String> = buildMap {
        if (!editing && kind == null) put("kind", "Elige qué ofreces.")
        if (name.trim().length < 2) put("name", "Escribe el nombre del negocio.")
        if (!isValidPhone(phone)) put("phone", "Debe tener 10 dígitos.")
        if (whatsApp.isNotBlank() && !isValidPhone(whatsApp)) put("whatsApp", "Debe tener 10 dígitos o quedar vacío.")
        if (!editing && address.trim().length < 5) put("address", "Escribe calle, número y colonia.")
        if (!editing && kind == BusinessKind.Veterinary && licenseNumber.isBlank()) put("license", "La cédula es obligatoria para veterinarias.")
        if (days.isNotEmpty()) {
            when {
                !TIME.matches(open) || !TIME.matches(close) -> put("hours", "Usa el formato 24 h, por ejemplo 09:00.")
                close <= open -> put("hours", "La hora de cierre debe ser después de la de apertura.")
            }
        }
    }

    companion object {
        fun cleanPhone(value: String) = value.filter { it.isDigit() || it == '+' }

        fun from(b: OwnBusiness): BusinessForm {
            val withHours = b.schedule.filterValues { it.isNotEmpty() }
            val first = withHours.values.firstOrNull()?.firstOrNull()
            return BusinessForm(
                kind = b.kind,
                name = b.name,
                phone = b.phone,
                whatsApp = b.whatsApp.orEmpty(),
                address = b.address,
                description = b.description.orEmpty(),
                licenseNumber = b.licenseNumber.orEmpty(),
                days = withHours.keys.toSet(),
                open = first?.open ?: "09:00",
                close = first?.close ?: "18:00",
            )
        }
    }
}

/** Servicio sugerido o agregado durante la afiliacion. */
data class DraftService(val name: String, val price: String, val selected: Boolean)

fun suggestedServices(kind: BusinessKind?): List<DraftService> = when (kind) {
    BusinessKind.Veterinary -> listOf(DraftService("Consulta general", "350", true), DraftService("Vacunación", "250", false), DraftService("Desparasitación", "150", false))
    BusinessKind.Store -> listOf(DraftService("Alimento", "", true), DraftService("Accesorios", "", false), DraftService("Juguetes", "", false))
    BusinessKind.Grooming -> listOf(DraftService("Baño", "250", true), DraftService("Corte de pelo", "300", false), DraftService("Corte de uñas", "80", false))
    null -> emptyList()
}

fun parsePrice(text: String): Double? = text.replace(",", "").filter { it.isDigit() || it == '.' }.toDoubleOrNull()

// ---------- Afiliar negocio (affiliate, affServices, affPin) ----------

data class AffiliateUiState(
    val step: Int = 1,
    val form: BusinessForm = BusinessForm(),
    val services: List<DraftService> = emptyList(),
    val newServiceName: String = "",
    val newServicePrice: String = "",
    val servicesError: String? = null,
    val latitude: Double = MapViewModel.DEFAULT_CENTER.first,
    val longitude: Double = MapViewModel.DEFAULT_CENTER.second,
    val saving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class AffiliateViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AffiliateUiState())
    val state: StateFlow<AffiliateUiState> = _state.asStateFlow()

    fun onForm(change: (BusinessForm) -> BusinessForm) = _state.update { it.copy(form = change(it.form).copy(errors = emptyMap()), error = null) }

    fun onKind(kind: BusinessKind) = _state.update {
        it.copy(form = it.form.copy(kind = kind, errors = emptyMap()), services = suggestedServices(kind))
    }

    fun toggleDay(day: String) = onForm { f -> f.copy(days = if (day in f.days) f.days - day else f.days + day) }

    fun toggleService(index: Int) = _state.update {
        it.copy(services = it.services.mapIndexed { i, s -> if (i == index) s.copy(selected = !s.selected) else s }, servicesError = null)
    }

    fun onServicePrice(index: Int, price: String) = _state.update {
        it.copy(services = it.services.mapIndexed { i, s -> if (i == index) s.copy(price = price) else s })
    }

    fun onNewService(name: String? = null, price: String? = null) = _state.update {
        it.copy(newServiceName = name ?: it.newServiceName, newServicePrice = price ?: it.newServicePrice)
    }

    fun addService() = _state.update {
        if (it.newServiceName.isBlank()) it else it.copy(
            services = it.services + DraftService(it.newServiceName.trim(), it.newServicePrice, true),
            newServiceName = "",
            newServicePrice = "",
            servicesError = null,
        )
    }

    fun onLocation(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) _state.update { it.copy(latitude = latitude, longitude = longitude) }
    }

    fun next() = _state.update { s ->
        when (s.step) {
            1 -> s.form.validate().let { errors -> if (errors.isEmpty()) s.copy(step = 2) else s.copy(form = s.form.copy(errors = errors)) }
            2 -> if (s.services.none { it.selected }) s.copy(servicesError = "Elige al menos un servicio.") else s.copy(step = 3)
            else -> s
        }
    }

    /** Regresa un paso; false si ya esta en el primero. */
    fun back(): Boolean {
        val step = _state.value.step
        if (step == 1) return false
        _state.update { it.copy(step = step - 1, error = null) }
        return true
    }

    fun finish(latitude: Double, longitude: Double) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null, latitude = latitude, longitude = longitude) }
        viewModelScope.launch {
            when (val created = repository.createBusiness(s.form.toDraft(), latitude, longitude)) {
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = created.error.businessMessage()) }
                is AppResult.Success -> {
                    s.services.filter { it.selected }.forEach { repository.createService(it.name, null, parsePrice(it.price)) }
                    // La sesion trae el tipo de negocio hasta renovarla (pm-107).
                    auth.refreshSession()
                    _state.update { it.copy(saving = false, done = true) }
                }
            }
        }
    }

    /** Despues de afiliar, entra directo al modo negocio. */
    fun enterBusinessMode() = auth.setBusinessMode(true)
}

// ---------- Mi negocio: panel, edicion, servicios ----------

data class MyBusinessUiState(
    val business: OwnBusiness? = null,
    val kind: BusinessKind? = null,
    val form: BusinessForm = BusinessForm(),
    val services: List<BusinessService> = emptyList(),
    val loadingServices: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class MyBusinessViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyBusinessUiState())
    val state: StateFlow<MyBusinessUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        val cached = repository.cachedOwnBusiness()
        viewModelScope.launch {
            val kind = cached?.kind ?: (auth.sessionState.first() as? SessionState.SignedIn)?.user?.businessType.let(BusinessKind::fromApi)
            _state.update {
                it.copy(business = cached, kind = kind, form = cached?.let(BusinessForm::from) ?: it.form.copy(kind = kind))
            }
            loadServices()
        }
    }

    fun loadServices() {
        _state.update { it.copy(loadingServices = true) }
        viewModelScope.launch {
            when (val r = repository.getServices()) {
                is AppResult.Success -> _state.update { it.copy(services = r.data, loadingServices = false) }
                is AppResult.Failure -> _state.update { it.copy(loadingServices = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun onForm(change: (BusinessForm) -> BusinessForm) = _state.update { it.copy(form = change(it.form).copy(errors = emptyMap()), error = null, message = null) }

    fun toggleDay(day: String) = onForm { f -> f.copy(days = if (day in f.days) f.days - day else f.days + day) }

    fun save(onSaved: () -> Unit) {
        val form = _state.value.form
        val errors = form.validate(editing = true)
        if (errors.isNotEmpty()) return _state.update { it.copy(form = form.copy(errors = errors)) }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.updateBusiness(form.toDraft())) {
                is AppResult.Success -> {
                    _state.update { it.copy(saving = false, business = r.data, message = "Cambios guardados") }
                    onSaved()
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun saveLocation(latitude: Double, longitude: Double, onSaved: () -> Unit) {
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.updateLocation(latitude, longitude)) {
                is AppResult.Success -> {
                    _state.update { it.copy(saving = false, business = r.data) }
                    onSaved()
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun delete() {
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.deleteBusiness()) {
                is AppResult.Success -> {
                    auth.setBusinessMode(false)
                    auth.refreshSession()
                    _state.update { it.copy(saving = false, deleted = true) }
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun saveService(id: String?, name: String, description: String, price: String, onSaved: () -> Unit) {
        if (name.isBlank()) return _state.update { it.copy(error = "Escribe el nombre del servicio.") }
        val parsed = price.takeIf { it.isNotBlank() }?.let { parsePrice(it) ?: return _state.update { s -> s.copy(error = "El precio debe ser un número, por ejemplo 300.") } }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val r = if (id == null) repository.createService(name, description, parsed) else repository.updateService(id, name, description, parsed)
            when (r) {
                is AppResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    loadServices()
                    onSaved()
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun deleteService(id: String) {
        viewModelScope.launch {
            when (val r = repository.deleteService(id)) {
                is AppResult.Success -> loadServices()
                is AppResult.Failure -> _state.update { it.copy(error = r.error.businessMessage()) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun switchToOwner() = auth.setBusinessMode(false)
}

// ---------- Escaneo y ficha ----------

data class ScanUiState(
    val code: String = "",
    val resolving: Boolean = false,
    val error: String? = null,
    val ficha: ScanFicha? = null,
    val isVet: Boolean = false,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: BusinessRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun onCode(code: String) = _state.update { it.copy(code = code.take(64), error = null) }

    /** Acepta el contenido del QR (token o URL que termina en el token) o el codigo corto. */
    fun resolve(raw: String = _state.value.code) {
        // El QR trae el token de 43 caracteres (base64url, puede llevar - y _); el codigo corto se escribe a mano.
        val trimmed = raw.trim().substringAfterLast('/')
        val code = if (trimmed.length == 43) trimmed else trimmed.filter { it.isLetterOrDigit() }.uppercase()
        if (code.length != 8 && code.length != 43) return _state.update { it.copy(error = "El código tiene 8 caracteres (o escanea el QR).") }
        _state.update { it.copy(resolving = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.resolveScan(code)) {
                is AppResult.Success -> _state.update {
                    it.copy(resolving = false, ficha = r.data, code = "", isVet = r.data.vaccines != null || r.data.consultations != null || r.data.petId != null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(
                        resolving = false,
                        error = when ((r.error as? AppError.Server)?.code) {
                            403 -> "El dueño no ha compartido el historial con tu negocio, o ya venció. Pídele que genere el QR para compartir."
                            404 -> "No encontramos ese código. Revisa que esté bien escrito o que el QR no se haya cambiado."
                            else -> r.error.businessMessage()
                        },
                    )
                }
            }
        }
    }

    fun closeFicha() = _state.update { ScanUiState() }
}

data class AttendedUiState(val period: String = "day", val loading: Boolean = true, val items: List<AttendedPet> = emptyList(), val error: String? = null)

@HiltViewModel
class AttendedViewModel @Inject constructor(private val repository: BusinessRepository) : ViewModel() {
    private val _state = MutableStateFlow(AttendedUiState())
    val state: StateFlow<AttendedUiState> = _state.asStateFlow()

    init {
        load("day")
    }

    fun load(period: String) {
        _state.update { it.copy(period = period, loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.getAttended(period)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, items = r.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.businessMessage()) }
            }
        }
    }
}

// ---------- Perfil publico del negocio (bizProfile) ----------

data class BizProfileUiState(val loading: Boolean = true, val business: PublicBusiness? = null, val error: String? = null)

@HiltViewModel
class BizProfileViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: BusinessRepository,
) : ViewModel() {
    private val id: String = checkNotNull(savedState[ARG_BUSINESS_ID])
    private val _state = MutableStateFlow(BizProfileUiState())
    val state: StateFlow<BizProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.getPublicBusiness(id)) {
                is AppResult.Success -> _state.update { it.copy(loading = false, business = r.data) }
                is AppResult.Failure -> _state.update { it.copy(loading = false, error = r.error.businessMessage()) }
            }
        }
    }

    companion object {
        const val ARG_BUSINESS_ID = "businessId"
    }
}

// ---------- Compartir historial (dueno, share) ----------

data class ShareUiState(
    val vets: List<NearbyBusiness> = emptyList(),
    val loadingVets: Boolean = true,
    /** null = cualquier negocio que escanee. */
    val businessId: String? = null,
    val data: Set<SharedData> = setOf(SharedData.Vaccines, SharedData.Dewormings, SharedData.Weights),
    val duration: ShareDuration = ShareDuration.OneDay,
    val saving: Boolean = false,
    val error: String? = null,
    val sharedWith: String? = null,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: BusinessRepository,
) : ViewModel() {
    val petId: String = checkNotNull(savedState[mx.petmate.app.ui.navigation.Route.ARG_PET_ID])
    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    fun loadVets(latitude: Double?, longitude: Double?) {
        val (lat, lng) = if (latitude != null && longitude != null) latitude to longitude else MapViewModel.DEFAULT_CENTER
        viewModelScope.launch {
            val vets = (repository.getNearby(lat, lng) as? AppResult.Success)?.data.orEmpty().filter { it.kind == BusinessKind.Veterinary }
            _state.update { it.copy(vets = vets, loadingVets = false) }
        }
    }

    fun selectBusiness(id: String?) = _state.update { it.copy(businessId = id) }

    fun toggle(item: SharedData) = _state.update { it.copy(data = if (item in it.data) it.data - item else it.data + item) }

    fun duration(value: ShareDuration) = _state.update { it.copy(duration = value) }

    fun share() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val r = repository.shareAccess(petId, s.businessId, s.data, s.duration)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, sharedWith = s.vets.firstOrNull { v -> v.id == s.businessId }?.name ?: "Cualquier negocio que escanee")
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, error = r.error.businessMessage()) }
            }
        }
    }

    fun dismissShared() = _state.update { it.copy(sharedWith = null) }
}

/** Dias en orden para los chips del horario. */
val ORDERED_DAYS: List<String> = SCHEDULE_DAYS
