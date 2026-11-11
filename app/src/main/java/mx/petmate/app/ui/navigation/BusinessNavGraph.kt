package mx.petmate.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.petmate.app.domain.model.User
import mx.petmate.app.ui.business.AttendedScreen
import mx.petmate.app.ui.business.BizEditScreen
import mx.petmate.app.ui.business.BusinessBottomBar
import mx.petmate.app.ui.business.BusinessPanelScreen
import mx.petmate.app.ui.business.BusinessTab
import mx.petmate.app.ui.business.EditLocationScreen
import mx.petmate.app.ui.business.ScannerScreen
import mx.petmate.app.ui.business.ServiceFormScreen
import mx.petmate.app.ui.business.ServicesScreen
import mx.petmate.app.ui.home.DeleteAccountScreen
import mx.petmate.app.ui.home.HomeScreen
import mx.petmate.app.ui.home.LegalScreen
import mx.petmate.app.ui.home.ProfileActions

/**
 * Modo negocio (el vendedor): Panel, Escanear, Atendidas y Perfil.
 * Desde Perfil o el panel se regresa a "mis mascotas" con [onSwitchToOwner].
 */
@Composable
fun BusinessGraph(user: User, onSwitchToOwner: () -> Unit, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }
    val bar: @Composable (BusinessTab) -> Unit = { tab -> BusinessBottomBar(selected = tab, onTabSelected = { navController.openBusinessTab(it) }) }

    NavHost(navController = navController, startDestination = Route.BIZ_PANEL, modifier = modifier) {
        composable(Route.BIZ_PANEL) {
            BusinessPanelScreen(
                viewModel = hiltViewModel(),
                onEditClick = { navController.navigate(Route.BIZ_EDIT) },
                onServicesClick = { navController.navigate(Route.BIZ_SERVICES) },
                onScanClick = { navController.openBusinessTab(BusinessTab.Scanner) },
                onAttendedClick = { navController.openBusinessTab(BusinessTab.Attended) },
                bottomBar = { bar(BusinessTab.Panel) },
            )
        }
        composable(Route.BIZ_SCANNER) {
            ScannerScreen(viewModel = hiltViewModel(), bottomBar = { bar(BusinessTab.Scanner) })
        }
        composable(Route.BIZ_ATTENDED) {
            AttendedScreen(viewModel = hiltViewModel(), bottomBar = { bar(BusinessTab.Attended) })
        }
        composable(Route.BIZ_PROFILE_TAB) {
            HomeScreen(
                isGuest = false,
                userName = user.name,
                userEmail = user.email,
                isAffiliate = true,
                businessMode = true,
                bottomBar = { bar(BusinessTab.Profile) },
                actions = ProfileActions(
                    onPrivacyClick = { navController.navigate(Route.LEGAL) },
                    onDeleteAccountClick = { navController.navigate(Route.DELETE_ACCOUNT) },
                    onSwitchModeClick = onSwitchToOwner,
                ),
            )
        }
        composable(Route.LEGAL) { LegalScreen(onBackClick = back, isGuest = false, onCreateAccountClick = {}) }
        composable(Route.DELETE_ACCOUNT) { DeleteAccountScreen(onBackClick = back) }
        composable(Route.BIZ_EDIT) {
            BizEditScreen(viewModel = hiltViewModel(), onBackClick = back, onEditLocationClick = { navController.navigate(Route.BIZ_LOCATION) })
        }
        composable(Route.BIZ_LOCATION) { EditLocationScreen(viewModel = hiltViewModel(), onBackClick = back) }
        composable(Route.BIZ_SERVICES) {
            ServicesScreen(
                viewModel = hiltViewModel(),
                onBackClick = back,
                onAddClick = { navController.navigate(Route.serviceForm()) },
                onEditClick = { navController.navigate(Route.serviceForm(it)) },
            )
        }
        composable(
            Route.BIZ_SERVICE_FORM,
            arguments = listOf(navArgument(Route.ARG_SERVICE_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            ServiceFormScreen(viewModel = hiltViewModel(), serviceId = entry.arguments?.getString(Route.ARG_SERVICE_ID), onBackClick = back)
        }
    }
}

private fun NavHostController.openBusinessTab(tab: BusinessTab) {
    val route = when (tab) {
        BusinessTab.Panel -> {
            popBackStack(Route.BIZ_PANEL, inclusive = false)
            return
        }
        BusinessTab.Scanner -> Route.BIZ_SCANNER
        BusinessTab.Attended -> Route.BIZ_ATTENDED
        BusinessTab.Profile -> Route.BIZ_PROFILE_TAB
    }
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(Route.BIZ_PANEL)
        launchSingleTop = true
    }
}
