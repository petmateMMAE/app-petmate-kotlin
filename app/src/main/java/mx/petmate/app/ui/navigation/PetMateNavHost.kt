package mx.petmate.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.ui.auth.LoginScreen
import mx.petmate.app.ui.auth.RegisterScreen
import mx.petmate.app.ui.auth.WelcomeScreen
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.home.HomeScreen
import mx.petmate.app.ui.map.MapScreen
import mx.petmate.app.ui.splash.SplashScreen

/**
 * Grafo unico de navegacion.
 *
 * El grafo que se muestra depende del estado de la sesion: no hay rutas de
 * identidad y de contenido mezcladas, asi que el boton atras nunca regresa al
 * login despues de autenticarse (spec 002, C-07).
 */
@Composable
fun PetMateNavHost(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()

    when (val state = sessionState) {
        // Mientras se revisa la sesion se ve la splash, sin el boton de empezar.
        SessionState.Checking -> SplashScreen(modifier = modifier)

        SessionState.SignedOut -> AuthGraph(modifier = modifier)

        is SessionState.SignedIn -> MainGraph(userName = state.user.name, modifier = modifier)
    }
}

@Composable
private fun AuthGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.SPLASH,
        modifier = modifier,
    ) {
        composable(Route.SPLASH) {
            SplashScreen(
                onStartClick = {
                    navController.navigate(Route.WELCOME) {
                        popUpTo(Route.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Route.LOGIN) },
                onRegisterClick = { navController.navigate(Route.REGISTER) },
            )
        }
        composable(Route.LOGIN) {
            LoginScreen(
                // Al autenticarse cambia el estado de sesion y el grafo entero
                // se sustituye: esta pantalla no navega por su cuenta.
                onSignedIn = {},
                onGoToRegister = {
                    navController.navigate(Route.REGISTER) {
                        popUpTo(Route.WELCOME)
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Route.REGISTER) {
            RegisterScreen(
                onSignedIn = {},
                onGoToLogin = {
                    navController.navigate(Route.LOGIN) {
                        popUpTo(Route.WELCOME)
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

/** Contenido con sesion iniciada: el mapa es la pantalla de inicio (spec 010, A-01). */
@Composable
private fun MainGraph(userName: String, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Route.MAP,
        modifier = modifier,
    ) {
        composable(Route.MAP) {
            MapScreen(
                onTabSelected = { tab ->
                    // Mascotas y Avisos aun no tienen pantalla (specs 007 y 008).
                    if (tab == OwnerTab.Profile) navController.navigate(Route.HOME)
                },
                // La ficha del negocio llega con la spec 013.
                onBusinessDetailsClick = {},
            )
        }
        composable(Route.HOME) {
            HomeScreen(userName = userName, onBackToMap = { navController.popBackStack() })
        }
    }
}
