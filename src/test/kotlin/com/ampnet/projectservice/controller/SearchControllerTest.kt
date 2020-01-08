package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.SearchOrgAndProjectResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SearchControllerTest : ControllerTestBase() {

    private val searchPath = "/search"

    @Test
    @WithMockUser
    fun mustReturnEmptyListForSearch() {
        suppose("There are no projects and organizations") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllProjects()
        }

        verify("Controller will return empty list of projects and organizations") {
            val result = mockMvc.perform(
                get(searchPath).param("name", "Empty")
                    .param("size", "10")
                    .param("page", "0"))
                .andExpect(status().isOk)
                .andReturn()

            val searchResponse: SearchOrgAndProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(searchResponse.organizations).hasSize(0)
            assertThat(searchResponse.projects).hasSize(0)
        }
    }

    @Test
    @WithMockUser
    fun mustReturnListOfOrganizationsAndProjects() {
        suppose("There are projects and organizations with similar name") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllProjects()

            val organization = createOrganization("The Prospect Organization", userUuid)
            createOrganization("The Organization", userUuid)

            createProject("The first project", organization, userUuid)
            createProject("The projcccp", organization, userUuid)
        }

        verify("Controller will a list of organizations and project containing searched word") {
            val result = mockMvc.perform(
                get(searchPath).param("name", "Pro")
                    .param("size", "10")
                    .param("page", "0"))
                .andExpect(status().isOk)
                .andReturn()

            val searchResponse: SearchOrgAndProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(searchResponse.organizations).hasSize(1)
            assertThat(searchResponse.projects).hasSize(2)
        }
    }
}
