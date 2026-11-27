package mx.petmate.app.data.push

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.log.Logger
import mx.petmate.app.domain.push.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Que se muestra y que se descarta de un mensaje entrante (spec 001, T-039; spec 006, T-004 y T-005).
 *
 * Usa un DataStore real sobre una carpeta temporal, igual que `NotificationPreferencesStoreTest`:
 * la regla que se prueba es "no mostrar lo que el usuario apago", y con un store simulado se
 * probaria el simulacro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushMessageRouterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger: Logger = mockk(relaxed = true)

    private fun newStore(scope: CoroutineScope) = NotificationPreferencesStore(
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { folder.newFolder().resolve("prefs.preferences_pb") },
        ),
    )

    private fun payload(category: String, targetId: String? = null, eventKey: String? = null) = buildMap {
        put(NotificationPresenter.KEY_CATEGORY, category)
        targetId?.let { put(NotificationPresenter.KEY_TARGET_ID, it) }
        eventKey?.let { put(NotificationPresenter.KEY_EVENT_KEY, it) }
    }

    private fun <T> withRouter(block: suspend (PushMessageRouter, NotificationPreferencesStore) -> T) =
        runTest(UnconfinedTestDispatcher()) {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
            val store = newStore(scope)
            try {
                block(PushMessageRouter(store, logger), store)
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun `una categoria encendida se muestra con su targetId`() = withRouter { router, _ ->
        val route = router.route(payload("health", targetId = "pet-1"))

        val show = route as PushRoute.Show
        assertEquals(NotificationCategory.Health, show.notification.category)
        // Del payload solo sobrevive el targetId: el texto sale de strings.xml, nunca del servidor.
        assertEquals("pet-1", show.notification.targetId)
    }

    @Test
    fun `una categoria apagada no se muestra`() = withRouter { router, store ->
        // News viene apagada por defecto, pero se apaga explicito para que la prueba diga lo que
        // prueba y no dependa del valor por omision.
        store.set(NotificationCategory.News, false)

        val route = router.route(payload("news"))

        assertEquals(PushRoute.Ignore(IgnoreReason.CategoryDisabled), route)
    }

    @Test
    fun `seguridad se muestra aunque se intente apagar`() = withRouter { router, store ->
        // A-09: la seguridad no se puede apagar, y NotificationPreferences lo impide por
        // construccion. Esto comprueba que el router respeta esa regla.
        store.set(NotificationCategory.Security, false)

        val route = router.route(payload("security"))

        assertTrue("el aviso de seguridad nunca se calla", route is PushRoute.Show)
    }

    @Test
    fun `una categoria desconocida se descarta`() = withRouter { router, _ ->
        // Puede venir de una version mas nueva de la app: mejor nada que un aviso sin destino.
        val route = router.route(payload("cumpleanos-del-perro"))

        assertEquals(PushRoute.Ignore(IgnoreReason.UnknownCategory), route)
    }

    @Test
    fun `el mismo eventKey no se muestra dos veces`() = withRouter { router, _ ->
        // FCM entrega "al menos una vez", no "exactamente una".
        val first = router.route(payload("chat", eventKey = "evt-9"))
        val second = router.route(payload("chat", eventKey = "evt-9"))

        assertTrue(first is PushRoute.Show)
        assertEquals(PushRoute.Ignore(IgnoreReason.AlreadyShown), second)
    }

    @Test
    fun `sin eventKey no se deduplica`() = withRouter { router, _ ->
        // Dos mensajes de chat distintos sin clave son dos avisos legitimos.
        assertTrue(router.route(payload("chat")) is PushRoute.Show)
        assertTrue(router.route(payload("chat")) is PushRoute.Show)
    }
}
