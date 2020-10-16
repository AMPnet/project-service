package com.ampnet.projectservice.persistence

import com.ampnet.projectservice.enums.OrganizationRole
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class OrganizationRoleConverter : AttributeConverter<OrganizationRole, Int> {

    override fun convertToDatabaseColumn(attribute: OrganizationRole): Int =
        attribute.id

    override fun convertToEntityAttribute(dbData: Int): OrganizationRole? =
        OrganizationRole.fromInt(dbData)
}
