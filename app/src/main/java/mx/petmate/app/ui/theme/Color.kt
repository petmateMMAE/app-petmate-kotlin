package mx.petmate.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Paleta de PetMate, copiada de la maqueta aprobada (PetMate_App.html).
 * Estos valores solo se usan dentro de ui/theme: las pantallas leen colores
 * desde MaterialTheme.colorScheme o MaterialTheme.petMateColors.
 */

// ---------- Claro ----------
internal val LightBg = Color(0xFFF4F1FD)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurface2 = Color(0xFFEEEAFB)
internal val LightInk = Color(0xFF1D1733)
internal val LightInk2 = Color(0xFF5E5878)
internal val LightInk3 = Color(0xFF8E88A8)
internal val LightLine = Color(0xFFE2DDF2)
internal val LightBrand = Color(0xFF5B3CC4)
internal val LightBrand2 = Color(0xFF4A2FA8)
internal val LightOnBrand = Color(0xFFFFFFFF)
internal val LightBrandSoft = Color(0xFFE8E1FF)
internal val LightSun = Color(0xFFFFC53D)
internal val LightSunInk = Color(0xFF5A3E00)
internal val LightSunSoft = Color(0xFFFFF2CC)
internal val LightOk = Color(0xFF1A8F5A)
internal val LightOkSoft = Color(0xFFD8F3E6)
internal val LightAlert = Color(0xFFD93D42)
internal val LightAlertSoft = Color(0xFFFFE4E4)
internal val LightShop = Color(0xFF2F74E0)
internal val LightShopSoft = Color(0xFFDCE9FF)
internal val LightSpa = Color(0xFFD63E93)
internal val LightSpaSoft = Color(0xFFFFE0F0)
internal val LightMapLand = Color(0xFFEFEBE1)
internal val LightMapBlock = Color(0xFFE3DDCF)
internal val LightMapRoad = Color(0xFFFFFFFF)
internal val LightMapRoadEdge = Color(0xFFD9D2C2)
internal val LightMapPark = Color(0xFFC9E8C1)
internal val LightMapTree = Color(0xFF9CCF8F)
internal val LightMapWater = Color(0xFFBFDDF7)
internal val LightMapLabel = Color(0xFF8A8170)
internal val LightSplash = Color(0xFF5B3CC4)

// ---------- Oscuro ----------
internal val DarkBg = Color(0xFF13101F)
internal val DarkSurface = Color(0xFF1D1830)
internal val DarkSurface2 = Color(0xFF272042)
internal val DarkInk = Color(0xFFF2EFFF)
internal val DarkInk2 = Color(0xFFB4ADD2)
internal val DarkInk3 = Color(0xFF7F78A0)
internal val DarkLine = Color(0xFF332B53)
internal val DarkBrand = Color(0xFF8F75FF)
internal val DarkBrand2 = Color(0xFFA08AFF)
internal val DarkOnBrand = Color(0xFF14102A)
internal val DarkBrandSoft = Color(0xFF2E2560)
internal val DarkSun = Color(0xFFFFCD55)
internal val DarkSunInk = Color(0xFFFFE7A6)
internal val DarkSunSoft = Color(0xFF3A3020)
internal val DarkOk = Color(0xFF43D193)
internal val DarkOkSoft = Color(0xFF163A2A)
internal val DarkAlert = Color(0xFFFF6D72)
internal val DarkAlertSoft = Color(0xFF45201F)
internal val DarkShop = Color(0xFF6BA6FF)
internal val DarkShopSoft = Color(0xFF1B2C4D)
internal val DarkSpa = Color(0xFFFF74BE)
internal val DarkSpaSoft = Color(0xFF46203A)
internal val DarkMapLand = Color(0xFF1E1A2C)
internal val DarkMapBlock = Color(0xFF262139)
internal val DarkMapRoad = Color(0xFF3A3355)
internal val DarkMapRoadEdge = Color(0xFF2C2644)
internal val DarkMapPark = Color(0xFF1D3A2B)
internal val DarkMapTree = Color(0xFF275038)
internal val DarkMapWater = Color(0xFF1A3150)
internal val DarkMapLabel = Color(0xFF8A83A8)
internal val DarkSplash = Color(0xFF2A1F66)

// ---------- Iguales en ambos temas ----------
/** Color de los marcadores del mapa: la maqueta los deja fijos en los dos temas. */
internal val PinVet = Color(0xFF1A8F5A)
internal val PinShop = Color(0xFF2F74E0)
internal val PinSpa = Color(0xFFD63E93)
internal val UserLocation = Color(0xFF2F74E0)
internal val OnSun = Color(0xFF3A2800)
internal val WhatsAppLight = Color(0xFFDCF8C6)
internal val WhatsAppInk = Color(0xFF128C4A)
internal val White = Color(0xFFFFFFFF)

/** Un QR siempre se dibuja oscuro sobre blanco para que se pueda escanear. */
internal val QrInk = Color(0xFF1D1733)
internal val QrBackground = Color(0xFFFFFFFF)
