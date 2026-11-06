package mx.petmate.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.petmate.app.domain.model.SessionState
import mx.petmate.app.domain.model.User
import mx.petmate.app.ui.auth.LoginScreen
import mx.petmate.app.ui.auth.RegisterScreen
import mx.petmate.app.ui.auth.WelcomeScreen
import mx.petmate.app.ui.components.DemoStrip
import mx.petmate.app.ui.components.OwnerTab
import mx.petmate.app.ui.home.DeleteAccountScreen
import mx.petmate.app.ui.home.HomeScreen
import mx.petmate.app.ui.home.LegalScreen
import mx.petmate.app.ui.home.ProfileActions
import mx.petmate.app.ui.map.MapScreen
import mx.petmate.app.ui.splash.SplashScreen

/**
 * Grafo unico de navegacion.
 *
 * El grafo que se muestra depende del estado de la sesion: no hay rutas de
 * identidad y de contenido mezcladas, asi que el boton atras nunca regresa al
 * login despues de autenticarse (spec 002, C-07).
 *
 * "Probar sin cuenta" (SessionState.Guest) usa el mismo grafo de contenido que una sesion real,
 * con la franja DEMO arriba. Al salir de la demo se abre el registro, el login o la bienvenida
 * segun el boton que se toco.
 */
@Composable
fun PetMateNavHost(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    // Pantalla de identidad que se abre al salir de la demo (null = flujo normal desde la splash).
    var authTarget by rememberSaveable { mutableStateOf<String?>(null) }

    when (val state = sessionState) {
        // Mientras se revisa la sesion se ve la splash, sin el boton de empezar.
        SessionState.Checking -> SplashScreen(modifier = modifier)

        SessionState.SignedOut -> AuthGraph(
            target = authTarget,
            onTargetOpened = { authTarget = null },
            onDemoClick = viewModel::enterGuestMode,
            modifier = modifier,
        )

        SessionState.Guest -> MainGraph(
            user = null,
            onLeaveDemo = { target ->
                authTarget = target
                viewModel.exitGuestMode()
            },
            modifier = modifier,
        )

        is SessionState.SignedIn -> MainGraph(user = state.user, onLeaveDemo = {}, modifier = modifier)
    }
}

@Composable
private fun AuthGraph(
    target: String?,
    onTargetOpened: () -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    // Se fija una sola vez: si cambiara, NavHost rearmaria el grafo y perderia la pila.
    val startDestination = remember { if (target != null) Route.WELCOME else Route.SPLASH }
    NavHost(
        navController = navController,
        startDestination = startDestination,
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
                onDemoClick = onDemoClick,
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

    // Viene de "Crear cuenta" o "Ya tengo cuenta" en la demo: se abre esa pantalla encima
    // de la bienvenida, para que atras regrese ahi.
    LaunchedEffect(target) {
        if (target != null) {
            if (target != Route.WELCOME) navController.navigate(target)
            onTargetOpened()
        }
    }
}

/**
 * Contenido de la app: el mapa es la pantalla de inicio (spec 010, A-01).
 * [user] es null en el modo demo.
 */
@Composable
private fun MainGraph(
    user: User?,
    onLeaveDemo: (authRoute: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGuest = user == null
    val navController = rememberNavController()
    val createAccount = { onLeaveDemo(Route.REGISTER) }

    NavHost(
        navController = navController,
        startDestination = Route.MAP,
        modifier = modifier,
    ) {
        composable(Route.MAP) {
            if (isGuest) {
                // La franja DEMO va arriba del mapa; statusBarsPadding consume el inset
                // para que el mapa no lo vuelva a sumar.
                Column(modifier = Modifier.statusBarsPadding()) {
                    DemoStrip(onCreateAccountClick = createAccount)
                    MapScreen(
                        onTabSelected = { navController.openTab(it) },
                        onBusinessDetailsClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                MapScreen(
                    onTabSelected = { navController.openTab(it) },
                    // La ficha del negocio llega con la spec 013.
                    onBusinessDetailsClick = {},
                )
            }
        }
        composable(Route.HOME) {
            HomeScreen(
                isGuest = isGuest,
                userName = user?.name.orEmpty(),
                userEmail = user?.email.orEmpty(),
                isAffiliate = user?.isAffiliate ?: false,
                actions = ProfileActions(
                    onTabSelected = { navController.openTab(it) },
                    onCreateAccountClick = createAccount,
                    onLoginClick = { onLeaveDemo(Route.LOGIN) },
                    onExitDemoClick = { onLeaveDemo(Route.WELCOME) },
                    onPrivacyClick = { navController.navigate(Route.LEGAL) },
                    onDeleteAccountClick = { navController.navigate(Route.DELETE_ACCOUNT) },
                ),
            )
        }
        composable(Route.LEGAL) {
            LegalScreen(
                onBackClick = { navController.popBackStack() },
                isGuest = isGuest,
                onCreateAccountClick = createAccount,
            )
        }
        composable(Route.DELETE_ACCOUNT) {
            DeleteAccountScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

/** Mascotas y Avisos aun no tienen pantalla (specs 007 y 008). */
private fun NavHostController.openTab(tab: OwnerTab) {
    when (tab) {
        OwnerTab.Map -> popBackStack(Route.MAP, inclusive = false)
        OwnerTab.Profile -> if (currentDestination?.route != Route.HOME) {
            navigate(Route.HOME) { launchSingleTop = true }
        }
        else -> Unit
    }
}
