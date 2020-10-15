package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.UpdateOrganizationRoleRequest
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.security.WithMockCrowdfundUser
import com.ampnet.userservice.proto.UserResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationMembershipControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetOrganizationMembers() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Organization has two members") {
            testContext.member = UUID.randomUUID()
            testContext.memberSecond = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
            addUserToOrganization(
                testContext.memberSecond, testContext.organization.uuid, OrganizationRole.ORG_ADMIN
            )
        }
        suppose("User service will return user data") {
            val userResponse = createUserResponse(testContext.memberSecond, "email@mail.com", "first", "last", true)
            val memberResponse = createUserResponse(testContext.member, "email@mail.com", "ss", "ll", true)
            testContext.userResponses = listOf(userResponse, memberResponse)
            Mockito.`when`(userService.getUsers(listOf(testContext.memberSecond, testContext.member)))
                .thenReturn(testContext.userResponses)
            Mockito.`when`(userService.getUsers(listOf(testContext.member, testContext.memberSecond)))
                .thenReturn(testContext.userResponses)
        }

        verify("Controller returns all organization members") {
            val result = mockMvc.perform(get("$organizationPath/${testContext.organization.uuid}/members"))
                .andExpect(status().isOk)
                .andReturn()

            val members: OrganizationMembershipsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(members.members.map { it.uuid }).hasSize(2)
                .containsAll(listOf(testContext.memberSecond, testContext.member))
            assertThat(members.members.map { it.role }).hasSize(2)
                .containsAll(listOf(OrganizationRole.ORG_ADMIN.name, OrganizationRole.ORG_MEMBER.name))
            assertThat(members.members.map { it.firstName }).containsAll(testContext.userResponses.map { it.firstName })
            assertThat(members.members.map { it.lastName }).containsAll(testContext.userResponses.map { it.lastName })
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToDeleteOrganizationMember() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Organization has a member") {
            testContext.member = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("User can delete organization member") {
            mockMvc.perform(
                delete("$organizationPath/${testContext.organization.uuid}/members/${testContext.member}")
            )
                .andExpect(status().isOk)
        }
        verify("Member is delete from organization") {
            val memberships = membershipRepository.findByOrganizationUuid(testContext.organization.uuid)
            assertThat(memberships).hasSize(1)
            assertThat(memberships[0].userUuid).isNotEqualTo(testContext.member)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToChangeUserOrganizationRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Organization has a member") {
            testContext.member = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Admin can change user organization role") {
            val request = UpdateOrganizationRoleRequest(
                testContext.member,
                OrganizationRole.ORG_ADMIN
            )
            mockMvc.perform(
                post("$organizationPath/${testContext.organization.uuid}/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))

            )
                .andExpect(status().isOk)
        }
        verify("Member has changed organization role") {
            val membership = membershipRepository.findByOrganizationUuidAndUserUuid(testContext.organization.uuid, testContext.member).get()
            assertThat(membership.role.id).isEqualTo(OrganizationRole.ORG_ADMIN.id)
            assertThat(membership.organizationUuid).isEqualTo(testContext.organization.uuid)
            assertThat(membership.userUuid).isEqualTo(testContext.member)
            assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var member: UUID
        lateinit var memberSecond: UUID
        var userResponses: List<UserResponse> = emptyList()
    }
}
