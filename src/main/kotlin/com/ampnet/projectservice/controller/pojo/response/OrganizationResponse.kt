package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.persistence.model.Organization
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationResponse(
    val uuid: UUID,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val legalInfo: String
) {
    constructor(organization: Organization) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.legalInfo.orEmpty()
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
    val legalInfo: String,
    val documents: List<DocumentResponse>
) {
    constructor(organization: Organization) : this(
        organization.uuid,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.legalInfo.orEmpty(),
        organization.documents?.map { DocumentResponse(it) }.orEmpty()
    )
}
