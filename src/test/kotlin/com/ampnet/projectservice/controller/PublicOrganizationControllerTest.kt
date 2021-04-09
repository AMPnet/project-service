package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsInfoResponse
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.OrganizationFullServiceResponse
import com.ampnet.userservice.proto.UserResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class PublicOrganizationControllerTest : ControllerTestBase() {

    private val organizationPath = "/public/organization"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    fun mustReturnListOfOrganizations() {
        suppose("Multiple organizations exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
            createOrganization("test 2", userUuid)
            createOrganization("test 3", userUuid)
        }
        suppose("Hidden organization exists") {
            createOrganization("test 4", userUuid, active = false)
        }

        verify("User can get all active organizations") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get(organizationPath)
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val organizationResponse: OrganizationListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.organizations).hasSize(3)
            assertThat(organizationResponse.organizations.map { it.name }).contains(testContext.organization.name)
        }
    }

    @Test
    fun mustReturnNotFoundForNonExistingOrganization() {
        verify("Response not found for non existing organization") {
            mockMvc.perform(MockMvcRequestBuilders.get("$organizationPath/${UUID.randomUUID()}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    @Test
    fun mustBeAbleToGetOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has document") {
            createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
        }
        suppose("Two projects belong to the organization") {
            createProject("Project1", testContext.organization, userUuid)
            createProject("Project1", testContext.organization, userUuid)
        }
        suppose("Antoher organization exists and has projects") {
            testContext.secondOrganization = createOrganization("test organization 2", userUuid)
            createProject("Project1", testContext.secondOrganization, userUuid)
            createProject("Project1", testContext.secondOrganization, userUuid)
        }

        verify("User can get organization with id") {
            val result =
                mockMvc.perform(MockMvcRequestBuilders.get("$organizationPath/${testContext.organization.uuid}"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val organizationFullServiceResponse: OrganizationFullServiceResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationFullServiceResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationFullServiceResponse.description).isEqualTo(testContext.organization.description)
            assertThat(organizationFullServiceResponse.headerImage).isEqualTo(testContext.organization.headerImage)
            verifyImageResponse(testContext.organization.headerImage, organizationFullServiceResponse.image)
            assertThat(organizationFullServiceResponse.uuid).isEqualTo(testContext.organization.uuid)
            assertThat(organizationFullServiceResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationFullServiceResponse.documents.size)
                .isEqualTo(testContext.organization.documents?.size)
            assertThat(organizationFullServiceResponse.createdAt).isEqualTo(testContext.organization.createdAt)
            assertThat(organizationFullServiceResponse.projectCount).isEqualTo(2)
            assertThat(organizationFullServiceResponse.coop).isEqualTo(COOP)
            assertThat(organizationFullServiceResponse.ownerUuid).isEqualTo(testContext.organization.createdByUserUuid)
        }
    }

    @Test
    fun mustBeAbleToGetOrganizationMembers() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Organization has one member") {
            testContext.member = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("User service will return user data") {
            testContext.adminResponse = createUserResponse(userUuid, "admin@mail.com", "admin", "admin", true)
            testContext.memberResponse = createUserResponse(testContext.member, "email@mail.com", "ss", "ll", true)
            testContext.userResponses = listOf(testContext.adminResponse, testContext.memberResponse)
            Mockito.`when`(userService.getUsers(listOf(userUuid, testContext.member)))
                .thenReturn(testContext.userResponses)
            Mockito.`when`(userService.getUsers(listOf(testContext.member, userUuid)))
                .thenReturn(testContext.userResponses)
        }

        verify("Controller returns all organization members") {
            val result = mockMvc.perform(RestDocumentationRequestBuilders.get("$organizationPath/${testContext.organization.uuid}/members"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val members: OrganizationMembershipsInfoResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(members.members).hasSize(2)
            assertThat(members.members.map { it.role }).hasSize(2)
                .containsAll(listOf(OrganizationRole.ORG_ADMIN.name, OrganizationRole.ORG_MEMBER.name))
            assertThat(members.members.map { it.firstName }).containsAll(testContext.userResponses.map { it.firstName })
            assertThat(members.members.map { it.lastName }).containsAll(testContext.userResponses.map { it.lastName })
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var secondOrganization: Organization
        val documentLink = "link"
        lateinit var member: UUID
        var userResponses: List<UserResponse> = emptyList()
        lateinit var adminResponse: UserResponse
        lateinit var memberResponse: UserResponse
    }
}
