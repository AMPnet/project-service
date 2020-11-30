package com.ampnet.projectservice.grpc

import com.ampnet.projectservice.TestBase
import com.ampnet.projectservice.config.DatabaseCleanerService
import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.proto.GetByUuid
import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.projectservice.proto.OrganizationMembershipsResponse
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Import(DatabaseCleanerService::class)
class GrpcProjectServerTest : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository

    private lateinit var grpcServer: GrpcProjectServer

    private val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")
    private val anotherUser = UUID.fromString("0ea43aec-3301-11eb-adc1-0242ac120002")
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizationMemberships()
        testContext = TestContext()
        grpcServer = GrpcProjectServer(projectRepository, organizationRepository, membershipRepository)
    }

    @Test
    @Transactional
    fun mustReturnOrganizationMembers() {
        suppose("There are two organizations") {
            testContext.organization = createOrganization("org", userUuid)
            testContext.secondOrganization = createOrganization("org-2", userUuid)
        }
        suppose("There is a project") {
            testContext.project = createProject("project", testContext.organization, userUuid)
        }
        suppose("Users are members of organization") {
            testContext.firstMembership =
                addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
            testContext.secondMembership =
                addUserToOrganization(anotherUser, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("There is a member in another organization") {
            addUserToOrganization(UUID.randomUUID(), testContext.secondOrganization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Grpc service will return organization members for specified project") {
            val request = GetByUuid.newBuilder().setProjectUuid(testContext.project.uuid.toString()).build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java)
                as StreamObserver<OrganizationMembershipsResponse>
            grpcServer.getOrganizationMembers(request, streamObserver)
            val organizationMembersResponse = generateMembersResponse(
                listOf(testContext.firstMembership, testContext.secondMembership)
            )
            val response = OrganizationMembershipsResponse.newBuilder()
                .addAllMemberships(organizationMembersResponse).build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    private fun createProject(
        name: String,
        organization: Organization,
        createdByUserUuid: UUID,
        active: Boolean = true,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: Long = 10_000_000,
        minPerUser: Long = 10,
        maxPerUser: Long = 10_000,
        coop: String = COOP
    ): Project {
        val project = Project::class.java.getDeclaredConstructor().newInstance()
        project.uuid = UUID.randomUUID()
        project.organization = organization
        project.name = name
        project.mainImage = "main_image"
        project.description = "description"
        project.location = ProjectLocation(0.1, 1.0)
        project.roi = ProjectRoi(4.44, 9.99)
        project.startDate = startDate
        project.endDate = endDate
        project.expectedFunding = expectedFunding
        project.currency = Currency.EUR
        project.minPerUser = minPerUser
        project.maxPerUser = maxPerUser
        project.createdByUserUuid = createdByUserUuid
        project.active = active
        project.createdAt = startDate.minusMinutes(1)
        project.coop = coop
        project.tags = listOf("tag_1", "tag_2")
        return projectRepository.save(project)
    }

    private fun createOrganization(name: String, userUuid: UUID, coop: String = COOP): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.uuid = UUID.randomUUID()
        organization.name = name
        organization.description = "Organization description"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUserUuid = userUuid
        organization.headerImage = "Organization header image"
        organization.coop = coop
        return organizationRepository.save(organization)
    }

    private fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRole
    ): OrganizationMembership {
        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.userUuid = userUuid
        membership.organizationUuid = organizationUuid
        membership.role = role
        membership.createdAt = ZonedDateTime.now()
        return membershipRepository.save(membership)
    }

    private fun generateMembersResponse(members: List<OrganizationMembership>): List<OrganizationMembershipResponse> =
        members.map { membership ->
            OrganizationMembershipResponse.newBuilder()
                .setUserUuid(membership.userUuid.toString())
                .setOrganizationUuid(membership.organizationUuid.toString())
                .setRole(getOrganizationRole(membership.role))
                .setMemberSince(membership.createdAt.toInstant().toEpochMilli())
                .build()
        }

    private fun getOrganizationRole(type: OrganizationRole): OrganizationMembershipResponse.Role =
        when (type) {
            OrganizationRole.ORG_ADMIN -> OrganizationMembershipResponse.Role.ORG_ADMIN
            OrganizationRole.ORG_MEMBER -> OrganizationMembershipResponse.Role.ORG_MEMBER
        }

    private class TestContext {
        lateinit var project: Project
        lateinit var organization: Organization
        lateinit var secondOrganization: Organization
        lateinit var firstMembership: OrganizationMembership
        lateinit var secondMembership: OrganizationMembership
    }
}
