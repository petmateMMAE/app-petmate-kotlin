package mx.petmate.app.ui.map

import android.content.Context
import mx.petmate.app.BuildConfig
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Configuracion de osmdroid y de las teselas de OpenStreetMap.
 *
 * La politica de uso de OSM (https://operations.osmfoundation.org/policies/tiles/)
 * exige tres cosas que se cumplen aqui:
 *
 * 1. Un User-Agent propio y estable que identifique a la aplicacion. El
 *    generico de la libreria esta prohibido y el trafico se bloquea.
 * 2. Cachear cada tesela al menos 7 dias.
 * 3. Nada de descargas masivas ni de uso sin conexion con estas teselas.
 *
 * La atribucion visible la pinta [OsmMap]; no se puede ocultar.
 */
object OsmMapConfig {

    /** Centro por defecto cuando no hay permiso de ubicacion: Ciudad de Mexico. */
    const val DEFAULT_LATITUDE = 19.4326
    const val DEFAULT_LONGITUDE = -99.1332
    const val DEFAULT_ZOOM = 14.0

    const val MIN_ZOOM = 4.0
    const val MAX_ZOOM = 19.0

    /** Texto obligatorio de atribucion. */
    const val ATTRIBUTION = "© OpenStreetMap contributors"

    private const val CACHE_DIR = "osmdroid"
    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    private var initialized = false

    /**
     * Prepara osmdroid. Se llama una vez antes de mostrar el primer mapa.
     *
     * El User-Agent identifica a la aplicacion por su identificador y version,
     * de modo que OSM pueda distinguir su trafico.
     */
    fun initialize(context: Context) {
        if (initialized) return
        val config = Configuration.getInstance()

        config.userAgentValue = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"

        val cacheRoot = File(context.cacheDir, CACHE_DIR)
        config.osmdroidBasePath = cacheRoot
        config.osmdroidTileCache = File(cacheRoot, "tiles")

        // La politica pide conservar cada tesela al menos siete dias.
        config.expirationExtendedDuration = SEVEN_DAYS_MS

        initialized = true
    }
}
