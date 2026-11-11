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

    // ---------- Negocios (specs 010 a 013; backend 0.4.0) ----------
    const val ARG_BUSINESS_ID = "businessId"
    const val ARG_SERVICE_ID = "serviceId"

    const val AFFILIATE = "affiliate"
    const val BIZ_PROFILE = "biz_profile/{$ARG_BUSINESS_ID}"
    const val SHARE = "share/{$ARG_PET_ID}"
    const val BIZ_PANEL = "biz_panel"
    const val BIZ_SCANNER = "biz_scanner"
    const val BIZ_ATTENDED = "biz_attended"
    const val BIZ_PROFILE_TAB = "biz_profile_tab"
    const val BIZ_EDIT = "biz_edit"
    const val BIZ_LOCATION = "biz_location"
    const val BIZ_SERVICES = "biz_services"
    const val BIZ_SERVICE_FORM = "biz_service_form?$ARG_SERVICE_ID={$ARG_SERVICE_ID}"

    const val ARG_CONSULTATION_ID = "consultationId"
    const val HISTORY = "history/{$ARG_PET_ID}"
    const val CONSULTATION = "consultation/{$ARG_CONSULTATION_ID}"
    fun history(petId: String) = "history/$petId"
    fun consultation(id: String) = "consultation/$id"

    fun bizProfile(businessId: String) = "biz_profile/$businessId"
    fun share(petId: String) = "share/$petId"
    fun serviceForm(serviceId: String? = null) = if (serviceId == null) "biz_service_form" else "biz_service_form?$ARG_SERVICE_ID=$serviceId"

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
