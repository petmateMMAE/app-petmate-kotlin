package mx.petmate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import mx.petmate.app.ui.navigation.PetMateNavHost
import mx.petmate.app.ui.theme.PetMateEdelTheme
import mx.petmate.app.ui.theme.ThemeSettings
import androidx.compose.foundation.isSystemInDarkTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PetMateEdelTheme(darkTheme = ThemeSettings.darkMode ?: isSystemInDarkTheme()) {
                // Sin padding global: cada pantalla maneja sus barras del
                // sistema, asi la splash y el mapa llegan hasta arriba.
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    PetMateNavHost()
                }
            }
        }
    }
}
