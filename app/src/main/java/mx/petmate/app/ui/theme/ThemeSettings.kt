package mx.petmate.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Preferencia de tema elegida en Perfil > Ajustes > Modo oscuro (maqueta).
 *
 * `null` = seguir al sistema. Vive en memoria mientras la app esta abierta; guardarla entre
 * sesiones queda para la spec 017 (ajustes y privacidad).
 */
object ThemeSettings {
    var darkMode by mutableStateOf<Boolean?>(null)
}
