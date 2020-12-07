package com.ampnet.projectservice.repository

import com.ampnet.projectservice.TestBase
import com.ampnet.projectservice.config.DatabaseCleanerService
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.persistence.repository.OrganizationInviteRepository
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
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
abstract class RepositoryTestBase : TestBase() {

    @Autowired
    protected lateinit var organizationInviteRepository: OrganizationInviteRepository

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    @Autowired
    protected lateinit var documentRepository: DocumentRepository

    @Autowired
    protected lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    protected lateinit var projectRepository: ProjectRepository

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    protected val defaultEmail = "user@email.com"
    protected val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")
    protected val coop = "ampnet-test"

    protected fun createOrganization(name: String, userUuid: UUID): Organization {
        val organization = Organization(
            UUID.randomUUID(), name, userUuid, ZonedDateTime.now(), null, true, null, null,
            null, "Organization header image", "description", coop
        )
        return organizationRepository.save(organization)
    }

    protected fun createOrganizationInvite(
        email: String,
        organization: Organization,
        invitedByUuid: UUID
    ): OrganizationInvitation {
        val organizationInvite = OrganizationInvitation(
            0, email, invitedByUuid, OrganizationRole.ORG_MEMBER, ZonedDateTime.now(), organization
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

    protected fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRole
    ): OrganizationMembership {
        val membership = OrganizationMembership(
            0, organizationUuid, userUuid, role, ZonedDateTime.now()
        )
        return membershipRepository.save(membership)
    }

    protected fun createProject(
        name: String,
        organization: Organization,
        createdByUserUuid: UUID,
        active: Boolean = true,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: Long = 10_000_000,
        minPerUser: Long = 10,
        maxPerUser: Long = 10_000
    ): Project {
        val project = Project(
            UUID.randomUUID(), organization, name, "description", ProjectLocation(0.1, 1.0),
            ProjectRoi(4.44, 9.99), startDate, endDate, expectedFunding, Currency.EUR, minPerUser, maxPerUser,
            null, listOf("gallery1", "gallery2"), listOf("news1", "news2"), createdByUserUuid,
            startDate.minusMinutes(1), active, null,
            listOf("blue", "yellow", "green"), coop, "short description"
        )
        return projectRepository.save(project)
    }

    protected fun createProjectDocument(
        project: Project,
        createdByUserUuid: UUID,
        name: String = "name",
        link: String = "link",
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdByUserUuid)
        val documents = project.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        project.documents = documents
        projectRepository.save(project)
        return savedDocument
    }
}
