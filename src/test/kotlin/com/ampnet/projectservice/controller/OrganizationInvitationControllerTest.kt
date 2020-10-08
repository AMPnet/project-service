package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.projectservice.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.projectservice.controller.pojo.response.PendingInvitationsListResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationInvitationControllerTest : ControllerTestBase() {

    private val pathMe = "/invites/me/"
    private val pathOrganization = "/invites/organization/"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetOrganizationInvitations() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(
                defaultEmail, testContext.organization.uuid, testContext.uuid,
                OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("User can get a list of his invites") {
            val result = mockMvc.perform(get(pathMe))
                .andExpect(status().isOk)
                .andReturn()

            val invitesResponse: OrganizationInvitesListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(invitesResponse.organizationInvites).hasSize(1)
            val invite = invitesResponse.organizationInvites.first()
            assertThat(invite.role).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invite.organizationUuid).isEqualTo(testContext.organization.uuid)
            assertThat(invite.organizationName).isEqualTo(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAcceptOrganizationInvitation() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvitations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(
                defaultEmail, testContext.organization.uuid, testContext.uuid,
                OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/${testContext.organization.uuid}/accept"))
                .andExpect(status().isOk)
        }
        verify("User is a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
            assertThat(organizations).hasSize(1)
            val organization = organizations.first()
            assertThat(organization.uuid).isEqualTo(testContext.organization.uuid)
            val memberships = organization.memberships ?: fail("Organization membership must no be null")
            assertThat(memberships).hasSize(1)
            assertThat(memberships.first().role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                .findByOrganizationUuidAndEmail(testContext.organization.uuid, defaultEmail)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRejectOrganizationInvitation() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvitations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(
                defaultEmail, testContext.organization.uuid, testContext.uuid,
                OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/${testContext.organization.uuid}/reject"))
                .andExpect(status().isOk)
                .andReturn()
        }
        verify("User is not a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
            assertThat(organizations).hasSize(0)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                .findByOrganizationUuidAndEmail(testContext.organization.uuid, defaultEmail)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToInviteUsersToOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has non organization invites") {
            databaseCleanerService.deleteAllOrganizationInvitations()
        }

        verify("Organization member can invite users to his organization") {
            val request = OrganizationInviteRequest(testContext.emails)
            mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
        }
        verify("Organization invite is stored in database") {
            val invites = organizationInviteRepository.findAll()
            assertThat(invites).hasSize(2)
            val firstInvite = invites.first()
            val secondInvite = invites.last()
            assertThat(firstInvite.email).isEqualTo(testContext.emails.first())
            assertThat(firstInvite.organizationUuid).isEqualTo(testContext.organization.uuid)
            assertThat(firstInvite.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(firstInvite.role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
            assertThat(secondInvite.email).isEqualTo(testContext.emails.last())
        }
        verify("Sending mail invitation is called") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendOrganizationInvitationMail(testContext.emails, testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotBeAbleToInviteUserToOrganizationWithoutOrgAdminRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User cannot invite other user without ORG_ADMIN role") {
            val request = OrganizationInviteRequest(testContext.emails)
            mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustNotBeAbleToInviteUserToOrganizationIfNotMemberOfOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", testContext.uuid)
        }

        verify("User can invite user to organization if he is not a member of organization") {
            val request = OrganizationInviteRequest(testContext.emails)
            mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustThrowValidationExceptionIfEmailIsInWrongFormat() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }

        verify("Organization invite request is in wrong format") {
            val request = OrganizationInviteRequest(listOf("wrongFormat", "$%wrongFormat2"))
            mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect { result -> assertThat(result.resolvedException).isInstanceOf(InvalidRequestException:: class.java) }
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRevokeUserInvitation() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has organization invites") {
            inviteUserToOrganization(
                testContext.invitedEmail, testContext.organization.uuid, userUuid,
                OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("User can revoke invitation") {
            mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/${testContext.invitedEmail}/revoke")
            )
                .andExpect(status().isOk)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetPendingInvitations() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
            testContext.organizationInvitation = createOrganizationInvite(
                defaultEmail, testContext.organization.uuid, testContext.uuid,
                OrganizationRoleType.ORG_MEMBER
            )
        }

        verify("We can get a list of pending invitations") {
            val result = mockMvc.perform(
                get("$pathOrganization${testContext.organization.uuid}")
            )
                .andExpect(status().isOk)
                .andReturn()

            val pendingInvitations: PendingInvitationsListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(pendingInvitations.pendingInvites).hasSize(1)
            val invite = pendingInvitations.pendingInvites.first()
            assertThat(invite.userEmail).isEqualTo(testContext.organizationInvitation.email)
            assertThat(invite.createdAt).isEqualTo(testContext.organizationInvitation.createdAt)
            assertThat(invite.role).isEqualTo(OrganizationRoleType.fromInt(testContext.organizationInvitation.role.id))
        }
    }

    private fun inviteUserToOrganization(
        email: String,
        organizationUuid: UUID,
        invitedByUuid: UUID,
        role: OrganizationRoleType
    ) {
        val invitation = OrganizationInvitation::class.java.getConstructor().newInstance()
        invitation.email = email
        invitation.organizationUuid = organizationUuid
        invitation.invitedByUserUuid = invitedByUuid
        invitation.createdAt = ZonedDateTime.now()
        invitation.role = roleRepository.getOne(role.id)
        organizationInviteRepository.save(invitation)
    }

    private fun createOrganizationInvite(
        email: String,
        organizationUuid: UUID,
        invitedByUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationInvitation {
        val organizationInvite = OrganizationInvitation::class.java.getConstructor().newInstance()
        organizationInvite.email = email
        organizationInvite.organizationUuid = organizationUuid
        organizationInvite.invitedByUserUuid = invitedByUuid
        organizationInvite.role = roleRepository.getOne(role.id)
        organizationInvite.createdAt = ZonedDateTime.now()
        return organizationInviteRepository.save(organizationInvite)
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var organizationInvitation: OrganizationInvitation
        val uuid: UUID = UUID.randomUUID()
        val invitedEmail = "invited@email.com"
        val emails = listOf("user1@gmail.com", "user2@gmail.com")
    }
}
