package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ProjectUpdatesRequest
import com.ampnet.projectservice.controller.pojo.response.ProjectUpdateResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectUpdatesResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectUpdate
import com.ampnet.projectservice.persistence.repository.ProjectUpdateRepository
import com.ampnet.projectservice.security.WithMockCrowdfoundUser
import com.ampnet.projectservice.security.WithMockUserSecurityFactory
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProjectUpdateControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var projectUpdateRepository: ProjectUpdateRepository

    private val project: Project by lazy {
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllProjects()
        val organization = createOrganization("Update org", userUuid)
        createProject("Project update", organization, userUuid)
    }
    private val testContext: TestContext = TestContext()

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllProjectUpdates()
        databaseCleanerService.deleteAllOrganizationMemberships()
    }

    @Test
    fun mustBeAbleToGetProjectUpdates() {
        suppose("Suppose project has some updates") {
            createProjectUpdate(testContext.title, testContext.content)
            createProjectUpdate("Webinar", "Fundamentals of software architecture")
        }

        verify("User can get project updates") {
            val result = mockMvc.perform(
                get("/public/project/${project.uuid}/updates")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc"))
                .andExpect(status().isOk)
                .andReturn()

            val response: ProjectUpdatesResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.updates).hasSize(2)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteProjectUpdate() {
        suppose("User is project admin") {
            addUserToOrganization(userUuid, project.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Suppose project has update") {
            testContext.projectUpdate = createProjectUpdate(testContext.title, testContext.content)
        }

        verify("Project admin can delete project update") {
            mockMvc.perform(
                delete("/project/${project.uuid}/updates/${testContext.projectUpdate.id}"))
                .andExpect(status().isOk)
        }
        verify("Project update is deleted") {
            assertThat(projectUpdateRepository.findAll()).isEmpty()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProjectUpdate() {
        suppose("User is project admin") {
            addUserToOrganization(userUuid, project.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("Project admin can create project update") {
            val request = ProjectUpdatesRequest(testContext.title, testContext.content)
            val result = mockMvc.perform(
                post("/project/${project.uuid}/updates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk)
                .andReturn()

            val updateResponse: ProjectUpdateResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(updateResponse.title).isEqualTo(testContext.title)
            assertThat(updateResponse.content).isEqualTo(testContext.content)
            assertThat(updateResponse.projectUuid).isEqualTo(project.uuid)
            assertThat(updateResponse.author).isEqualTo(WithMockUserSecurityFactory.fullName)
            assertThat(updateResponse.date).isNotNull()
        }
        verify("Project update is created") {
            val updates = projectUpdateRepository.findAll()
            assertThat(updates).hasSize(1)
            val projectUpdate = updates.first()
            assertThat(projectUpdate.title).isEqualTo(testContext.title)
            assertThat(projectUpdate.content).isEqualTo(testContext.content)
            assertThat(projectUpdate.createdBy).isEqualTo(userUuid)
            assertThat(projectUpdate.projectUuid).isEqualTo(project.uuid)
        }
    }

    private fun createProjectUpdate(title: String, content: String): ProjectUpdate {
        val projectUpdate = ProjectUpdate(project.uuid, title, content, "Some user", userUuid)
        return projectUpdateRepository.save(projectUpdate)
    }

    private class TestContext {
        val title = "Some title"
        val content = "Some content"
        lateinit var projectUpdate: ProjectUpdate
    }
}
