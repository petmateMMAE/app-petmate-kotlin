package mx.petmate.app.data.moderation

import java.io.File

/**
 * Guarda un archivo mientras se decide si puede subirse (spec 004, T-006).
 *
 * Existe para que el temporal tenga dueno. Sin esto, una imagen rechazada se queda en la cache del telefono
 * sin que nadie la borre: el usuario cree que no se guardo nada y el archivo sigue ahi, legible por cualquier
 * cosa que lea el directorio de cache.
 *
 * El contrato es simple a proposito: se escribe, se lee y se borra. Quien lo use tiene que borrar SIEMPRE,
 * pase lo que pase con la moderacion, y por eso [ImageModerator] lo hace en un `finally`.
 */
interface TemporaryFileStore {
    /** Escribe los bytes en un archivo temporal y devuelve su ruta. */
    fun write(bytes: ByteArray, extension: String): File

    /** Borra el archivo. No falla si ya no existe: borrar dos veces es correcto. */
    fun delete(file: File): Boolean

    /** Cuantos temporales quedan. Sirve para comprobar en pruebas que no se acumulan. */
    fun count(): Int
}

/** Implementacion sobre un directorio del telefono, normalmente `context.cacheDir`. */
class FileSystemTemporaryStore(private val directory: File) : TemporaryFileStore {

    override fun write(bytes: ByteArray, extension: String): File {
        if (!directory.exists()) directory.mkdirs()
        val file = File.createTempFile("pm_upload_", ".$extension", directory)
        file.writeBytes(bytes)
        return file
    }

    override fun delete(file: File): Boolean = !file.exists() || file.delete()

    override fun count(): Int = directory.listFiles { f -> f.name.startsWith("pm_upload_") }?.size ?: 0
}
