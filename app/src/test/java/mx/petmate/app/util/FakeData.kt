package mx.petmate.app.util

import mx.petmate.app.domain.model.AttendedPet
import mx.petmate.app.domain.model.BusinessKind
import mx.petmate.app.domain.model.BusinessService
import mx.petmate.app.domain.model.NearbyBusiness
import mx.petmate.app.domain.model.OwnBusiness
import mx.petmate.app.domain.model.PublicBusiness
import mx.petmate.app.domain.model.SCHEDULE_DAYS
import mx.petmate.app.domain.model.TimeRange
import mx.petmate.app.domain.model.User
import mx.petmate.app.domain.model.UserProfile

/**
 * Constructores de datos falsos (001/T-023).
 *
 * Todos los parametros tienen valor por omision a proposito: una prueba nombra **solo** el campo
 * que le importa y el resto queda razonable. Asi se lee lo que la prueba esta comprobando de
 * verdad, en vez de un constructor con seis argumentos de los cuales cinco son relleno.
 *
 * ```
 * val afiliado = usuarioFalso(isAffiliate = true)   // se ve que lo que importa es la afiliacion
 * ```
 */

fun usuarioFalso(
    id: String = ID_USUARIO,
    name: String = NOMBRE,
    email: String = CORREO,
    isAffiliate: Boolean = false,
    businessType: String? = null,
): User = User(
    id = id,
    name = name,
    email = email,
    isAffiliate = isAffiliate,
    businessType = businessType,
)

fun perfilFalso(
    id: String = ID_USUARIO,
    email: String = CORREO,
    name: String = NOMBRE,
    isAffiliate: Boolean = false,
    memberSince: String? = FECHA_ALTA,
): UserProfile = UserProfile(
    id = id,
    email = email,
    name = name,
    isAffiliate = isAffiliate,
    memberSince = memberSince,
)

// ---- Cuerpos JSON con el formato real del backend ----
// Copian docs/api/endpoints.md del backend. Van aqui y no en cada prueba para que, si el contrato
// cambia, se arregle en un solo lugar y todas las pruebas se enteren a la vez.

/** El objeto `user` que viaja dentro de la sesion (pm-086, pm-087). */
fun cuerpoDeUsuario(
    id: String = ID_USUARIO,
    email: String = CORREO,
    nombre: String? = NOMBRE,
    tipoDeNegocio: String? = null,
): String = buildString {
    append("""{"id":"$id","email":"$email"""")
    if (nombre != null) append(""","displayName":"$nombre"""")
    if (tipoDeNegocio != null) append(""","businessType":"$tipoDeNegocio"""")
    append("}")
}

/** Respuesta de inicio de sesion o registro, ya envuelta en el sobre del backend. */
fun cuerpoDeSesion(
    codigo: Int = 200,
    accessToken: String = "acc-1",
    refreshToken: String = "ref-1",
    nombre: String? = NOMBRE,
    tipoDeNegocio: String? = null,
): String = sobre(
    codigo = codigo,
    datos = """
        {"accessToken":"$accessToken","tokenType":"Bearer",
         "accessTokenExpiresAt":"$EXPIRA_ACCESO","refreshToken":"$refreshToken",
         "refreshTokenExpiresAt":"$EXPIRA_REFRESCO",
         "user":${cuerpoDeUsuario(email = CORREO, nombre = nombre, tipoDeNegocio = tipoDeNegocio)}}
    """.trimIndent().replace("\n", ""),
)

/** Respuesta de `GET users/me/get` (pm-037). */
fun cuerpoDePerfil(
    nombre: String? = NOMBRE,
    esAfiliado: Boolean = false,
): String = sobre(
    codigo = 200,
    datos = """
        {"id":"$ID_USUARIO","email":"$CORREO",
         "displayName":${if (nombre == null) "null" else "\"$nombre\""},
         "isAffiliate":$esAfiliado,"createdAt":"$FECHA_ALTA"}
    """.trimIndent().replace("\n", ""),
)

const val ID_USUARIO = "u-1"
const val NOMBRE = "Ana Lopez"
const val CORREO = "ana@petmate.mx"
const val FECHA_ALTA = "2026-11-06T17:00:00Z"
private const val EXPIRA_ACCESO = "2026-11-06T17:15:00Z"
private const val EXPIRA_REFRESCO = "2026-12-06T17:00:00Z"

// ---- Negocios (specs 010 a 013) ----

fun negocioCercanoFalso(
    id: String = "b-1",
    name: String = "Veterinaria San Jorge",
    kind: BusinessKind = BusinessKind.Veterinary,
    isOpen: Boolean = true,
    distanceMeters: Double = 450.0,
    latitude: Double? = null,
    longitude: Double? = null,
): NearbyBusiness = NearbyBusiness(
    id = id,
    name = name,
    kind = kind,
    isOpen = isOpen,
    todayHours = listOf(TimeRange("09:00", "18:00")),
    distanceMeters = distanceMeters,
    photoUrl = null,
    latitude = latitude,
    longitude = longitude,
)

fun perfilPublicoFalso(
    id: String = "b-1",
    name: String = "Veterinaria San Jorge",
    kind: BusinessKind = BusinessKind.Veterinary,
    isOpen: Boolean = true,
    services: List<BusinessService> = emptyList(),
): PublicBusiness = PublicBusiness(
    id = id,
    name = name,
    kind = kind,
    description = "Consulta general y vacunacion",
    isOpen = isOpen,
    todayHours = listOf(TimeRange("09:00", "18:00")),
    schedule = SCHEDULE_DAYS.associateWith { listOf(TimeRange("09:00", "18:00")) },
    latitude = 20.0,
    longitude = -99.0,
    address = "Calle 1 num 2",
    phone = "7711234567",
    whatsApp = null,
    services = services,
)

/**
 * Negocio propio, el que ve su dueno en "Mi negocio".
 *
 * El horario sale con las siete claves del backend porque `BusinessForm.from` se queda solo con
 * los dias que traen franja: si aqui vinieran vacios, la prueba comprobaria un formulario sin dias
 * y eso no es lo que pasa en la app.
 */
fun negocioPropioFalso(
    id: String = "b-1",
    kind: BusinessKind = BusinessKind.Veterinary,
    name: String = "Veterinaria San Jorge",
    phone: String = "7711234567",
    whatsApp: String? = null,
    address: String = "Calle 1 num 2, Pachuca",
    licenseNumber: String? = "CED-123",
    schedule: Map<String, List<TimeRange>> =
        listOf("monday", "tuesday", "wednesday").associateWith { listOf(TimeRange("09:00", "18:00")) },
): OwnBusiness = OwnBusiness(
    id = id,
    kind = kind,
    name = name,
    phone = phone,
    whatsApp = whatsApp,
    address = address,
    description = "Consulta general y vacunacion",
    latitude = 20.12,
    longitude = -98.73,
    licenseNumber = licenseNumber,
    schedule = schedule,
)

fun mascotaAtendidaFalsa(
    name: String = "Firulais",
    scannedAt: String = "2026-11-27T10:00:00Z",
    petId: String? = "p-1",
): AttendedPet = AttendedPet(name = name, scannedAt = scannedAt, petId = petId)

fun servicioFalso(
    id: String = "s-1",
    name: String = "Consulta general",
    price: Double? = 350.0,
): BusinessService = BusinessService(id = id, name = name, description = null, price = price)
