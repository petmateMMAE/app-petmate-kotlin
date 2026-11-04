package mx.petmate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores de la marca que no tienen lugar en el ColorScheme de Material 3.
 *
 * Se leen con `MaterialTheme.petMateColors` y cambian solos entre claro y
 * oscuro, igual que los de Material.
 */
@Immutable
data class PetMateColors(
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val surface2: Color,
    val brandSoft: Color,
    /** Color de marca para texto sobre fondo oscuro (enlaces, pestaña activa). */
    val brandText: Color,
    val sun: Color,
    val sunInk: Color,
    val sunSoft: Color,
    val onSun: Color,
    val ok: Color,
    val okSoft: Color,
    val alert: Color,
    val alertSoft: Color,
    val vet: Color,
    val vetSoft: Color,
    val shop: Color,
    val shopSoft: Color,
    val spa: Color,
    val spaSoft: Color,
    val pinVet: Color,
    val pinShop: Color,
    val pinSpa: Color,
    val userLocation: Color,
    /** Borde e icono blancos de los marcadores y del punto "estas aqui". */
    val onPin: Color,
    val whatsApp: Color,
    val whatsAppInk: Color,
    val splash: Color,
    val onSplash: Color,
    val mapLand: Color,
    val mapBlock: Color,
    val mapRoad: Color,
    val mapRoadEdge: Color,
    val mapPark: Color,
    val mapTree: Color,
    val mapWater: Color,
    val mapLabel: Color,
    val qrInk: Color,
    val qrBackground: Color,
)

internal val LightPetMateColors = PetMateColors(
    ink2 = LightInk2,
    ink3 = LightInk3,
    line = LightLine,
    surface2 = LightSurface2,
    brandSoft = LightBrandSoft,
    brandText = LightBrand,
    sun = LightSun,
    sunInk = LightSunInk,
    sunSoft = LightSunSoft,
    onSun = OnSun,
    ok = LightOk,
    okSoft = LightOkSoft,
    alert = LightAlert,
    alertSoft = LightAlertSoft,
    vet = LightOk,
    vetSoft = LightOkSoft,
    shop = LightShop,
    shopSoft = LightShopSoft,
    spa = LightSpa,
    spaSoft = LightSpaSoft,
    pinVet = PinVet,
    pinShop = PinShop,
    pinSpa = PinSpa,
    userLocation = UserLocation,
    onPin = White,
    whatsApp = WhatsAppLight,
    whatsAppInk = WhatsAppInk,
    splash = LightSplash,
    onSplash = White,
    mapLand = LightMapLand,
    mapBlock = LightMapBlock,
    mapRoad = LightMapRoad,
    mapRoadEdge = LightMapRoadEdge,
    mapPark = LightMapPark,
    mapTree = LightMapTree,
    mapWater = LightMapWater,
    mapLabel = LightMapLabel,
    qrInk = QrInk,
    qrBackground = QrBackground,
)

internal val DarkPetMateColors = PetMateColors(
    ink2 = DarkInk2,
    ink3 = DarkInk3,
    line = DarkLine,
    surface2 = DarkSurface2,
    brandSoft = DarkBrandSoft,
    brandText = DarkBrand2,
    sun = DarkSun,
    sunInk = DarkSunInk,
    sunSoft = DarkSunSoft,
    onSun = OnSun,
    ok = DarkOk,
    okSoft = DarkOkSoft,
    alert = DarkAlert,
    alertSoft = DarkAlertSoft,
    vet = DarkOk,
    vetSoft = DarkOkSoft,
    shop = DarkShop,
    shopSoft = DarkShopSoft,
    spa = DarkSpa,
    spaSoft = DarkSpaSoft,
    pinVet = PinVet,
    pinShop = PinShop,
    pinSpa = PinSpa,
    userLocation = UserLocation,
    onPin = White,
    // En la maqueta el boton de WhatsApp usa el mismo verde claro en ambos temas.
    whatsApp = WhatsAppLight,
    whatsAppInk = WhatsAppInk,
    splash = DarkSplash,
    onSplash = White,
    mapLand = DarkMapLand,
    mapBlock = DarkMapBlock,
    mapRoad = DarkMapRoad,
    mapRoadEdge = DarkMapRoadEdge,
    mapPark = DarkMapPark,
    mapTree = DarkMapTree,
    mapWater = DarkMapWater,
    mapLabel = DarkMapLabel,
    qrInk = QrInk,
    qrBackground = QrBackground,
)

internal val LocalPetMateColors = staticCompositionLocalOf { LightPetMateColors }

/** Colores extra de PetMate para el tema actual. */
val MaterialTheme.petMateColors: PetMateColors
    @Composable
    @ReadOnlyComposable
    get() = LocalPetMateColors.current
