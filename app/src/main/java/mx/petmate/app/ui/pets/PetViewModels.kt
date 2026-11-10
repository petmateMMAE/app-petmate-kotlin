package mx.petmate.app.ui.pets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.core.result.getOrNull
import mx.petmate.app.core.ui.UiText
import mx.petmate.app.domain.model.Allergy
import mx.petmate.app.domain.model.AllergyDraft
import mx.petmate.app.domain.model.Carnet
import mx.petmate.app.domain.model.DewormingDraft
import mx.petmate.app.domain.model.Pet
import mx.petmate.app.domain.model.PetDraft
import mx.petmate.app.domain.model.PetQr
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.model.VaccineDraft
import mx.petmate.app.domain.repository.PetRepository
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.domain.usecase.PetRules
import javax.inject.Inject

// =====================================================================================================
// Lista de mascotas (spec 007, A-04)
// =====================================================================================================

/** Lo proximo que le toca a una mascota (vacuna o desparasitacion con fecha de hoy en adelante). */
data class NextEvent(val isVaccine: Boolean, val name: String, val date: String)

data class PetSummary(val pet: Pet, val next: NextEvent?, val activeAllergies: Int)

data class PetsUiState(
    val isLoading: Boolean = true,
    val pets: List<PetSummary> = emptyList(),
    val error: UiText? = null,
)

/** Calcula el proximo evento del carnet (nextOf de la maqueta). */
fun Carnet.nextEvent(today: String): NextEvent? {
    val vaccines = vaccines.mapNotNull { v -> v.nextDoseOn?.let { NextEvent(true, v.name, it) } }
    val dewormings = dewormings.mapNotNull { d -> d.nextOn?.let { NextEvent(false, d.product, it) } }
    return (vaccines + dewormings).filter { it.date >= today }.minByOrNull { it.date }
}

@HiltViewModel
class PetsViewModel @Inject constructor(
    private val repository: PetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetsUiState())
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.update { it.copy(isLoading = it.pets.isEmpty(), error = null) }
        viewModelScope.launch {
            when (val result = repository.getPets()) {
                is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.toPetUiText()) }
                is AppResult.Success -> {
                    val today = PetDates.today()
                    // El carnet de cada mascota alimenta su etiqueta ("Vacuna en 3 dias", "1 alergia").
                    val summaries = result.data.map { pet ->
                        async {
                            val carnet = repository.getCarnet(pet.id).getOrNull()
                            PetSummary(
                                pet = pet,
                                next = carnet?.nextEvent(today),
                                activeAllergies = carnet?.allergies?.count { it.active } ?: 0,
                            )
                        }
                    }.awaitAll()
                    _uiState.update { PetsUiState(isLoading = false, pets = summaries) }
                }
            }
        }
    }
}

// =====================================================================================================
// Alta y edicion (spec 007, A-01, A-02, A-07)
// =====================================================================================================

data class PetFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val species: PetSpecies? = null,
    val breed: String = "",
    val sex: PetSex = PetSex.Unknown,
    val birthDate: String? = null,
    val errors: Map<PetRules.Field, UiText> = emptyMap(),
    val isSaving: Boolean = false,
    val message: UiText? = null,
    /** Id de la mascota recien guardada: la pantalla navega a su carnet. */
    val savedPetId: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class PetFormViewModel @Inject constructor(
    private val repository: PetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val petId: String? = savedStateHandle.get<String>(ARG_PET_ID)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(PetFormUiState(isEdit = petId != null, isLoading = petId != null))
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    init {
        if (petId != null) load(petId)
    }

    private fun load(id: String) = viewModelScope.launch {
        when (val result = repository.getPet(id)) {
            is AppResult.Success -> _uiState.update {
                val p = result.data
                it.copy(isLoading = false, name = p.name, species = p.species, breed = p.breed.orEmpty(), sex = p.sex, birthDate = p.birthDate)
            }
            is AppResult.Failure -> _uiState.update { it.copy(isLoading = false, message = result.error.toPetUiText()) }
        }
    }

    fun onName(value: String) = _uiState.update { it.copy(name = value, errors = it.errors - PetRules.Field.Name) }
    fun onSpecies(value: PetSpecies) = _uiState.update { it.copy(species = value, errors = it.errors - PetRules.Field.Species) }
    fun onBreed(value: String) = _uiState.update { it.copy(breed = value, errors = it.errors - PetRules.Field.Breed) }

    /** Tocar el sexo elegido otra vez lo deja sin dato (es opcional). */
    fun onSex(value: PetSex) = _uiState.update { it.copy(sex = if (it.sex == value) PetSex.Unknown else value) }
    fun onBirthDate(value: String?) = _uiState.update { it.copy(birthDate = value, errors = it.errors - PetRules.Field.BirthDate) }
    fun messageShown() = _uiState.update { it.copy(message = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val draft = PetDraft(s.name, s.species, s.breed, s.birthDate, s.sex)
        val issues = PetRules.validatePet(draft, PetDates.today())
        if (issues.isNotEmpty()) {
            _uiState.update { it.copy(errors = issues.toFieldErrors()) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (petId == null) repository.createPet(draft) else repository.updatePet(petId, draft)
            _uiState.update {
                when (result) {
                    is AppResult.Success -> it.copy(isSaving = false, savedPetId = result.data.id)
                    is AppResult.Failure -> it.copy(isSaving = false, message = result.error.toPetUiText())
                }
            }
        }
    }

    fun delete() {
        val id = petId ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            _uiState.update {
                when (val result = repository.deletePet(id)) {
                    is AppResult.Success -> it.copy(isSaving = false, deleted = true)
                    is AppResult.Failure -> it.copy(isSaving = false, message = result.error.toPetUiText())
                }
            }
        }
    }

    companion object {
        const val ARG_PET_ID = "petId"
    }
}

// =====================================================================================================
// Carnet, QR y registros de una mascota (spec 008 y 009)
// =====================================================================================================

data class CarnetUiState(
    val isLoading: Boolean = true,
    val pet: Pet? = null,
    val carnet: Carnet = Carnet(),
    val qr: PetQr? = null,
    val error: UiText? = null,
    val isBusy: Boolean = false,
    val formErrors: Map<PetRules.Field, UiText> = emptyMap(),
    /** Aviso de una sola vez (toast). */
    val message: UiText? = null,
    /** Un formulario termino bien: la pantalla regresa. */
    val saved: Boolean = false,
)

@HiltViewModel
class PetCarnetViewModel @Inject constructor(
    private val repository: PetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val petId: String = checkNotNull(savedStateHandle.get<String>(PetFormViewModel.ARG_PET_ID))

    private val _uiState = MutableStateFlow(CarnetUiState())
    val uiState: StateFlow<CarnetUiState> = _uiState.asStateFlow()

    /** Mascota y carnet completos. Se llama cada vez que la pantalla aparece. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = it.pet == null, error = null) }
        viewModelScope.launch {
            val pet = async { repository.getPet(petId) }
            val carnet = async { repository.getCarnet(petId) }
            val petResult = pet.await()
            val carnetResult = carnet.await()
            _uiState.update {
                when {
                    petResult is AppResult.Failure -> it.copy(isLoading = false, error = petResult.error.toPetUiText())
                    carnetResult is AppResult.Failure -> it.copy(isLoading = false, pet = petResult.getOrNull(), error = carnetResult.error.toPetUiText())
                    else -> it.copy(isLoading = false, pet = petResult.getOrNull(), carnet = carnetResult.getOrNull() ?: Carnet())
                }
            }
        }
    }

    fun loadQr() = viewModelScope.launch {
        when (val result = repository.getQr(petId)) {
            is AppResult.Success -> _uiState.update { it.copy(qr = result.data) }
            is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
        }
    }

    fun regenerateQr() = busy {
        when (val result = repository.regenerateQr(petId)) {
            is AppResult.Success -> _uiState.update { it.copy(qr = result.data, message = UiText.Resource(mx.petmate.app.R.string.qr_regenerated)) }
            is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
        }
    }

    fun saveVaccine(draft: VaccineDraft) = validated(PetRules.validateVaccine(draft, PetDates.today())) {
        repository.addVaccine(petId, draft)
    }

    fun saveDeworming(draft: DewormingDraft) = validated(PetRules.validateDeworming(draft, PetDates.today())) {
        repository.addDeworming(petId, draft)
    }

    fun saveWeight(kilograms: Double) {
        val kg = PetRules.roundKilograms(kilograms)
        val issues = PetRules.validateWeight(kg)
        if (issues.isNotEmpty()) {
            _uiState.update { it.copy(message = issues.first().toUiText()) }
            return
        }
        busy {
            when (val result = repository.addWeight(petId, PetDates.today(), kg)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(message = UiText.Resource(mx.petmate.app.R.string.weight_saved)) }
                    refresh()
                }
                is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
            }
        }
    }

    /** [allergyId] nulo = alta. */
    fun saveAllergy(allergyId: String?, draft: AllergyDraft) = validated(PetRules.validateAllergy(draft)) {
        if (allergyId == null) repository.addAllergy(petId, draft) else repository.updateAllergy(allergyId, draft)
    }

    /** "Marcar como superada" / "Volver a activar". */
    fun toggleAllergy(allergy: Allergy) = busy {
        val draft = AllergyDraft(allergy.name, allergy.kind, allergy.notes.orEmpty(), active = !allergy.active)
        when (val result = repository.updateAllergy(allergy.id, draft)) {
            is AppResult.Success -> refresh()
            is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
        }
    }

    fun deleteVaccine(id: String) = busy {
        when (val result = repository.deleteVaccine(id)) {
            is AppResult.Success -> refresh()
            is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
        }
    }

    fun deleteDeworming(id: String) = busy {
        when (val result = repository.deleteDeworming(id)) {
            is AppResult.Success -> refresh()
            is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
        }
    }

    fun clearFieldError(field: PetRules.Field) = _uiState.update { it.copy(formErrors = it.formErrors - field) }
    fun messageShown() = _uiState.update { it.copy(message = null) }

    private fun validated(issues: List<PetRules.Issue>, save: suspend () -> AppResult<*>) {
        if (issues.isNotEmpty()) {
            _uiState.update { it.copy(formErrors = issues.toFieldErrors()) }
            return
        }
        busy {
            when (val result = save()) {
                is AppResult.Success -> _uiState.update { it.copy(saved = true, formErrors = emptyMap()) }
                is AppResult.Failure -> _uiState.update { it.copy(message = result.error.toPetUiText()) }
            }
        }
    }

    private fun busy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }
}
