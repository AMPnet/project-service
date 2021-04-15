package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationFullServiceResponse
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationWitProjectCountServiceResponse
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val organizationMembershipService: OrganizationMembershipService,
    private val storageService: StorageService,
    private val imageProxyService: ImageProxyService,
    private val projectRepository: ProjectRepository
) : OrganizationService {

    companion object : KLogging()

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        if (organizationRepository.findByNameAndCoop(serviceRequest.name, serviceRequest.owner.coop).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_NAME,
                "Organization with name: ${serviceRequest.name} already exists in coop: ${serviceRequest.owner.coop}"
            )
        }
        val imageName = ServiceUtils.getImageNameFromMultipartFile(serviceRequest.headerImage)
        val link = storageService.saveImage(imageName, serviceRequest.headerImage.bytes)
        val organization = Organization(
            serviceRequest.name, serviceRequest.owner.uuid, link,
            serviceRequest.description, serviceRequest.owner.coop
        )
        val savedOrganization = organizationRepository.save(organization)
        organizationMembershipService.addUserToOrganization(
            serviceRequest.owner.uuid, organization.uuid, OrganizationRole.ORG_ADMIN
        )

        logger.info { "Created organization: ${organization.name}" }

        return savedOrganization
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(pageable: Pageable): Page<Organization> {
        return organizationRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    override fun findOrganizationWithProjectCountById(organizationUuid: UUID): OrganizationFullServiceResponse? {
        val organization = findOrganizationById(organizationUuid) ?: return null
        val projectCount = projectRepository.countProjectsByOrganization(organizationUuid)
        val image = imageProxyService.generateImageResponse(organization.headerImage)
        return OrganizationFullServiceResponse(organization, image, projectCount)
    }

    override fun findOrganizationById(organizationUuid: UUID): Organization? {
        return ServiceUtils.wrapOptional(organizationRepository.findByIdWithDocuments(organizationUuid))
    }

    @Transactional(readOnly = true)
    override fun findAllOrganizationsForUser(userUuid: UUID): List<OrganizationWitProjectCountServiceResponse> {
        val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
        val projectsMap = projectRepository.findAllByOrganizations(organizations.map { it.uuid })
            .groupBy { it.organization.uuid }
        return organizations.map { organization ->
            val projectCount = projectsMap[organization.uuid]?.size ?: 0
            val image = imageProxyService.generateImageResponse(organization.headerImage)
            OrganizationWitProjectCountServiceResponse(organization, image, projectCount)
        }
    }

    @Transactional(readOnly = true)
    override fun findByIdWithMemberships(organizationUuid: UUID): Organization? {
        return ServiceUtils.wrapOptional(organizationRepository.findByIdWithMemberships(organizationUuid))
    }

    @Transactional(readOnly = true)
    override fun getAllByActive(pageable: Pageable, active: Boolean): Page<Organization> {
        return organizationRepository.findByActive(active, pageable)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun addDocument(organizationUuid: UUID, request: DocumentSaveRequest): Document {
        val organization = getOrganization(organizationUuid)
        val document = storageService.saveDocument(request)
        addDocumentToOrganization(organization, document)
        return document
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun removeDocument(organizationUuid: UUID, documentId: Int) {
        val organization = getOrganization(organizationUuid)
        val storedDocuments = organization.documents.orEmpty().toMutableList()
        storedDocuments.firstOrNull { it.id == documentId }.let {
            storedDocuments.remove(it)
            organization.documents = storedDocuments
        }
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun updateOrganization(request: OrganizationUpdateServiceRequest): Organization {
        val organization = getOrganization(request.organizationUuid)
        request.headerImage?.let {
            val imageName = ServiceUtils.getImageNameFromMultipartFile(it)
            val link = storageService.saveImage(
                imageName,
                it.bytes
            )
            organization.headerImage = link
        }
        request.description?.let {
            organization.description = it
        }
        request.active?.let {
            organization.active = it
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
}
