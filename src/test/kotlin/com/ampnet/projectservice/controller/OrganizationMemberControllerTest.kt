package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.security.WithMockCrowdfoundUser
import com.ampnet.userservice.proto.UserResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class OrganizationMemberControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganizationMembers() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Organization has two members") {
            testContext.member = UUID.randomUUID()
            testContext.memberSecond = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
            addUserToOrganization(
                testContext.memberSecond, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN
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
            val result = mockMvc.perform(MockMvcRequestBuilders.get("$organizationPath/${testContext.organization.uuid}/members"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val members: OrganizationMembershipsResponse = objectMapper.readValue(result.response.contentAsString)
            Assertions.assertThat(members.members.map { it.uuid }).hasSize(2)
                .containsAll(listOf(testContext.memberSecond, testContext.member))
            Assertions.assertThat(members.members.map { it.role }).hasSize(2)
                .containsAll(listOf(OrganizationRoleType.ORG_ADMIN.name, OrganizationRoleType.ORG_MEMBER.name))
            Assertions.assertThat(members.members.map { it.firstName }).containsAll(testContext.userResponses.map { it.firstName })
            Assertions.assertThat(members.members.map { it.lastName }).containsAll(testContext.userResponses.map { it.lastName })
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteOrganizationMember() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Organization has a member") {
            testContext.member = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can delete organization member") {
            mockMvc.perform(
                MockMvcRequestBuilders.delete("$organizationPath/${testContext.organization.uuid}/members/${testContext.member}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("Member is delete from organization") {
            val memberships = membershipRepository.findByOrganizationUuid(testContext.organization.uuid)
            Assertions.assertThat(memberships).hasSize(1)
            Assertions.assertThat(memberships[0].userUuid).isNotEqualTo(testContext.member)
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var member: UUID
        lateinit var memberSecond: UUID
        var userResponses: List<UserResponse> = emptyList()
    }
}
