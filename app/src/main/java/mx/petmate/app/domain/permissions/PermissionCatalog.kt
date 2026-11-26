package mx.petmate.app.domain.permissions

/** Momento en que una pantalla puede pedir el permiso. Ninguno es "al arrancar" ni "al iniciar sesion" (R-01). */
enum class PermissionMoment {
    /** Al abrir el mapa o pedir negocios cercanos. */
    MapOrNearby,

    /** Al escanear un QR o tomar una foto. */
    ScanOrTakePhoto,

    /** Al elegir una foto de la galeria. */
    PickPhoto,

    /** Al activar notificaciones en 006 o en los ajustes de 017. */
    EnableNotifications,
}

/** Lo que la app ofrece si el permiso se niega (R-05 a R-08). */
enum class PermissionFallback {
    /** Mapa con centro por defecto, lista y busqueda manual. */
    DefaultCenterAndList,

    /** Escribir a mano el codigo del QR. */
    ManualCode,

    /** Mascota con iniciales y opcion de reintentar. */
    Initials,

    /** Avisos dentro de la interfaz. */
    InAppNotice,
}

/** Regla de un permiso: cuando se pide, que se ofrece sin el y si de verdad pasa por el sistema. */
data class PermissionRule(
    val type: PermissionType,
    val moment: PermissionMoment,
    val fallback: PermissionFallback,
    /**
     * Falso cuando no hay dialogo del sistema. Las fotos usan el selector de fotos del sistema, que no pide
     * permiso en ninguna version (en API 24-29 llega por Google Play Services), asi que no se declara
     * `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE` (spec 003, Q-05; R-14).
     */
    val needsSystemDialog: Boolean,
)

/**
 * Catalogo unico de permisos (spec 003, A-01). Las demas unidades lo consumen sin volver a decidir el momento,
 * la justificacion ni la alternativa. Los textos viven en `strings.xml` (R-11), no aqui.
 */
object PermissionCatalog {

    val rules: List<PermissionRule> = listOf(
        PermissionRule(
            PermissionType.Location,
            PermissionMoment.MapOrNearby,
            PermissionFallback.DefaultCenterAndList,
            needsSystemDialog = true,
        ),
        PermissionRule(
            PermissionType.Camera,
            PermissionMoment.ScanOrTakePhoto,
            PermissionFallback.ManualCode,
            needsSystemDialog = true,
        ),
        PermissionRule(
            PermissionType.Photos,
            PermissionMoment.PickPhoto,
            PermissionFallback.Initials,
            needsSystemDialog = false,
        ),
        PermissionRule(
            PermissionType.Notifications,
            PermissionMoment.EnableNotifications,
            PermissionFallback.InAppNotice,
            needsSystemDialog = true,
        ),
    )

    fun ruleFor(type: PermissionType): PermissionRule = rules.first { it.type == type }
}
