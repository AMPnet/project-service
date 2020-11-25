package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.Organization
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationServiceResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val description: String,
    val headerImage: String?,
    val coop: String
) {
    constructor(organization: Organization) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.description.orEmpty(),
        organization.headerImage,
        organization.coop
    )
}
