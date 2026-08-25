package com.idoelbak.tracker.data.db

import androidx.room.TypeConverter
import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.model.ScheduleType
import java.time.LocalDate

/**
 * Dates are stored as epoch days -- a plain integer that sorts and range-queries correctly in SQL,
 * with no timezone attached. A tracked day is a calendar day, not an instant.
 */
class Converters {

    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun scheduleTypeToName(type: ScheduleType): String = type.name

    @TypeConverter
    fun nameToScheduleType(name: String): ScheduleType = ScheduleType.valueOf(name)

    @TypeConverter
    fun verdictToName(verdict: DayVerdict): String = verdict.name

    @TypeConverter
    fun nameToVerdict(name: String): DayVerdict = DayVerdict.valueOf(name)
}
