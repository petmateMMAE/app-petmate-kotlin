package mx.petmate.app.ui.map

import mx.petmate.app.R

/**
 * Negocios de ejemplo de la maqueta, solo para previews y para la pantalla
 * mientras no exista su ViewModel. No se usan como datos reales.
 */
internal object MapSampleData {

    val businesses: List<MapBusiness> = listOf(
        MapBusiness(
            id = "b1",
            name = "Veterinaria San Marcos",
            type = BusinessType.Vet,
            latitude = 19.434400,
            longitude = -99.120600,
            distance = "600 m",
            isOpen = true,
            scheduleTime = "20:00",
            phone = "771 123 4567",
            hasWhatsApp = true,
            photo = R.drawable.sample_business_vet,
        ),
        MapBusiness(
            id = "b2",
            name = "Patitas Felices",
            type = BusinessType.Spa,
            latitude = 19.403800,
            longitude = -99.145800,
            distance = "1.1 km",
            isOpen = true,
            scheduleTime = "19:00",
            phone = "771 222 0190",
            hasWhatsApp = true,
            photo = R.drawable.sample_business_spa,
        ),
        MapBusiness(
            id = "b3",
            name = "Croquetas El Sabueso",
            type = BusinessType.Shop,
            latitude = 19.402000,
            longitude = -99.108000,
            distance = "900 m",
            isOpen = true,
            scheduleTime = "21:00",
            phone = "771 300 4411",
            hasWhatsApp = true,
            photo = R.drawable.sample_business_shop,
        ),
        MapBusiness(
            id = "b4",
            photo = R.drawable.sample_business_b4,
            name = "Hospital Veterinario Tula",
            type = BusinessType.Vet,
            latitude = 19.459600,
            longitude = -99.088200,
            distance = "2.3 km",
            isOpen = true,
            scheduleTime = null,
            phone = "771 555 0001",
            hasWhatsApp = true,
        ),
        MapBusiness(
            id = "b5",
            photo = R.drawable.sample_business_b5,
            name = "Guau Spa",
            type = BusinessType.Spa,
            latitude = 19.438000,
            longitude = -99.165600,
            distance = "1.8 km",
            isOpen = false,
            scheduleTime = "10:00",
            phone = "771 410 7788",
            hasWhatsApp = false,
        ),
        MapBusiness(
            id = "b6",
            photo = R.drawable.sample_business_b6,
            name = "Mundo Mascota",
            type = BusinessType.Shop,
            latitude = 19.461400,
            longitude = -99.151200,
            distance = "1.5 km",
            isOpen = true,
            scheduleTime = "20:30",
            phone = "771 610 2233",
            hasWhatsApp = true,
        ),
        MapBusiness(
            id = "b7",
            photo = R.drawable.sample_business_b7,
            name = "Clínica Huellitas",
            type = BusinessType.Vet,
            latitude = 19.369600,
            longitude = -99.095400,
            distance = "3.0 km",
            isOpen = false,
            scheduleTime = "9:00",
            phone = "771 700 8899",
            hasWhatsApp = false,
        ),
    )

    val state = MapUiState(
        businesses = businesses,
        remindersCount = 3,
    )
}
