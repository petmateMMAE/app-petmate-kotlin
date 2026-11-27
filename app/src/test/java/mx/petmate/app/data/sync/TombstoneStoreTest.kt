package mx.petmate.app.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mx.petmate.app.core.coroutines.DispatcherProvider
import mx.petmate.app.domain.sync.SyncEntityRef
import mx.petmate.app.domain.sync.SyncEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lo borrado sin red no reaparece al sincronizar (spec 005, T-007; C-08, R-09).
 *
 * El caso que da nombre a la tarea es `una lectura del servidor no resucita lo borrado`: es el
 * unico que demuestra C-08, y los demas protegen las piezas que lo hacen posible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TombstoneStoreTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val io = dispatcher
        override val default = dispatcher
        override val main = dispatcher
    }
    private val dao = InMemoryTombstoneDao()
    private var ahora = 1_000L
    private val store = TombstoneStore(dao, dispatchers, now = { ahora })

    private val firulais = SyncEntityRef(SyncEntityType.Pet, "p-1")
    private val otra = SyncEntityRef(SyncEntityType.Pet, "p-2")

    /** Lo que traeria una lectura del servidor: la mascota borrada sigue ahi. */
    private data class PetDto(val id: String, val nombre: String)

    @Test
    fun `una lectura del servidor no resucita lo borrado`() = runTest(dispatcher) {
        store.remember(firulais)

        // El servidor todavia no procesa el borrado (fue sin red) y devuelve las dos mascotas.
        val delServidor = listOf(PetDto("p-1", "Firulais"), PetDto("p-2", "Michi"))
        val aGuardar = store.filterDeleted(SyncEntityType.Pet, delServidor) { it.id }

        assertEquals(listOf(PetDto("p-2", "Michi")), aGuardar)
    }

    @Test
    fun `borrar deja lapida y una lectura posterior no lo devuelve`() = runTest(dispatcher) {
        assertFalse(store.isDeleted(firulais))

        store.remember(firulais)

        assertTrue(store.isDeleted(firulais))
        assertFalse("la otra mascota no se toca", store.isDeleted(otra))
    }

    @Test
    fun `borrar dos veces no deja dos lapidas`() = runTest(dispatcher) {
        store.remember(firulais)
        ahora = 2_000L
        store.remember(firulais)

        assertEquals(1, dao.size)
    }

    @Test
    fun `la lapida sigue tapando despues de que el servidor confirma`() = runTest(dispatcher) {
        // Es la parte contraintuitiva y por eso tiene prueba: confirmar NO borra la lapida. Una
        // respuesta que salio antes del borrado puede llegar despues y volveria a meter el dato.
        store.remember(firulais)
        store.confirm(firulais)

        assertTrue(store.isDeleted(firulais))
        assertEquals("ya no hay nada que reenviar", emptyList<SyncEntityRef>(), store.pending())
    }

    @Test
    fun `lo que el servidor no confirma queda pendiente de reenviar`() = runTest(dispatcher) {
        store.remember(firulais)
        store.remember(otra)
        store.confirm(firulais)

        assertEquals(listOf(otra), store.pending())
    }

    @Test
    fun `la limpieza solo se lleva lapidas confirmadas y viejas`() = runTest(dispatcher) {
        store.remember(firulais)
        store.confirm(firulais)
        store.remember(otra)
        store.confirm(otra)

        // Todavia no pasa la retencion: no se limpia nada.
        ahora += TombstoneStore.RETENCION_POR_OMISION / 2
        assertEquals(0, store.pruneConfirmed())

        // Ya paso: se van las dos.
        ahora += TombstoneStore.RETENCION_POR_OMISION
        assertEquals(2, store.pruneConfirmed())
        assertFalse("limpiada la lapida, el elemento ya puede volver a existir", store.isDeleted(firulais))
    }

    @Test
    fun `una lista vacia no consulta nada`() = runTest(dispatcher) {
        store.remember(firulais)

        assertEquals(emptyList<PetDto>(), store.filterDeleted(SyncEntityType.Pet, emptyList<PetDto>()) { it.id })
    }

    @Test
    fun `la lapida de una mascota no tapa una vacuna con el mismo id`() = runTest(dispatcher) {
        // Los ids los genera el cliente por tipo, asi que un choque entre tipos es posible.
        store.remember(SyncEntityRef(SyncEntityType.Pet, "x-1"))

        assertFalse(store.isDeleted(SyncEntityRef(SyncEntityType.Vaccine, "x-1")))
    }
}
