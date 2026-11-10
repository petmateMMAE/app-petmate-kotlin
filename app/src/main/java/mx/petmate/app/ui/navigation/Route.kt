package mx.petmate.app.ui.navigation

/** Rutas del grafo de navegacion. */
object Route {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAP = "map"

    /** Perfil (pestana Perfil, spec 017). */
    const val HOME = "home"

    /** Privacidad y terminos, desde Perfil. */
    const val LEGAL = "legal"

    /** Eliminar mi cuenta, desde Perfil. */
    const val DELETE_ACCOUNT = "delete_account"

    // ---------- Mascotas y carnet (specs 007, 008 y 009) ----------
    const val ARG_PET_ID = "petId"
    const val ARG_ALLERGY_ID = "allergyId"

    const val PETS = "pets"
    const val PET_FORM = "pet_form?$ARG_PET_ID={$ARG_PET_ID}"
    const val CARNET = "carnet/{$ARG_PET_ID}"
    const val QR = "qr/{$ARG_PET_ID}"
    const val VACCINES = "vaccines/{$ARG_PET_ID}"
    const val VACCINE_FORM = "vaccine_form/{$ARG_PET_ID}"
    const val DEWORM = "deworm/{$ARG_PET_ID}"
    const val DEWORM_FORM = "deworm_form/{$ARG_PET_ID}"
    const val WEIGHT = "weight/{$ARG_PET_ID}"
    const val ALLERGIES = "allergies/{$ARG_PET_ID}"
    const val ALLERGY_FORM = "allergy_form/{$ARG_PET_ID}?$ARG_ALLERGY_ID={$ARG_ALLERGY_ID}"

    fun petForm(petId: String? = null) = if (petId == null) "pet_form" else "pet_form?$ARG_PET_ID=$petId"
    fun carnet(petId: String) = "carnet/$petId"
    fun qr(petId: String) = "qr/$petId"
    fun vaccines(petId: String) = "vaccines/$petId"
    fun vaccineForm(petId: String) = "vaccine_form/$petId"
    fun deworm(petId: String) = "deworm/$petId"
    fun dewormForm(petId: String) = "deworm_form/$petId"
    fun weight(petId: String) = "weight/$petId"
    fun allergies(petId: String) = "allergies/$petId"
    fun allergyForm(petId: String, allergyId: String? = null) =
        if (allergyId == null) "allergy_form/$petId" else "allergy_form/$petId?$ARG_ALLERGY_ID=$allergyId"
}
