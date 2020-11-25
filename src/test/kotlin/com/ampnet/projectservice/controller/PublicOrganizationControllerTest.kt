package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.OrganizationFullServiceResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

        verify("User can get all organizations") {
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
            assertThat(organizationFullServiceResponse.uuid).isEqualTo(testContext.organization.uuid)
            assertThat(organizationFullServiceResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationFullServiceResponse.documents.size)
                .isEqualTo(testContext.organization.documents?.size)
            assertThat(organizationFullServiceResponse.createdAt).isEqualTo(testContext.organization.createdAt)
            assertThat(organizationFullServiceResponse.projectCount).isEqualTo(2)
            assertThat(organizationFullServiceResponse.coop).isEqualTo(COOP)
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var secondOrganization: Organization
        val documentLink = "link"
    }
}
