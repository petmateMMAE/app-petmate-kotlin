package mx.petmate.app.core.time

/**
 * El reloj, inyectable (spec 001, T-018).
 *
 * Existe para una cosa: que una prueba pueda **adelantar el tiempo** sin esperarlo. Sin esto, una
 * prueba de vigencia de caché tendría que dormir de verdad, y una de "la compartición venció a los
 * 30 minutos" sería imposible de escribir.
 *
 * Es `fun interface` a propósito: en una prueba se escribe `Clock { 1_000L }` y ya, sin clase
 * anónima. Y como `Clock::nowMillis` es un `() -> Long`, encaja con el patrón que ya usan
 * `OfflineFirstPetRepository` y `ConsultationDraftRepositoryImpl`
 * (`now: () -> Long = System::currentTimeMillis`): esos dos pueden recibir `clock::nowMillis` el día
 * que alguien los toque, sin reescribir nada hoy. No se cambiaron en esta tarea para no mover
 * código ajeno que ya está en verde y probado.
 *
 * Milisegundos desde el epoch, en UTC. No lleva zona horaria: lo que se compara son instantes, y
 * meter zonas aquí solo invita a que alguien calcule fechas de calendario con esto. Para eso está
 * `PetDates`, que trabaja con texto `yyyy-MM-dd`.
 */
fun interface Clock {

    fun nowMillis(): Long

    companion object {

        /** El reloj del sistema. Es el que se inyecta en la app. */
        val SYSTEM: Clock = Clock { System.currentTimeMillis() }

        /**
         * Un reloj fijo, para pruebas y previsualizaciones.
         *
         * Está aquí y no en `test/` porque las previsualizaciones de Compose también lo necesitan,
         * y están en `main`.
         */
        fun fixed(millis: Long): Clock = Clock { millis }
    }
}
