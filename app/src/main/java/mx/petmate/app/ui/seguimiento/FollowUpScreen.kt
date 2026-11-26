package mx.petmate.app.ui.seguimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.petmate.app.R
import mx.petmate.app.ui.components.PetMateBackBar
import mx.petmate.app.ui.components.PetMateButton
import mx.petmate.app.ui.components.PetMateButtonStyle
import mx.petmate.app.ui.components.PetMateSmallButton
import mx.petmate.app.ui.components.PetMateTextField
import mx.petmate.app.ui.theme.petMateColors

/**
 * Seguimiento de una consulta para la veterinaria (spec 015, T-009 y T-011). Se abre desde la ficha justo
 * despues de guardar la consulta: si no hay seguimiento ofrece abrirlo; si hay, deja cambiar el estado solo a
 * las transiciones validas, fijar la proxima revision y agregar notas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FollowUpScreen(
    consultationId: String,
    petName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpViewModel = hiltViewModel(key = "followup-$consultationId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.petMateColors
    LaunchedEffect(consultationId) { viewModel.load(consultationId) }

    Column(modifier = modifier.fillMaxSize()) {
        PetMateBackBar(onBackClick = onClose)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.followup_title, petName), style = MaterialTheme.typography.headlineSmall)
            val followup = state.followup
            when {
                state.isLoading -> CircularProgressIndicator()
                followup == null -> {
                    Text(stringResource(R.string.followup_none), color = colors.ink2)
                    ReviewDaysField(state.reviewDays, viewModel::onReviewDays)
                    PetMateButton(
                        stringResource(R.string.followup_open),
                        viewModel::open,
                        loading = state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PetMateButton(
                        stringResource(R.string.followup_skip),
                        onClose,
                        style = PetMateButtonStyle.Outline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(stringResource(state.status.label()), style = MaterialTheme.typography.titleLarge)
                    ReviewDateBadge(state.reviewClass, followup.nextReviewOn)
                    if (state.isClosed) {
                        Text(stringResource(R.string.followup_closed), color = colors.ink2)
                    } else {
                        ReviewDaysField(state.reviewDays, viewModel::onReviewDays)
                        Text(stringResource(R.string.followup_change_to), style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.nextOptions.forEach { option ->
                                PetMateSmallButton(stringResource(option.label()), { viewModel.changeTo(option) })
                            }
                        }
                        PetMateTextField(
                            value = state.note,
                            onValueChange = viewModel::onNote,
                            label = stringResource(R.string.followup_note),
                            hint = stringResource(R.string.followup_note_hint),
                        )
                        PetMateButton(
                            stringResource(R.string.followup_add_note),
                            viewModel::addNote,
                            style = PetMateButtonStyle.Outline,
                            loading = state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    StatusTimeline(followup.notes)
                    PetMateButton(stringResource(R.string.followup_done), onClose, modifier = Modifier.fillMaxWidth())
                }
            }
            state.error?.let { Text(stringResource(it), color = colors.alert) }
        }
    }
}

@Composable
private fun ReviewDaysField(value: String, onChange: (String) -> Unit) {
    PetMateTextField(
        value = value,
        onValueChange = onChange,
        label = stringResource(R.string.followup_review_days),
        hint = stringResource(R.string.followup_review_days_hint),
        keyboardType = KeyboardType.Number,
    )
}
