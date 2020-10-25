package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(pageable: Pageable): Page<Organization>
    fun findOrganizationById(organizationUuid: UUID): Organization?
    fun findAllOrganizationsForUser(userUuid: UUID): List<Organization>
    fun findByIdWithMemberships(organizationUuid: UUID): Organization?

    fun addDocument(organizationUuid: UUID, request: DocumentSaveRequest): Document
    fun removeDocument(organizationUuid: UUID, documentId: Int)
    fun updateOrganization(request: OrganizationUpdateServiceRequest): Organization
}
