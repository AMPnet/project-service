package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Role
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.RoleRepository
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
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val roleRepository: RoleRepository,
    private val storageService: StorageService
) : OrganizationService {

    companion object : KLogging()

    private val adminRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_MEMBER.id) }

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        if (organizationRepository.findByName(serviceRequest.name).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_NAME,
                "Organization with name: ${serviceRequest.name} already exists"
            )
        }
        val imageName = getImageNameFromMultipartFile(serviceRequest.headerImage)
        storageService.saveImage(imageName, serviceRequest.headerImage.bytes)
        val organization = Organization(serviceRequest.name, serviceRequest.ownerUuid, imageName, serviceRequest.description)
        val savedOrganization = organizationRepository.save(organization)
        addUserToOrganization(serviceRequest.ownerUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)

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

    @Transactional(readOnly = true)
    override fun getOrganizationMemberships(organizationUuid: UUID): List<OrganizationMembership> {
        return membershipRepository.findByOrganizationUuid(organizationUuid)
    }

    @Transactional
    override fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationMembership {
        // user can have only one membership(role) per one organization
        membershipRepository.findByOrganizationUuidAndUserUuid(organizationUuid, userUuid).ifPresent {
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_USER,
                "User ${it.userUuid} is already a member of this organization ${it.organizationUuid}"
            )
        }
        logger.debug { "Adding user: $userUuid to organization: $organizationUuid" }

        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.organizationUuid = organizationUuid
        membership.userUuid = userUuid
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipRepository.save(membership)
    }

    @Transactional
    override fun removeUserFromOrganization(userUuid: UUID, organizationUuid: UUID) {
        membershipRepository.findByOrganizationUuidAndUserUuid(organizationUuid, userUuid).ifPresent {
            logger.debug { "Removing user: $userUuid from organization: $organizationUuid" }
            membershipRepository.delete(it)
        }
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
            organizationRepository.save(organization)
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

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }

    private fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
        multipartFile.originalFilename ?: multipartFile.name
}
