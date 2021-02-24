package com.ampnet.projectservice.service

import com.ampnet.projectservice.amqp.mailservice.MailOrgInvitationMessage
import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.OrganizationInviteServiceImpl
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class OrganizationInvitationServiceTest : JpaServiceTestBase() {

    private val organizationInviteService: OrganizationInviteService by lazy {
        OrganizationInviteServiceImpl(
            inviteRepository, followerRepository, mailService, organizationService, organizationMembershipService, userService
        )
    }

    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org", userUuid)
    }
    private val invitedUsers = listOf("invited@email.com", "invited2@email.com", userEmail)

    @Test
    fun userCanFollowOrganization() {
        suppose("User exists without following organizations") {
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            organizationInviteService.followOrganization(userUuid, organization.uuid)
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
            organizationInviteService.followOrganization(userUuid, organization.uuid)
            val followers = followerRepository.findByOrganizationUuid(organization.uuid)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            organizationInviteService.unfollowOrganization(userUuid, organization.uuid)
        }

        verify("User is not following the organization") {
            val followers = followerRepository.findByOrganizationUuid(organization.uuid)
            assertThat(followers).hasSize(0)
        }
    }

    @Test
    fun adminUserCanInviteOtherUsersToOrganization() {
        suppose("User is admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMembershipService.addUserToOrganization(
                userUuid,
                organization.uuid,
                OrganizationRole.ORG_ADMIN
            )
        }

        verify("The admin can invite user to organization") {
            val request = OrganizationInviteServiceRequest(
                invitedUsers, organization.uuid, createUserPrincipal(userUuid, userEmail)
            )
            organizationInviteService.sendInvitation(request)
        }
        verify("Invitation is stored in database") {
            val firstInvitationOptional =
                inviteRepository.findByOrganizationUuidAndEmail(organization.uuid, invitedUsers.first())
            val secondInvitationOptional =
                inviteRepository.findByOrganizationUuidAndEmail(organization.uuid, invitedUsers.last())
            assertThat(firstInvitationOptional).isPresent
            assertThat(secondInvitationOptional).isPresent
            val firstInvitation = firstInvitationOptional.get()
            val secondInvitation = secondInvitationOptional.get()
            assertThat(firstInvitation.email).isEqualTo(invitedUsers.first())
            assertThat(firstInvitation.organization.uuid).isEqualTo(organization.uuid)
            assertThat(firstInvitation.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(firstInvitation.role).isEqualTo(OrganizationRole.ORG_MEMBER)
            assertThat(firstInvitation.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(secondInvitation.email).isEqualTo(invitedUsers.last())
        }
        verify("Sending mail invitation is called") {
            val captor = argumentCaptor<MailOrgInvitationMessage>()
            Mockito.verify(mailService, Mockito.times(1))
                .sendOrganizationInvitationMail(captor.capture())
            val mailRequest = captor.firstValue
            assertThat(mailRequest.emails).containsAll(invitedUsers)
            assertThat(mailRequest.organizationName).isEqualTo(organization.name)
            assertThat(mailRequest.sender).isEqualTo(userUuid)
            assertThat(mailRequest.coop).isEqualTo(COOP)
        }
    }

    @Test
    fun mustThrowErrorForDuplicateOrganizationInvite() {
        suppose("User has organization invite") {
            databaseCleanerService.deleteAllOrganizationInvitations()
            val request = OrganizationInviteServiceRequest(
                invitedUsers, organization.uuid, createUserPrincipal(userUuid, userEmail)
            )
            organizationInviteService.sendInvitation(request)
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = OrganizationInviteServiceRequest(
                invitedUsers, organization.uuid, createUserPrincipal(userUuid, userEmail)
            )
            val exception = assertThrows<ResourceAlreadyExistsException> {
                organizationInviteService.sendInvitation(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_DUPLICATE_INVITE)
        }
    }

    @Test
    fun mustThrowErrorForUserAlreadyMemberOfOrganization() {
        suppose("User is a member or organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationMembershipService.addUserToOrganization(
                userUuid,
                organization.uuid,
                OrganizationRole.ORG_ADMIN
            )
        }
        suppose("User service will return user already in organization") {
            Mockito.`when`(userService.getUsersByEmail(COOP, invitedUsers)).thenReturn(listOf(createUserResponse(userUuid, userEmail)))
        }

        verify("Service will throw an error for inviting user who is already a member") {
            val request = OrganizationInviteServiceRequest(
                invitedUsers, organization.uuid, createUserPrincipal(userUuid, userEmail)
            )
            val exception = assertThrows<ResourceAlreadyExistsException> {
                organizationInviteService.sendInvitation(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_DUPLICATE_USER)
        }
    }
}
