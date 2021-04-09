package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.ImageResponse
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val description: String,
    val headerImage: String?,
    val image: ImageResponse?,
    val coop: String,
    val active: Boolean,
    val ownerUuid: UUID
) {
    constructor(organization: Organization, image: ImageResponse?) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.description.orEmpty(),
        organization.headerImage,
        image,
        organization.coop,
        organization.active,
        organization.createdByUserUuid
    )
}

data class OrganizationListResponse(
    val organizations: List<OrganizationResponse>,
    val page: Int = 0,
    val totalPages: Int = 1
)

data class OrganizationWithDocumentResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val documents: List<DocumentResponse>,
    val description: String,
    val headerImage: String?,
    val image: ImageResponse?,
    val coop: String,
    val ownerUuid: UUID
) {
    constructor(organization: Organization, image: ImageResponse?) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.documents?.map { DocumentResponse(it) }.orEmpty(),
        organization.description.orEmpty(),
        organization.headerImage,
        image,
        organization.coop,
        organization.createdByUserUuid
    )
}
