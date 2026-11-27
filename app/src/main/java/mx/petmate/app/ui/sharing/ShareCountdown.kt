package mx.petmate.app.ui.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mx.petmate.app.R
import mx.petmate.app.domain.sharing.ShareExpiry
import mx.petmate.app.ui.components.PetMatePreviews
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.petMateColors

/** Etiquetas para las pruebas de interfaz. */
object ShareCountdownTags {
    const val LABEL = "share_countdown"
}

/**
 * Cuenta regresiva de una comparticion (spec 009, T-005, R-06 y R-07).
 *
 * [expiresAt] es la marca que mando el servidor. El reloj del telefono solo
 * sirve para saber cuanto falta; **la fecha de corte nunca se calcula aqui**
 * (ver `ShareExpiry`).
 *
 * Cuando llega a cero avisa con [onExpired] para que la pantalla deje de
 * mostrar el QR, y el texto cambia a "ya venció".
 *
 * [nowMillis] se inyecta para poder probarlo y para las previsualizaciones.
 */
@Composable
fun ShareCountdown(
    expiresAt: String?,
    modifier: Modifier = Modifier,
    onExpired: () -> Unit = {},
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    var now by remember(expiresAt) { mutableLongStateOf(nowMillis()) }
    val state = ShareExpiry.remaining(expiresAt, now)

    // Un tic por segundo, y solo mientras quede tiempo: cuando vence no hay
    // nada que seguir contando.
    LaunchedEffect(expiresAt, state is ShareExpiry.State.Live) {
        while (state is ShareExpiry.State.Live) {
            delay(TICK_MILLIS)
            now = nowMillis()
        }
    }

    LaunchedEffect(expiresAt, state) {
        if (state !is ShareExpiry.State.Live) onExpired()
    }

    val colors = MaterialTheme.petMateColors
    val isLive = state is ShareExpiry.State.Live

    Surface(
        color = if (isLive) colors.surface2 else colors.alertSoft,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag(ShareCountdownTags.LABEL),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = when (state) {
                    is ShareExpiry.State.Live ->
                        stringResource(R.string.share_countdown_label, state.asClock())
                    ShareExpiry.State.Expired -> stringResource(R.string.share_countdown_expired)
                    ShareExpiry.State.Unknown -> stringResource(R.string.share_countdown_unknown)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isLive) colors.ink2 else colors.alert,
            )
        }
    }
}

/**
 * Lo que queda, como reloj: `mm:ss`, o `h:mm:ss` cuando pasa de una hora.
 *
 * El formato se arma aqui, en la interfaz, porque es presentacion; el dominio
 * solo da los numeros.
 */
private fun ShareExpiry.State.Live.asClock(): String {
    val hours = hours
    val minutes = minutes % 60
    val seconds = seconds
    return if (hours > 0) {
        "$hours:${minutes.pad()}:${seconds.pad()}"
    } else {
        "${minutes.pad()}:${seconds.pad()}"
    }
}

private fun Long.pad(): String = toString().padStart(2, '0')

private const val TICK_MILLIS = 1_000L

@PetMatePreviews
@Preview
@Composable
private fun ShareCountdownPreview() {
    PetMateEdelTheme {
        ShareCountdown(
            expiresAt = "2026-11-26T10:30:00Z",
            nowMillis = { 0L },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PetMatePreviews
@Preview
@Composable
private fun ShareCountdownExpiredPreview() {
    PetMateEdelTheme {
        ShareCountdown(
            expiresAt = "1970-01-01T00:00:00Z",
            modifier = Modifier.padding(16.dp),
        )
    }
}
