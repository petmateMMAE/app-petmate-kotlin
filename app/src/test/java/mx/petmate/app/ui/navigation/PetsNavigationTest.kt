package mx.petmate.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rutas de mascotas (spec 007, T-010, criterio C-11).
 *
 * Las rutas no se escriben a mano en las pantallas: se construyen con las
 * funciones de `Route`, que es lo que evita un destino mal formado. Aqui se
 * comprueba que lo que construyen encaja con el patron que declararon, porque
 * un patron y su constructora que se separan dan un fallo de navegacion que no
 * se ve hasta que el usuario toca el boton.
 */
class PetsNavigationTest {

    /** Convierte el patron declarado en lo que deberia producir la constructora. */
    private fun fill(pattern: String, vararg args: Pair<String, String>): String {
        var filled = pattern
        args.forEach { (name, value) -> filled = filled.replace("{$name}", value) }
        return filled
    }

    @Test
    fun el_alta_sin_mascota_no_arrastra_el_argumento_vacio() {
        // Un "pet_form?petId=" suelto llega al formulario como id en blanco, y
        // entonces el alta se cree una edicion.
        assertEquals("pet_form", Route.petForm())
        assertFalse(Route.petForm().contains("?"))
    }

    @Test
    fun la_edicion_lleva_el_identificador_de_la_mascota() {
        assertEquals("pet_form?petId=p1", Route.petForm("p1"))
    }

    @Test
    fun la_ruta_de_edicion_encaja_con_su_patron() {
        assertEquals(
            fill(Route.PET_FORM, Route.ARG_PET_ID to "p1"),
            Route.petForm("p1"),
        )
    }

    @Test
    fun la_ruta_del_carnet_encaja_con_su_patron() {
        assertEquals(
            fill(Route.CARNET, Route.ARG_PET_ID to "p1"),
            Route.carnet("p1"),
        )
    }

    @Test
    fun la_lista_de_mascotas_no_lleva_argumentos() {
        assertEquals("pets", Route.PETS)
        assertFalse(Route.PETS.contains("{"))
    }

    @Test
    fun cada_ruta_de_una_mascota_usa_el_mismo_nombre_de_argumento() {
        // Si una ruta lo llamara "pet_id" y otra "petId", el argumento llegaria
        // nulo sin que nada avise.
        val withPetId = listOf(
            Route.PET_FORM,
            Route.CARNET,
            Route.QR,
            Route.VACCINES,
            Route.VACCINE_FORM,
            Route.DEWORM,
            Route.DEWORM_FORM,
            Route.WEIGHT,
            Route.ALLERGIES,
            Route.ALLERGY_FORM,
            Route.HISTORY,
            Route.SHARE,
        )

        withPetId.forEach { pattern ->
            assertTrue(
                "la ruta $pattern deberia declarar {${Route.ARG_PET_ID}}",
                pattern.contains("{${Route.ARG_PET_ID}}"),
            )
        }
    }

    @Test
    fun las_rutas_construidas_no_dejan_marcadores_sin_rellenar() {
        val built = listOf(
            Route.petForm("p1"),
            Route.carnet("p1"),
            Route.qr("p1"),
            Route.vaccines("p1"),
            Route.vaccineForm("p1"),
            Route.deworm("p1"),
            Route.dewormForm("p1"),
            Route.weight("p1"),
            Route.allergies("p1"),
            Route.allergyForm("p1"),
            Route.allergyForm("p1", "a1"),
            Route.history("p1"),
            Route.share("p1"),
        )

        built.forEach { route ->
            assertFalse("la ruta $route quedo con un marcador sin rellenar", route.contains("{"))
            assertTrue("la ruta $route deberia llevar el id", route.contains("p1"))
        }
    }

    @Test
    fun la_alergia_opcional_solo_aparece_cuando_se_da() {
        assertEquals("allergy_form/p1", Route.allergyForm("p1"))
        assertEquals("allergy_form/p1?allergyId=a1", Route.allergyForm("p1", "a1"))
    }

    @Test
    fun ninguna_ruta_del_grafo_se_repite() {
        // Dos destinos con la misma ruta hacen que Navigation se queje en
        // tiempo de ejecucion, no al compilar.
        val routes = listOf(
            Route.SPLASH, Route.WELCOME, Route.LOGIN, Route.REGISTER, Route.MAP,
            Route.HOME, Route.LEGAL, Route.DELETE_ACCOUNT,
            Route.PETS, Route.PET_FORM, Route.CARNET, Route.QR,
            Route.VACCINES, Route.VACCINE_FORM, Route.DEWORM, Route.DEWORM_FORM,
            Route.WEIGHT, Route.ALLERGIES, Route.ALLERGY_FORM,
            Route.AFFILIATE, Route.BIZ_PROFILE, Route.SHARE, Route.BIZ_PANEL,
            Route.BIZ_SCANNER, Route.BIZ_ATTENDED, Route.BIZ_PROFILE_TAB,
            Route.BIZ_EDIT, Route.BIZ_LOCATION, Route.BIZ_SERVICES, Route.BIZ_SERVICE_FORM,
            Route.HISTORY, Route.CONSULTATION,
        )

        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun el_andamio_de_verificacion_de_001_ya_no_es_una_ruta() {
        // C-11: la pantalla de verificacion era andamio de 001 y no debe quedar
        // ninguna forma de llegar a ella.
        val names = Route::class.java.declaredFields.map { it.name.lowercase() }

        assertTrue(
            "quedo una ruta de verificacion: $names",
            names.none { it.contains("verif") || it.contains("check") },
        )
    }
}
