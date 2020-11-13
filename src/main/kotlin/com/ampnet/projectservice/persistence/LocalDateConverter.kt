package com.ampnet.projectservice.persistence

import java.sql.Date
import java.time.LocalDate
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class LocalDateConverter : AttributeConverter<LocalDate, Date> {

    override fun convertToDatabaseColumn(attribute: LocalDate): Date =
        Date.valueOf(attribute)

    override fun convertToEntityAttribute(sqlDate: Date): LocalDate =
        sqlDate.toLocalDate()
}
