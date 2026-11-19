package mx.petmate.app.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Fechas en la base como milisegundos UTC (spec 001, T-011).
 *
 * `java.util.Date` y no `java.time`: minSdk 24 no trae `java.time` sin desugaring.
 */
class DateConverters {

    @TypeConverter
    fun fromMillis(value: Long?): Date? = value?.let(::Date)

    @TypeConverter
    fun toMillis(date: Date?): Long? = date?.time
}
