package com.ampnet.projectservice.service

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import java.util.UUID

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(organizationUuid: UUID): Organization?
    fun findAllOrganizationsForUser(userUuid: UUID): List<Organization>

    fun getOrganizationMemberships(organizationUuid: UUID): List<OrganizationMembership>
    fun addUserToOrganization(userUuid: UUID, organizationUuid: UUID, role: OrganizationRoleType): OrganizationMembership
    fun removeUserFromOrganization(userUuid: UUID, organizationUuid: UUID)

    fun addDocument(organizationUuid: UUID, request: DocumentSaveRequest): Document
    fun removeDocument(organizationUuid: UUID, documentId: Int)
}
