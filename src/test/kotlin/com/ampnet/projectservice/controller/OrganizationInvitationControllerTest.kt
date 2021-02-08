package com.ampnet.projectservice.controller

import com.ampnet.projectservice.amqp.mailservice.MailOrgInvitationMessage
import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.projectservice.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.projectservice.controller.pojo.response.PendingInvitationsListResponse
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ErrorResponse
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.hibernate.Hibernate
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
            createOrganizationInvite(defaultEmail, testContext.organization, testContext.uuid)
            createOrganizationDocument(testContext.organization, userUuid)
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("User has invite from another organization") {
            val org = createOrganization("Second org", testContext.uuid, coop = "new-cop")
            createOrganizationInvite(defaultEmail, org, testContext.uuid)
        }

        verify("User can get a list of his invites") {
            val result = mockMvc.perform(get(pathMe))
                .andExpect(status().isOk)
                .andReturn()

            val invitesResponse: OrganizationInvitesListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(invitesResponse.organizationInvites).hasSize(1)
            val invite = invitesResponse.organizationInvites.first()
            assertThat(invite.role).isEqualTo(OrganizationRole.ORG_MEMBER)
            assertThat(invite.organizationUuid).isEqualTo(testContext.organization.uuid)
            assertThat(invite.organizationName).isEqualTo(testContext.organization.name)
            assertThat(invite.organizationHeaderImage).isEqualTo(testContext.organization.headerImage)
        }
        verify("Hibernate fetches only required entities") {
            val invites = organizationInviteRepository.findAllByEmailAndCoop(defaultEmail, COOP)
            val invite = invites.first()
            assertThat(Hibernate.isInitialized(invite.organization)).isTrue()
            assertThat(Hibernate.isInitialized(invite.organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(invite.organization.memberships)).isFalse()
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAcceptOrganizationInvitation() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvitations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(defaultEmail, testContext.organization, testContext.uuid)
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
            assertThat(memberships.first().role.id).isEqualTo(OrganizationRole.ORG_MEMBER.id)
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
            createOrganizationInvite(defaultEmail, testContext.organization, testContext.uuid)
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
    @WithMockCrowdfundUser(email = "user@email.com")
    fun mustBeAbleToInviteUsersToOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
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
            assertThat(firstInvite.organization.uuid).isEqualTo(testContext.organization.uuid)
            assertThat(firstInvite.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(firstInvite.role.id).isEqualTo(OrganizationRole.ORG_MEMBER.id)
            assertThat(secondInvite.email).isEqualTo(testContext.emails.last())
        }
        verify("Sending mail invitation is called") {
            val captor = argumentCaptor<MailOrgInvitationMessage>()
            Mockito.verify(mailService, Mockito.times(1))
                .sendOrganizationInvitationMail(captor.capture())
            val mailRequest = captor.firstValue
            assertThat(mailRequest.emails).containsAll(testContext.emails)
            assertThat(mailRequest.organizationName).isEqualTo(testContext.organization.name)
            assertThat(mailRequest.sender).isEqualTo(userUuid)
            assertThat(mailRequest.coop).isEqualTo(testContext.organization.coop)
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
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
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
            val request = OrganizationInviteRequest(listOf("valid@email.com, wrongFormat@", "$%wrongFor@mat2"))
            val result = mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            val errorResponse: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(errorResponse.errCode).isEqualTo(getResponseErrorCode(ErrorCode.ORG_INVALID_INVITE))
            assertThat(errorResponse.errors?.values?.first()?.split(",")).hasSize(2)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustThrowValidationExceptionForDuplicatedInvitation() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("There are user invitations") {
            testContext.emails.forEach {
                createOrganizationInvite(it, testContext.organization, userUuid)
            }
        }

        verify("Organization invites already exist") {
            val request = OrganizationInviteRequest(testContext.emails)
            val result = mockMvc.perform(
                post("$pathOrganization/${testContext.organization.uuid}/invite")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.ORG_DUPLICATE_INVITE)
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
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Other user has organization invites") {
            createOrganizationInvite(testContext.invitedEmail, testContext.organization, userUuid)
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
            testContext.organizationInvitation =
                createOrganizationInvite(defaultEmail, testContext.organization, testContext.uuid)
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
            assertThat(invite.role).isEqualTo(testContext.organizationInvitation.role)
        }
    }

    private fun createOrganizationInvite(
        email: String,
        organization: Organization,
        invitedByUuid: UUID
    ): OrganizationInvitation {
        val organizationInvite = OrganizationInvitation::class.java.getConstructor().newInstance()
        organizationInvite.email = email
        organizationInvite.organization = organization
        organizationInvite.invitedByUserUuid = invitedByUuid
        organizationInvite.role = OrganizationRole.ORG_MEMBER
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
