package mx.petmate.app.domain.permissions

/** Permisos que la app puede llegar a pedir (spec 003, A-01). */
enum class PermissionType {
    Location,
    Camera,
    Photos,
    Notifications,
}
