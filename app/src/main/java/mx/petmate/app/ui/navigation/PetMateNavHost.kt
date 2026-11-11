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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.petmate.app.ui.pets.AllergiesScreen
import mx.petmate.app.ui.pets.AllergyFormScreen
import mx.petmate.app.ui.pets.CarnetScreen
import mx.petmate.app.ui.pets.CarnetSection
import mx.petmate.app.ui.pets.DewormFormScreen
import mx.petmate.app.ui.pets.DewormScreen
import mx.petmate.app.ui.pets.PetCarnetViewModel
import mx.petmate.app.ui.pets.PetFormScreen
import mx.petmate.app.ui.pets.PetsScreen
import mx.petmate.app.ui.pets.QrScreen
import mx.petmate.app.ui.pets.VaccineFormScreen
import mx.petmate.app.ui.pets.VaccinesScreen
import mx.petmate.app.ui.pets.WeightScreen
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
import mx.petmate.app.ui.business.AffiliateScreen
import mx.petmate.app.ui.business.BizProfileScreen
import mx.petmate.app.ui.business.ShareScreen
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

        // Con negocio y en modo negocio se ve el grafo del vendedor (panel, escanear, atendidas).
        is SessionState.SignedIn -> if (state.businessMode) {
            BusinessGraph(user = state.user, onSwitchToOwner = { viewModel.setBusinessMode(false) }, modifier = modifier)
        } else {
            MainGraph(user = state.user, onLeaveDemo = {}, onSwitchToBusiness = { viewModel.setBusinessMode(true) }, modifier = modifier)
        }
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
    onSwitchToBusiness: () -> Unit = {},
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
                        onBusinessDetailsClick = { createAccount() },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                MapScreen(
                    onTabSelected = { navController.openTab(it) },
                    onBusinessDetailsClick = { navController.navigate(Route.bizProfile(it)) },
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
                    onAffiliateClick = { if (isGuest) createAccount() else navController.navigate(Route.AFFILIATE) },
                    onSwitchModeClick = onSwitchToBusiness,
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

        petsGraph(navController = navController, user = user, isGuest = isGuest, onNeedAccount = createAccount)

        composable(Route.AFFILIATE) {
            AffiliateScreen(viewModel = hiltViewModel(), onBackClick = { navController.popBackStack() })
        }
        composable(Route.BIZ_PROFILE, arguments = listOf(navArgument(Route.ARG_BUSINESS_ID) { type = NavType.StringType })) {
            BizProfileScreen(viewModel = hiltViewModel(), onBackClick = { navController.popBackStack() })
        }
        composable(Route.SHARE, arguments = listOf(navArgument(Route.ARG_PET_ID) { type = NavType.StringType })) { entry ->
            val pets: PetCarnetViewModel = hiltViewModel(entry)
            val petState by pets.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { pets.refresh() }
            ShareScreen(viewModel = hiltViewModel(entry), petName = petState.pet?.name.orEmpty(), onBackClick = { navController.popBackStack() })
        }
    }
}

/** Mascotas, carnet y QR (specs 007, 008 y 009). En el modo demo se ven las mascotas de ejemplo y guardar pide cuenta. */
private fun NavGraphBuilder.petsGraph(
    navController: NavHostController,
    user: User?,
    isGuest: Boolean,
    onNeedAccount: () -> Unit,
) {
    val petArg = listOf(navArgument(Route.ARG_PET_ID) { type = NavType.StringType })
    val back: () -> Unit = { navController.popBackStack() }

    composable(Route.PETS) {
        PetsScreen(
            userName = user?.name.orEmpty(),
            isGuest = isGuest,
            onTabSelected = { navController.openTab(it) },
            onPetClick = { navController.navigate(Route.carnet(it)) },
            onAddPet = { navController.navigate(Route.petForm()) },
            onNeedAccount = onNeedAccount,
        )
    }
    composable(
        Route.PET_FORM,
        arguments = listOf(navArgument(Route.ARG_PET_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) {
        PetFormScreen(
            onBackClick = back,
            onSaved = { petId, isNew ->
                if (isNew) {
                    // Recien creada: directo a su carnet (petSaved de la maqueta), sin regresar al formulario.
                    navController.navigate(Route.carnet(petId)) { popUpTo(Route.PET_FORM) { inclusive = true } }
                } else {
                    navController.popBackStack()
                }
            },
            onDeleted = { navController.popBackStack(Route.PETS, inclusive = false) },
        )
    }
    composable(Route.CARNET, arguments = petArg) { entry ->
        val viewModel: PetCarnetViewModel = hiltViewModel(entry)
        val petId = viewModel.petId
        CarnetScreen(
            viewModel = viewModel,
            isGuest = isGuest,
            onBackClick = back,
            onEditClick = { navController.navigate(Route.petForm(petId)) },
            onOpen = { section ->
                navController.navigate(
                    when (section) {
                        CarnetSection.Vaccines -> Route.vaccines(petId)
                        CarnetSection.Deworm -> Route.deworm(petId)
                        CarnetSection.Weight -> Route.weight(petId)
                        CarnetSection.Allergies -> Route.allergies(petId)
                        CarnetSection.Qr -> Route.qr(petId)
                    },
                )
            },
            onNeedAccount = onNeedAccount,
        )
    }
    composable(Route.QR, arguments = petArg) { entry ->
        QrScreen(
            hiltViewModel(entry),
            isGuest,
            onBackClick = back,
            onNeedAccount = onNeedAccount,
            onShareClick = { entry.arguments?.getString(Route.ARG_PET_ID)?.let { navController.navigate(Route.share(it)) } },
        )
    }
    composable(Route.VACCINES, arguments = petArg) { entry ->
        val viewModel: PetCarnetViewModel = hiltViewModel(entry)
        VaccinesScreen(viewModel, isGuest, back, onAddClick = { navController.navigate(Route.vaccineForm(viewModel.petId)) }, onNeedAccount = onNeedAccount)
    }
    composable(Route.VACCINE_FORM, arguments = petArg) { entry ->
        VaccineFormScreen(hiltViewModel(entry), onBackClick = back, onSaved = back)
    }
    composable(Route.DEWORM, arguments = petArg) { entry ->
        val viewModel: PetCarnetViewModel = hiltViewModel(entry)
        DewormScreen(viewModel, isGuest, back, onAddClick = { navController.navigate(Route.dewormForm(viewModel.petId)) }, onNeedAccount = onNeedAccount)
    }
    composable(Route.DEWORM_FORM, arguments = petArg) { entry ->
        DewormFormScreen(hiltViewModel(entry), onBackClick = back, onSaved = back)
    }
    composable(Route.WEIGHT, arguments = petArg) { entry ->
        WeightScreen(hiltViewModel(entry), isGuest, onBackClick = back, onNeedAccount = onNeedAccount)
    }
    composable(Route.ALLERGIES, arguments = petArg) { entry ->
        val viewModel: PetCarnetViewModel = hiltViewModel(entry)
        AllergiesScreen(
            viewModel = viewModel,
            isGuest = isGuest,
            onBackClick = back,
            onAddClick = { navController.navigate(Route.allergyForm(viewModel.petId)) },
            onEditClick = { navController.navigate(Route.allergyForm(viewModel.petId, it)) },
            onNeedAccount = onNeedAccount,
        )
    }
    composable(
        Route.ALLERGY_FORM,
        arguments = petArg + navArgument(Route.ARG_ALLERGY_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
    ) { entry ->
        AllergyFormScreen(
            viewModel = hiltViewModel(entry),
            allergyId = entry.arguments?.getString(Route.ARG_ALLERGY_ID),
            onBackClick = back,
            onSaved = back,
        )
    }
}

/** Pestanas de la barra inferior. Avisos aun no tiene pantalla (spec 006). */
private fun NavHostController.openTab(tab: OwnerTab) {
    when (tab) {
        OwnerTab.Map -> popBackStack(Route.MAP, inclusive = false)
        OwnerTab.Pets -> if (currentDestination?.route != Route.PETS) {
            navigate(Route.PETS) {
                popUpTo(Route.MAP)
                launchSingleTop = true
            }
        }
        OwnerTab.Profile -> if (currentDestination?.route != Route.HOME) {
            navigate(Route.HOME) {
                popUpTo(Route.MAP)
                launchSingleTop = true
            }
        }
        else -> Unit
    }
}
