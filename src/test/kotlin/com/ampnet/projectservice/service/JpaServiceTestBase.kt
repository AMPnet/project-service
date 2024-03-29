package com.ampnet.projectservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.TestBase
import com.ampnet.projectservice.amqp.mailservice.MailService
import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.config.DatabaseCleanerService
import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.grpc.walletservice.WalletService
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.persistence.repository.OrganizationFollowerRepository
import com.ampnet.projectservice.persistence.repository.OrganizationInviteRepository
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import com.ampnet.projectservice.persistence.repository.impl.ProjectTagRepositoryImpl
import com.ampnet.projectservice.service.impl.ImageProxyServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationMembershipServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import com.ampnet.userservice.proto.UserResponse
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class, ApplicationProperties::class, ProjectTagRepositoryImpl::class)
abstract class JpaServiceTestBase : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    @Autowired
    protected lateinit var membershipRepository: OrganizationMembershipRepository

    @Autowired
    protected lateinit var followerRepository: OrganizationFollowerRepository

    @Autowired
    protected lateinit var inviteRepository: OrganizationInviteRepository

    @Autowired
    protected lateinit var projectRepository: ProjectRepository

    @Autowired
    protected lateinit var projectTagRepository: ProjectTagRepository

    @Autowired
    protected lateinit var documentRepository: DocumentRepository

    @Autowired
    protected lateinit var applicationProperties: ApplicationProperties

    protected val organizationMembershipService by lazy { OrganizationMembershipServiceImpl(membershipRepository) }

    protected val imageProxyService by lazy { ImageProxyServiceImpl(applicationProperties) }

    protected val organizationService: OrganizationService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(
            organizationRepository, organizationMembershipService, storageServiceImpl,
            imageProxyService, projectRepository
        )
    }

    protected val cloudStorageService: CloudStorageService = Mockito.mock(CloudStorageService::class.java)
    protected val mailService: MailService = Mockito.mock(MailService::class.java)
    protected val walletService: WalletService = Mockito.mock(WalletService::class.java)
    protected val userService: UserService = Mockito.mock(UserService::class.java)
    protected val userUuid: UUID = UUID.randomUUID()
    protected val defaultPageable: Pageable = PageRequest.of(0, 20)
    protected val userEmail = "user@email.com"

    protected fun createOrganization(name: String, createdByUuid: UUID, active: Boolean = true): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.uuid = UUID.randomUUID()
        organization.name = name
        organization.description = "Organization description"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUserUuid = createdByUuid
        organization.headerImage = null
        organization.coop = COOP
        organization.active = active
        return organizationRepository.save(organization)
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
        val project = Project::class.java.getDeclaredConstructor().newInstance()
        project.uuid = UUID.randomUUID()
        project.organization = organization
        project.name = name
        project.location = ProjectLocation(1.12, 2.22)
        project.description = "description"
        project.roi = ProjectRoi(1.11, 9.99)
        project.startDate = startDate
        project.endDate = endDate
        project.expectedFunding = expectedFunding
        project.currency = Currency.EUR
        project.minPerUser = minPerUser
        project.maxPerUser = maxPerUser
        project.createdByUserUuid = createdByUserUuid
        project.active = active
        project.createdAt = startDate.minusMinutes(1)
        project.coop = COOP
        return projectRepository.save(project)
    }

    protected fun saveDocument(
        name: String,
        link: String,
        createdByUserUuid: UUID,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val document = Document(link, name, type, size, createdByUserUuid)
        return documentRepository.save(document)
    }

    protected fun createUserPrincipal(
        userUuid: UUID,
        email: String = "email@email",
        name: String = "Username",
        authorities: Set<String> = mutableSetOf(),
        enabled: Boolean = true,
        verified: Boolean = true,
        coop: String = COOP
    ): UserPrincipal {
        return UserPrincipal(
            userUuid, email, name, authorities,
            enabled, verified, coop
        )
    }

    protected fun createUserResponse(
        uuid: UUID,
        email: String = "email@mail.com",
        first: String = "First",
        last: String = "Last",
        enabled: Boolean = true
    ): UserResponse =
        UserResponse.newBuilder()
            .setUuid(uuid.toString())
            .setEmail(email)
            .setFirstName(first)
            .setLastName(last)
            .setEnabled(enabled)
            .build()

    protected fun addUserToOrganization(userUuid: UUID, organizationUuid: UUID, role: OrganizationRole) {
        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.userUuid = userUuid
        membership.organizationUuid = organizationUuid
        membership.role = role
        membership.createdAt = ZonedDateTime.now()
        membershipRepository.save(membership)
    }

    protected fun createImage(
        originalFilename: String = "original-file-name",
        content: ByteArray = "ImageData".toByteArray()
    ): MockMultipartFile =
        MockMultipartFile("image", originalFilename, "image/png", content)
}
