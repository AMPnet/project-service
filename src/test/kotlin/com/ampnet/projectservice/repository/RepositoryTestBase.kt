package com.ampnet.projectservice.repository

import com.ampnet.projectservice.TestBase
import com.ampnet.projectservice.config.DatabaseCleanerService
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.persistence.repository.OrganizationInviteRepository
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.RoleRepository
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(value = [SpringExtension::class])
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class)
class RepositoryTestBase : TestBase() {

    @Autowired
    protected lateinit var organizationInviteRepository: OrganizationInviteRepository

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    @Autowired
    protected lateinit var roleRepository: RoleRepository

    @Autowired
    protected lateinit var documentRepository: DocumentRepository

    @Autowired
    protected lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    protected val defaultEmail = "user@email.com"
    protected val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")

    protected fun createOrganization(name: String, userUuid: UUID): Organization {
        val organization = Organization(
            UUID.randomUUID(), name, userUuid, ZonedDateTime.now(), null, true, null, null,
            null, "Organization header image", "description"
        )
        return organizationRepository.save(organization)
    }

    protected fun createOrganizationInvite(
        email: String,
        organization: Organization,
        invitedByUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationInvitation {
        val organizationInvite = OrganizationInvitation(
            0, email, invitedByUuid, roleRepository.getOne(role.id), ZonedDateTime.now(), organization
        )
        return organizationInviteRepository.save(organizationInvite)
    }

    protected fun createOrganizationDocument(
        organization: Organization,
        createdByUserUuid: UUID,
        name: String = "name",
        link: String = "link",
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdByUserUuid)
        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        organization.documents = documents
        organizationRepository.save(organization)
        return savedDocument
    }

    protected fun saveDocument(
        name: String,
        link: String,
        type: String,
        size: Int,
        createdByUserUuid: UUID
    ): Document {
        val document = Document(0, link, name, type, size, createdByUserUuid, ZonedDateTime.now())
        return documentRepository.save(document)
    }

    protected fun addUserToOrganization(userUuid: UUID, organizationUuid: UUID, role: OrganizationRoleType) {
        val membership = OrganizationMembership(
            0, organizationUuid, userUuid, roleRepository.getOne(role.id), ZonedDateTime.now()
        )
        membershipRepository.save(membership)
    }
}
