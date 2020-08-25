package com.ampnet.projectservice.service

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.OrganizationMemberServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationMemberServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val organizationMemberServiceImpl = OrganizationMemberServiceImpl(membershipRepository, roleRepository)
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, organizationMemberServiceImpl, storageServiceImpl)
    }
    override val organizationMemberService: OrganizationMemberService by lazy {
        OrganizationMemberServiceImpl(membershipRepository, roleRepository)
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
            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User has admin role") {
            verifyUserMembership(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
    }

    @Test
    fun mustBeAbleToAddUserAsMemberToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User has member role") {
            verifyUserMembership(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }
    }

    @Test
    fun mustBeAbleToRemoveUserFromOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can be removed from organization") {
            organizationMemberService.removeUserFromOrganization(userUuid, organization.uuid)
        }
        verify("User is no longer member of organization") {
            val memberships = membershipRepository.findByOrganizationUuid(organization.uuid)
            Assertions.assertThat(memberships).hasSize(0)
        }
    }

    @Test
    fun userCanGetListOfPersonalOrganizations() {
        suppose("User is a member of two organizations") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            testContext.secondOrganization = createOrganization("Second org", userUuid)

            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
            organizationMemberService.addUserToOrganization(
                userUuid, testContext.secondOrganization.uuid, OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("User is a member of two organizations") {
            val organizations = organizationService.findAllOrganizationsForUser(userUuid)
            Assertions.assertThat(organizations).hasSize(2)
            Assertions.assertThat(organizations.map { it.uuid }).contains(organization.uuid, testContext.secondOrganization.uuid)
        }
    }

    @Test
    fun userCanHaveOnlyOneRoleInOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Service will throw an exception for adding second role to the user in the same organization") {
            assertThrows<ResourceAlreadyExistsException> {
                organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
            }
        }
    }

    @Test
    fun mustBeAbleToGetMembersOfOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMemberService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
            organizationMemberService.addUserToOrganization(
                testContext.member, organization.uuid, OrganizationRoleType.ORG_MEMBER
            )
        }
        suppose("There is another organization with members") {
            val additionalOrganization = createOrganization("Second organization", userUuid)
            organizationMemberService.addUserToOrganization(
                UUID.randomUUID(), additionalOrganization.uuid, OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("Service will list all members of organization") {
            val memberships = organizationMemberService.getOrganizationMemberships(organization.uuid)
            Assertions.assertThat(memberships).hasSize(2)
            Assertions.assertThat(memberships.map { it.userUuid }).containsAll(listOf(userUuid, testContext.member))
        }
    }

    private fun verifyUserMembership(userUuid: UUID, organizationUuid: UUID, role: OrganizationRoleType) {
        val memberships = membershipRepository.findByUserUuid(userUuid)
        Assertions.assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        Assertions.assertThat(membership.userUuid).isEqualTo(userUuid)
        Assertions.assertThat(membership.organizationUuid).isEqualTo(organizationUuid)
        Assertions.assertThat(OrganizationRoleType.fromInt(membership.role.id)).isEqualTo(role)
        Assertions.assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private class TestContext {
        lateinit var secondOrganization: Organization
        val member: UUID = UUID.randomUUID()
    }
}
