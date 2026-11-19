package mx.petmate.app.domain.permissions

import kotlinx.coroutines.flow.Flow

/**
 * Contrato para consultar y registrar permisos sin tocar las APIs de Android desde la UI (spec 003, R-13).
 *
 * El dialogo del sistema solo se puede lanzar desde una pantalla (`ActivityResult`), asi que "solicitar" se
 * parte en dos: la UI lanza el dialogo y le entrega el resultado al gateway con [onSystemResult].
 */
interface PermissionGateway {

    /** Estado actual de [type]. No hace E/S: se puede llamar desde el hilo principal (R-12). */
    fun status(type: PermissionType): PermissionStatus

    /** Estado de [type] que cambia con cada resultado y con cada [refresh]. */
    fun observe(type: PermissionType): Flow<PermissionStatus>

    /** Permisos del sistema que hay que pedir para [type]. Vacio si no hace falta ninguno. */
    fun systemPermissions(type: PermissionType): List<String>

    /** Registra la respuesta del dialogo. [canAskAgain] es falso si el sistema ya no lo volvera a mostrar. */
    fun onSystemResult(type: PermissionType, granted: Boolean, canAskAgain: Boolean)

    /** Vuelve a leer todos los permisos, por ejemplo al regresar de los ajustes de la app (spec 003, T-005). */
    fun refresh()
}
