package com.ampnet.projectservice.grpc

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.proto.GetByUuid
import com.ampnet.projectservice.proto.OrganizationMembershipResponse
import com.ampnet.projectservice.proto.OrganizationMembershipsResponse
import com.ampnet.projectservice.repository.RepositoryTestBase
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class GrpcProjectServerTest : RepositoryTestBase() {

    private val grpcServer: GrpcProjectServer by lazy {
        GrpcProjectServer(projectRepository, organizationRepository, membershipRepository)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizationMemberships()
        testContext = TestContext()
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
                addUserToOrganization(UUID.randomUUID(), testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("There is a member in another organization") {
            addUserToOrganization(UUID.randomUUID(), testContext.secondOrganization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Grpc service will return organization members for specified project") {
            val request = GetByUuid.newBuilder().setProjectUuid(testContext.project.uuid.toString()).build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java)
                as StreamObserver<OrganizationMembershipsResponse>
            grpcServer.getOrganizationMembersForProject(request, streamObserver)
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
