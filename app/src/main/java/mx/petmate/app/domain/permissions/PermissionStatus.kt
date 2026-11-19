package mx.petmate.app.domain.permissions

/** Estado de un permiso (spec 003, R-03). */
enum class PermissionStatus {
    /** Todavia no se pidio en este proceso. */
    NotRequested,

    Granted,

    /** El usuario lo nego, pero se le puede volver a pedir. */
    Denied,

    /** El sistema ya no muestra el dialogo: solo se puede conceder desde los ajustes de la app (R-04). */
    PermanentlyDenied,
}
