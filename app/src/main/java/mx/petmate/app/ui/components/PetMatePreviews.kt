package mx.petmate.app.ui.components

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Vista previa en claro y en oscuro con medidas de telefono.
 *
 * Ponla sobre cada @Composable de preview para revisar los dos temas a la vez.
 */
@Preview(
    name = "Claro",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
)
@Preview(
    name = "Oscuro",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class PetMatePreviews
