package mx.petmate.app.ui.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.domain.model.PetSex
import mx.petmate.app.domain.model.PetSpecies
import mx.petmate.app.domain.usecase.PetDates
import mx.petmate.app.domain.usecase.PetRules
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.components.PetMateBadge
import mx.petmate.app.ui.components.PetMateBottomBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.petMateColors

// =====================================================================================================
// Mis mascotas (SCREENS.pets / petsEmpty de la maqueta)
// =====================================================================================================

@Composable
fun PetsScreen(
    userName: String,
    isGuest: Boolean,
    onTabSelected: (OwnerTab) -> Unit,
    onPetClick: (String) -> Unit,
    onAddPet: () -> Unit,
    onNeedAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val firstName = userName.trim().substringBefore(' ')

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            if (isGuest) DemoStrip(onCreateAccountClick = onNeedAccount)
            PetTopBar(
                title = stringResource(if (isGuest) R.string.pets_title_demo else R.string.pets_title),
                subtitle = if (isGuest) stringResource(R.string.pets_subtitle_demo) else stringResource(R.string.pets_hello, firstName),
                onBackClick = null,
            )
        },
        bottom = { PetMateBottomBar(selected = OwnerTab.Pets, onTabSelected = onTabSelected) },
    ) {
        when {
            state.isLoading -> PetLoading()
            state.error != null && state.pets.isEmpty() -> PetError(state.error!!, onRetry = viewModel::refresh)
            state.pets.isEmpty() -> PetEmptyState(
                icon = R.drawable.ic_pm_paw,
                title = stringResource(R.string.pets_empty_title),
                text = stringResource(R.string.pets_empty_text),
                iconBackground = MaterialTheme.petMateColors.sunSoft,
                iconTint = MaterialTheme.petMateColors.sunInk,
                action = {
                    PetMateButton(
                        text = stringResource(R.string.pets_add_first),
                        onClick = if (isGuest) onNeedAccount else onAddPet,
                        icon = R.drawable.ic_pm_plus,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                val first = state.pets.first()
                first.next?.let { NextStrip(petName = first.pet.name, event = it, onClick = { onPetClick(first.pet.id) }) }
                state.pets.forEach { summary ->
                    PetCard(summary = summary, onClick = { onPetClick(summary.pet.id) })
                }
                AddPetCard(onClick = if (isGuest) onNeedAccount else onAddPet)
                VSpace(16.dp)
            }
        }
    }
}

/** .next-strip: lo proximo que le toca a la primera mascota. */
@Composable
private fun NextStrip(petName: String, event: NextEvent, onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(extra.sunSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp).background(extra.sun, RoundedCornerShape(12.dp))) {
            Icon(painterResource(R.drawable.ic_pm_bell), contentDescription = null, tint = extra.onSun, modifier = Modifier.size(22.dp))
        }
        Column {
            val what = if (event.isVaccine) stringResource(R.string.pets_next_vaccine, event.name) else stringResource(R.string.pets_next_deworm)
            Text("$what · $petName ${whenText(event.date)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(formatDate(event.date), fontSize = 13.sp, color = extra.ink2)
        }
    }
}

/** .pet-card */
@Composable
private fun PetCard(summary: PetSummary, onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    val pet = summary.pet
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(2.dp, shape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        PetAvatar(name = pet.name, species = pet.species, size = 76.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(pet.name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
            val parts = listOfNotNull(speciesLabel(pet.species), pet.breed, ageText(pet.birthDate))
            Text(parts.joinToString(" · "), fontSize = 14.sp, color = extra.ink2, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val next = summary.next
                if (next != null) {
                    val soon = (PetDates.daysBetween(PetDates.today(), next.date) ?: 99) <= 7
                    PetMateBadge(
                        text = "${stringResource(if (next.isVaccine) R.string.pets_badge_vaccine else R.string.pets_badge_deworm)} ${whenText(next.date)}",
                        container = if (soon) extra.sunSoft else extra.surface2,
                        content = if (soon) extra.sunInk else extra.ink2,
                        icon = if (next.isVaccine) R.drawable.ic_pm_syringe else R.drawable.ic_pm_pill,
                    )
                } else {
                    PetMateBadge(stringResource(R.string.pets_badge_ok), extra.okSoft, extra.ok, icon = R.drawable.ic_pm_check)
                }
                if (summary.activeAllergies > 0) {
                    PetMateBadge(
                        pluralStringResource(R.plurals.pets_badge_allergies, summary.activeAllergies, summary.activeAllergies),
                        extra.alertSoft,
                        extra.alert,
                        icon = R.drawable.ic_pm_alert,
                    )
                }
            }
        }
        Icon(painterResource(R.drawable.ic_pm_next), contentDescription = null, tint = extra.ink3, modifier = Modifier.size(24.dp))
    }
}

/** .add-card */
@Composable
private fun AddPetCard(onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, extra.line, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(painterResource(R.drawable.ic_pm_plus), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.pets_add), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun speciesLabel(species: PetSpecies): String =
    stringResource(if (species == PetSpecies.Dog) R.string.pets_species_dog else R.string.pets_species_cat)

@Composable
fun sexLabel(sex: PetSex): String? = when (sex) {
    PetSex.Male -> stringResource(R.string.pets_sex_male)
    PetSex.Female -> stringResource(R.string.pets_sex_female)
    PetSex.Unknown -> null
}

// =====================================================================================================
// Nueva mascota / Editar (SCREENS.petForm de la maqueta)
// =====================================================================================================

@Composable
fun PetFormScreen(
    onBackClick: () -> Unit,
    onSaved: (petId: String, isNew: Boolean) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PetFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.savedPetId) { state.savedPetId?.let { onSaved(it, !state.isEdit) } }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    state.message?.let { message ->
        val text = message.asString()
        LaunchedEffect(message) {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    PetScaffoldColumn(
        modifier = modifier.fillMaxSize(),
        top = {
            PetTopBar(
                title = if (state.isEdit) stringResource(R.string.pet_form_edit_title, state.name) else stringResource(R.string.pet_form_new_title),
                onBackClick = onBackClick,
            )
        },
        bottom = {
            PetBottomAction {
                PetMateButton(
                    text = stringResource(if (state.isEdit) R.string.pet_form_save_changes else R.string.pet_form_save),
                    onClick = viewModel::save,
                    icon = R.drawable.ic_pm_check,
                    loading = state.isSaving,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        if (state.isLoading) {
            PetLoading()
            return@PetScaffoldColumn
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            // Foto: llega con la carga de imagenes del backend (pm-079); mientras, el retrato con la inicial.
            Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                PetAvatar(name = state.name, species = state.species ?: PetSpecies.Dog, size = 116.dp, radius = 34.dp)
            }
            PetFieldLabel(stringResource(R.string.pet_form_species), required = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SpeciesTile(PetSpecies.Dog, state.species == PetSpecies.Dog, Modifier.weight(1f)) { viewModel.onSpecies(PetSpecies.Dog) }
                SpeciesTile(PetSpecies.Cat, state.species == PetSpecies.Cat, Modifier.weight(1f)) { viewModel.onSpecies(PetSpecies.Cat) }
            }
            state.errors[PetRules.Field.Species]?.let {
                Text(it.asString(), color = MaterialTheme.petMateColors.alert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            }
            Text(
                stringResource(R.string.pet_form_species_hint),
                fontSize = 13.sp,
                color = MaterialTheme.petMateColors.ink2,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            PetMateTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = stringResource(R.string.pet_form_name),
                placeholder = stringResource(R.string.pet_form_name_placeholder),
                required = true,
                error = state.errors[PetRules.Field.Name],
            )
            PetMateTextField(
                value = state.breed,
                onValueChange = viewModel::onBreed,
                label = stringResource(R.string.pet_form_breed),
                placeholder = stringResource(R.string.pet_form_breed_placeholder),
                error = state.errors[PetRules.Field.Breed],
            )
            PetFieldLabel(stringResource(R.string.pet_form_sex))
            PetSegmented(
                options = listOf(PetSex.Male to stringResource(R.string.pets_sex_male), PetSex.Female to stringResource(R.string.pets_sex_female)),
                selected = state.sex.takeIf { it != PetSex.Unknown },
                onSelect = viewModel::onSex,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            PetDateField(
                label = stringResource(R.string.pet_form_birth),
                value = state.birthDate,
                onValueChange = viewModel::onBirthDate,
                hint = stringResource(R.string.pet_form_birth_hint),
                error = state.errors[PetRules.Field.BirthDate],
            )
            if (state.isEdit) {
                PetMateButton(
                    text = stringResource(R.string.pet_form_delete, state.name),
                    onClick = { confirmDelete = true },
                    style = PetMateButtonStyle.Soft,
                    icon = R.drawable.ic_pm_trash,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
            VSpace(24.dp)
        }
    }

    if (confirmDelete) {
        PetConfirmDialog(
            title = stringResource(R.string.pet_delete_title, state.name),
            text = stringResource(R.string.pet_delete_text),
            confirm = stringResource(R.string.pet_delete_confirm),
            danger = true,
            icon = R.drawable.ic_pm_trash,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** .tile de la maqueta para elegir especie. */
@Composable
private fun SpeciesTile(species: PetSpecies, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val extra = MaterialTheme.petMateColors
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(shape)
            .background(if (selected) extra.brandSoft else MaterialTheme.colorScheme.surface)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else extra.line, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
    ) {
        PetAvatar(name = speciesLabel(species), species = species, size = 52.dp, radius = 16.dp)
        Text(
            speciesLabel(species),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
