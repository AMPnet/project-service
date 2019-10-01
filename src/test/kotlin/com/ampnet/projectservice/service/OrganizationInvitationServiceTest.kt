package com.ampnet.projectservice.service

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.OrganizationInviteServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class OrganizationInvitationServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, membershipRepository, roleRepository, storageServiceImpl)
    }
    private val service: OrganizationInviteService by lazy {
        OrganizationInviteServiceImpl(
                inviteRepository, followerRepository, roleRepository, mailService, organizationService)
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org", userUuid)
    }
    private val invitedUser = "invited@email.com"

    @Test
    fun userCanFollowOrganization() {
        suppose("User exists without following organizations") {
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            service.followOrganization(userUuid, organization.uuid)
        }

        verify("User is following the organization") {
            val followers = followerRepository.findByOrganizationUuid(organization.uuid)
            assertThat(followers).hasSize(1)

            val follower = followers[0]
            assertThat(follower.userUuid).isEqualTo(userUuid)
            assertThat(follower.organizationUuid).isEqualTo(organization.uuid)
            assertThat(follower.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    fun userCanUnFollowOrganization() {
        suppose("User is following the organization") {
            databaseCleanerService.deleteAllOrganizationFollowers()
            service.followOrganization(userUuid, organization.uuid)
            val followers = followerRepository.findByOrganizationUuid(organization.uuid)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            service.unfollowOrganization(userUuid, organization.uuid)
        }

        verify("User is not following the organization") {
            val followers = followerRepository.findByOrganizationUuid(organization.uuid)
            assertThat(followers).hasSize(0)
        }
    }

    @Test
    fun adminUserCanInviteOtherUserToOrganization() {
        suppose("User is admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationService.addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("The admin can invite user to organization") {
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.uuid, userUuid)
            service.sendInvitation(request)
        }
        verify("Invitation is stored in database") {
            val optionalInvitation =
                    inviteRepository.findByOrganizationUuidAndEmail(organization.uuid, invitedUser)
            assertThat(optionalInvitation).isPresent
            val invitation = optionalInvitation.get()
            assertThat(invitation.email).isEqualTo(invitedUser)
            assertThat(invitation.organizationUuid).isEqualTo(organization.uuid)
            assertThat(invitation.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(OrganizationRoleType.fromInt(invitation.role.id)).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invitation.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Sending mail invitation is called") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendOrganizationInvitationMail(invitedUser, organization.name)
        }
    }

    @Test
    fun mustThrowErrorForDuplicateOrganizationInvite() {
        suppose("User has organization invite") {
            databaseCleanerService.deleteAllOrganizationInvitations()
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.uuid, userUuid)
            service.sendInvitation(request)
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.uuid, userUuid)
            assertThrows<ResourceAlreadyExistsException> {
                service.sendInvitation(request)
            }
        }
    }
}
