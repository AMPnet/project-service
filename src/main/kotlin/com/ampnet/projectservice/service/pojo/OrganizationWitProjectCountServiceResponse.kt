package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.Organization
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationWitProjectCountServiceResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val description: String,
    val headerImage: String,
    val projectCount: Int
) {
    constructor(organization: Organization, projectCount: Int) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.description.orEmpty(),
        organization.headerImage.orEmpty(),
        projectCount
    )
}
