package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationFullServiceResponse
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationWitProjectCountServiceResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(pageable: Pageable): Page<Organization>
    fun findOrganizationWithProjectCountById(organizationUuid: UUID): OrganizationFullServiceResponse?
    fun findOrganizationById(organizationUuid: UUID): Organization?
    fun findAllOrganizationsForUser(userUuid: UUID): List<OrganizationWitProjectCountServiceResponse>
    fun findByIdWithMemberships(organizationUuid: UUID): Organization?
    fun getAllActiveOrganizations(pageable: Pageable): Page<Organization>

    fun addDocument(organizationUuid: UUID, request: DocumentSaveRequest): Document
    fun removeDocument(organizationUuid: UUID, documentId: Int)
    fun updateOrganization(request: OrganizationUpdateServiceRequest): Organization
}
