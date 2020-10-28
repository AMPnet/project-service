package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.pojo.request.UpdateOrganizationRoleRequest
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.OrganizationMembershipServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import com.ampnet.projectservice.service.pojo.OrganizationMemberServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationMembershipServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val organizationMemberServiceImpl = OrganizationMembershipServiceImpl(membershipRepository)
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, organizationMemberServiceImpl, storageServiceImpl, projectRepository)
    }
    private val organizationMembershipService: OrganizationMembershipService by lazy {
        OrganizationMembershipServiceImpl(membershipRepository)
    }
    private lateinit var organization: Organization

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("test org", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddUserAsAdminToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added as admin") {
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("User has admin role") {
            verifyUserMembership(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
    }

    @Test
    fun mustBeAbleToAddUserAsMemberToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("User has member role") {
            verifyUserMembership(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }
    }

    @Test
    fun mustBeAbleToRemoveUserFromOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("User can be removed from organization") {
            organizationMembershipService.removeUserFromOrganization(userUuid, organization.uuid)
        }
        verify("User is no longer member of organization") {
            val memberships = membershipRepository.findByOrganizationUuid(organization.uuid)
            assertThat(memberships).hasSize(0)
        }
    }

    @Test
    fun userCanGetListOfPersonalOrganizations() {
        suppose("User is a member of two organizations") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            testContext.secondOrganization = createOrganization("Second org", userUuid)

            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
            organizationMembershipService
                .addUserToOrganization(userUuid, testContext.secondOrganization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("User is a member of two organizations") {
            val organizations = organizationService.findAllOrganizationsForUser(userUuid)
            assertThat(organizations).hasSize(2)
            assertThat(organizations.map { it.uuid }).contains(organization.uuid, testContext.secondOrganization.uuid)
        }
    }

    @Test
    fun userCanHaveOnlyOneRoleInOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Service will throw an exception for adding second role to the user in the same organization") {
            assertThrows<ResourceAlreadyExistsException> {
                organizationMembershipService
                    .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
            }
        }
    }

    @Test
    fun mustBeAbleToGetMembersOfOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
            organizationMembershipService
                .addUserToOrganization(testContext.member, organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("There is another organization with members") {
            val additionalOrganization = createOrganization("Second organization", userUuid)
            organizationMembershipService
                .addUserToOrganization(UUID.randomUUID(), additionalOrganization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Service will list all members of organization") {
            val memberships =
                organizationMembershipService.getOrganizationMemberships(organization.uuid)
            assertThat(memberships).hasSize(2)
            assertThat(memberships.map { it.userUuid }).containsAll(listOf(userUuid, testContext.member))
        }
    }

    @Test
    fun mustBeAbleToChangeUserOrganizationRole() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("Admin has changed user's organization role") {
            val request = UpdateOrganizationRoleRequest(userUuid, OrganizationRole.ORG_ADMIN)
            organizationMembershipService
                .updateOrganizationRole(OrganizationMemberServiceRequest(organization.uuid, request))
        }

        verify("User has admin role") {
            verifyUserMembership(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationMembershipNotFound() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = UpdateOrganizationRoleRequest(
                testContext.member, OrganizationRole.ORG_ADMIN
            )
            assertThrows<ResourceNotFoundException> {
                organizationMembershipService
                    .updateOrganizationRole(OrganizationMemberServiceRequest(organization.uuid, request))
            }
        }
    }

    private fun verifyUserMembership(userUuid: UUID, organizationUuid: UUID, role: OrganizationRole) {
        val memberships = membershipRepository.findByUserUuid(userUuid)
        assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        assertThat(membership.userUuid).isEqualTo(userUuid)
        assertThat(membership.organizationUuid).isEqualTo(organizationUuid)
        assertThat(membership.role).isEqualTo(role)
        assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private class TestContext {
        lateinit var secondOrganization: Organization
        val member: UUID = UUID.randomUUID()
    }
}
