package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.persistence.model.Organization
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationWitProjectCountServiceResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val description: String,
    val headerImage: String?,
    val projectCount: Int,
    val coop: String,
    val ownerUuid: UUID
) {
    constructor(organization: Organization, projectCount: Int) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.description.orEmpty(),
        organization.headerImage,
        projectCount,
        organization.coop,
        organization.createdByUserUuid
    )
}

data class OrganizationFullServiceResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val documents: List<DocumentResponse>,
    val description: String,
    val headerImage: String?,
    val projectCount: Int,
    val coop: String,
    val ownerUuid: UUID
) {
    constructor(organization: Organization, projectCount: Int) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.documents?.map { DocumentResponse(it) }.orEmpty(),
        organization.description.orEmpty(),
        organization.headerImage,
        projectCount,
        organization.coop,
        organization.createdByUserUuid
    )
}
