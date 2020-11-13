package com.ampnet.projectservice.persistence

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class LocalDateTimeConverter : AttributeConverter<LocalDateTime, Timestamp> {

    override fun convertToDatabaseColumn(attribute: LocalDateTime): Timestamp =
        Timestamp.valueOf(attribute)

    override fun convertToEntityAttribute(sqlTimestamp: Timestamp): LocalDateTime =
        sqlTimestamp.toLocalDateTime()
}
