package mx.petmate.app.data.repository

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mx.petmate.app.core.result.AppError
import mx.petmate.app.core.result.AppResult
import mx.petmate.app.data.local.dao.AffiliationDraftDao
import mx.petmate.app.data.local.entity.AffiliationDraftEntity
import mx.petmate.app.data.remote.AuthorizedCaller
import mx.petmate.app.data.remote.BusinessApiService
import mx.petmate.app.data.session.SessionStore
import mx.petmate.app.domain.model.AffiliationDraft
import mx.petmate.app.domain.model.AffiliationStep
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.DraftedService
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.model.User
import mx.petmate.app.util.testDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Borrador de afiliacion: guardado, mapeo sin perdida y limpieza al cambiar de
 * sesion (spec 011, T-002, T-006 y T-008, C-13).
 *
 * El DAO se reemplaza por uno en memoria: lo que se prueba aqui es la regla de
 * aislamiento, no SQLite. La base de verdad se prueba en `AffiliationDraftDaoTest`
 * (instrumentada).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BusinessRepositoryDraftTest {

    /** DAO en memoria que respeta las mismas reglas de `ownerId` que el de Room. */
    // internal, no private: BusinessRepositoryImplTest tambien lo necesita desde que el repositorio pide
    // un DAO de borrador. Tener un solo doble evita que dos pruebas se desincronicen del DAO real.
    internal class FakeAffiliationDraftDao : AffiliationDraftDao {
        val rows = mutableMapOf<String, AffiliationDraftEntity>()

        override suspend fun get(ownerId: String): AffiliationDraftEntity? = rows[ownerId]

        override fun observe(ownerId: String): Flow<AffiliationDraftEntity?> = flowOf(rows[ownerId])

        override suspend fun upsert(draft: AffiliationDraftEntity) {
            rows[draft.ownerId] = draft
        }

        override suspend fun delete(ownerId: String) {
            rows.remove(ownerId)
        }

        override suspend fun deleteOthers(ownerId: String) {
            rows.keys.retainAll(setOf(ownerId))
        }

        override suspend fun deleteAll() = rows.clear()
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = testDispatcherProvider(dispatcher)

    private val api: BusinessApiService = mockk()
    private val caller: AuthorizedCaller = mockk()
    private val sessionStore: SessionStore = mockk()
    private val draftDao = FakeAffiliationDraftDao()

    private val repository = BusinessRepositoryImpl(
        api = api,
        caller = caller,
        sessionStore = sessionStore,
        draftDao = draftDao,
        json = Json { ignoreUnknownKeys = true },
        dispatchers = dispatchers,
    )

    private val ana = User(id = "u1", name = "Ana", email = "ana@petmate.mx")
    private val beto = User(id = "u2", name = "Beto", email = "beto@petmate.mx")

    private val draft = AffiliationDraft(
        business = AffiliationDraft.EMPTY_BUSINESS.copy(
            kind = BusinessKind.Grooming,
            name = "Estetica Luna",
            phone = "5512345678",
            whatsApp = "5598765432",
            address = "Calle 2, Centro",
            description = "Bano y corte",
            schedule = mapOf(
                "monday" to listOf(TimeRange("09:00", "14:00"), TimeRange("15:00", "18:00")),
                "saturday" to listOf(TimeRange("10:00", "14:00")),
            ),
        ),
        services = listOf(DraftedService("Bano", 250.0), DraftedService("Corte de unas", null)),
        latitude = 19.43,
        longitude = -99.13,
        step = AffiliationStep.Services,
    )

    private fun signedInAs(user: User?) {
        every { sessionStore.readUser() } returns user
    }

    @Test
    fun `guarda y devuelve el borrador completo sin perder nada`() = runTest {
        signedInAs(ana)

        assertEquals(AppResult.Success(Unit), repository.saveAffiliationDraft(draft))
        assertEquals(draft, (repository.readAffiliationDraft() as AppResult.Success).data)
    }

    @Test
    fun `sin borrador guardado devuelve nulo y no falla`() = runTest {
        signedInAs(ana)

        assertNull((repository.readAffiliationDraft() as AppResult.Success).data)
    }

    @Test
    fun `sin sesion no se puede guardar`() = runTest {
        signedInAs(null)

        val result = repository.saveAffiliationDraft(draft)

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
        assertTrue(draftDao.rows.isEmpty())
    }

    @Test
    fun `el logout borra el borrador`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        signedInAs(null)

        assertNull((repository.readAffiliationDraft() as AppResult.Success).data)
        assertTrue("no debe quedar nada guardado", draftDao.rows.isEmpty())
    }

    @Test
    fun `otro usuario en el mismo telefono no ve el borrador del anterior`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        signedInAs(beto)

        assertNull((repository.readAffiliationDraft() as AppResult.Success).data)
    }

    @Test
    fun `el borrador del anterior se borra y no reaparece al volver a entrar`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        signedInAs(beto)
        repository.readAffiliationDraft()

        signedInAs(ana)
        assertNull((repository.readAffiliationDraft() as AppResult.Success).data)
    }

    @Test
    fun `guardar con otra cuenta tira el borrador de la anterior`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        signedInAs(beto)
        repository.saveAffiliationDraft(draft.copy(step = AffiliationStep.Pin))

        assertEquals(setOf("u2"), draftDao.rows.keys)
    }

    @Test
    fun `borrar el borrador lo deja vacio`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        assertEquals(AppResult.Success(Unit), repository.clearAffiliationDraft())
        assertNull((repository.readAffiliationDraft() as AppResult.Success).data)
    }

    @Test
    fun `guardar de nuevo reemplaza el borrador anterior`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)

        val advanced = draft.copy(step = AffiliationStep.Pin, services = emptyList())
        repository.saveAffiliationDraft(advanced)

        assertEquals(advanced, (repository.readAffiliationDraft() as AppResult.Success).data)
        assertEquals(1, draftDao.rows.size)
    }

    @Test
    fun `un paso guardado que no se reconoce vuelve al primero y no tira el borrador`() = runTest {
        signedInAs(ana)
        repository.saveAffiliationDraft(draft)
        draftDao.rows["u1"] = draftDao.rows.getValue("u1").copy(step = "PasoInventado")

        val stored = (repository.readAffiliationDraft() as AppResult.Success).data

        assertEquals(AffiliationStep.Data, stored?.step)
        assertEquals("Estetica Luna", stored?.business?.name)
    }
}
