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
    fun findOrganizationById(id: Int): Organization?
    fun findAllOrganizationsForUser(userUuid: UUID): List<Organization>

    fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership>
    fun addUserToOrganization(userUuid: UUID, organizationId: Int, role: OrganizationRoleType): OrganizationMembership
    fun removeUserFromOrganization(userUuid: UUID, organizationId: Int)

    fun addDocument(organizationId: Int, request: DocumentSaveRequest): Document
    fun removeDocument(organizationId: Int, documentId: Int)
}
