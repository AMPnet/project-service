package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipService: OrganizationMembershipService,
    private val storageService: StorageService
) : OrganizationService {

    companion object : KLogging()

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        if (organizationRepository.findByName(serviceRequest.name).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_NAME,
                "Organization with name: ${serviceRequest.name} already exists"
            )
        }
        val imageName = getImageNameFromMultipartFile(serviceRequest.headerImage)
        val link = storageService.saveImage(imageName, serviceRequest.headerImage.bytes)
        val organization = Organization(
            serviceRequest.name, serviceRequest.owner.uuid, link,
            serviceRequest.description, serviceRequest.owner.coop
        )
        val savedOrganization = organizationRepository.save(organization)
        organizationMembershipService.addUserToOrganization(
            serviceRequest.owner.uuid, organization.uuid, OrganizationRoleType.ORG_ADMIN
        )

        logger.info { "Created organization: ${organization.name}" }

        return savedOrganization
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(pageable: Pageable): Page<Organization> {
        return organizationRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    override fun findOrganizationById(organizationUuid: UUID): Organization? {
        return ServiceUtils.wrapOptional(organizationRepository.findByIdWithDocuments(organizationUuid))
    }

    @Transactional(readOnly = true)
    override fun findAllOrganizationsForUser(userUuid: UUID): List<Organization> {
        return organizationRepository.findAllOrganizationsForUserUuid(userUuid)
    }

    @Transactional
    override fun addDocument(organizationUuid: UUID, request: DocumentSaveRequest): Document {
        val organization = getOrganization(organizationUuid)
        val document = storageService.saveDocument(request)
        addDocumentToOrganization(organization, document)
        return document
    }

    @Transactional
    override fun removeDocument(organizationUuid: UUID, documentId: Int) {
        val organization = getOrganization(organizationUuid)
        val storedDocuments = organization.documents.orEmpty().toMutableList()
        storedDocuments.firstOrNull { it.id == documentId }.let {
            storedDocuments.remove(it)
            organization.documents = storedDocuments
        }
    }

    @Transactional
    override fun updateOrganization(request: OrganizationUpdateServiceRequest): Organization {
        val organization = getOrganization(request.organizationUuid)
        request.headerImage?.let {
            val imageName = getImageNameFromMultipartFile(it)
            val link = storageService.saveImage(
                imageName,
                it.bytes
            )
            organization.headerImage = link
        }
        request.description?.let {
            organization.description = it
        }
        return organization
    }

    private fun getOrganization(organizationUuid: UUID): Organization =
        findOrganizationById(organizationUuid)
            ?: throw ResourceNotFoundException(
                ErrorCode.ORG_MISSING,
                "Missing organization with uuid: $organizationUuid"
            )

    private fun addDocumentToOrganization(organization: Organization, document: Document) {
        val documents = organization.documents.orEmpty().toMutableList()
        documents += document
        organization.documents = documents
        organizationRepository.save(organization)
        logger.debug { "Add document: ${document.name} to organization: ${organization.uuid}" }
    }

    private fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
        multipartFile.originalFilename ?: multipartFile.name
}
