package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.ProjectFullResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.TagsResponse
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PublicProjectControllerTest : ControllerTestBase() {

    private val publicProjectPath = "/public/project"

    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("Test organization", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetSpecificProject() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("Project response is valid") {
            val result = mockMvc.perform(get("$publicProjectPath/${testContext.project.uuid}"))
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isEqualTo(testContext.project.name)
            assertThat(projectResponse.description).isEqualTo(testContext.project.description)
            assertThat(projectResponse.location).isEqualTo(testContext.project.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
            assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
            assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
            assertThat(projectResponse.gallery).isEqualTo(testContext.project.gallery.orEmpty())
            assertThat(projectResponse.news).isEqualTo(testContext.project.newsLinks.orEmpty())
            assertThat(projectResponse.active).isEqualTo(testContext.project.active)
        }
    }

    @Test
    fun mustReturnNotFoundForMissingProject() {
        verify("Controller will return not found for missing project") {
            mockMvc.perform(get("$publicProjectPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustBeAbleToGetAllProjects() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("Another organization has project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.secondProject = createProject("Second project", secondOrganization, userUuid)
        }

        verify("Controller will return all projects") {
            val result = mockMvc.perform(
                get(publicProjectPath)
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc"))
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(2)
            assertThat(projectsResponse.projects.map { it.uuid })
                .containsAll(listOf(testContext.project.uuid, testContext.secondProject.uuid))
        }
    }

    @Test
    fun mustBeAbleToGetActiveProjects() {
        suppose("Active project exists") {
            testContext.project = createProject("Active project", organization, userUuid)
        }
        suppose("Another organization has active project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.secondProject = createProject("Second active project", secondOrganization, userUuid)
        }
        suppose("One project is not active") {
            createProject("Not active", organization, userUuid, active = false)
        }

        verify("Controller will return active projects") {
            val result = mockMvc.perform(
                get("$publicProjectPath/active")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc"))
                .andExpect(status().isOk)
                .andReturn()

            val projectsResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(2)
            assertThat(projectsResponse.projects.map { it.uuid })
                .containsAll(listOf(testContext.project.uuid, testContext.secondProject.uuid))
        }
    }

    @Test
    fun mustBeAbleToQueryProjectsByTags() {
        suppose("There are projects with tags") {
            val project1 = createProject("Project 1", organization, userUuid)
            addTagsToProject(project1, listOf("wind", "blue"))
            val project2 = createProject("Project 2", organization, userUuid)
            addTagsToProject(project2, listOf("wind", "green"))
        }

        verify("Controller will return projects containing all tags") {
            val result = mockMvc.perform(
                get(publicProjectPath)
                    // TODO: check this in docs
                    .param("tags", "wind", "green")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc"))
                .andExpect(status().isOk)
                .andReturn()

            val projectListResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(1)
            assertThat(projectListResponse.projects.first().tags).containsAll(listOf("wind", "green"))
        }
    }

    @Test
    fun mustBeAbleToGetAllProjectTags() {
        suppose("There are projects with tags") {
            val project1 = createProject("Project 1", organization, userUuid)
            addTagsToProject(project1, listOf("wind", "solar"))
            val project2 = createProject("Project 2", organization, userUuid)
            addTagsToProject(project2, listOf("wind", "green"))
        }

        verify("Controller will return all tags") {
            val result = mockMvc.perform(get("$publicProjectPath/tags"))
                .andExpect(status().isOk)
                .andReturn()

            val tagsResponse: TagsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(tagsResponse.tags)
                .hasSize(3)
                .containsAll(listOf("wind", "solar", "green"))
        }
    }

    @Test
    fun mustBeAbleToGetListOfProjectsForOrganization() {
        suppose("Organization has 3 projects") {
            testContext.project = createProject("Project 1", organization, userUuid)
            createProject("Project 2", organization, userUuid)
            createProject("Project 3", organization, userUuid)
        }
        suppose("Second organization has project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.secondProject = createProject("Second project", secondOrganization, userUuid)
        }

        verify("Controller will return all projects for specified organization") {
            val result = mockMvc.perform(
                get("$publicProjectPath/organization/${organization.uuid}")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "name,asc"))
                .andExpect(status().isOk)
                .andReturn()

            val projectListResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(3)
            assertThat(projectListResponse.projects.map { it.uuid }).doesNotContain(testContext.secondProject.uuid)

            val filterResponse = projectListResponse.projects.filter { it.uuid == testContext.project.uuid }
            assertThat(filterResponse).hasSize(1)
            val projectResponse = filterResponse.first()
            assertThat(projectResponse.name).isEqualTo(testContext.project.name)
            assertThat(projectResponse.description).isEqualTo(testContext.project.description)
            assertThat(projectResponse.location).isEqualTo(testContext.project.location)
            assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
            assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
            assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
            assertThat(projectResponse.active).isEqualTo(testContext.project.active)
        }
    }

    private fun addTagsToProject(project: Project, tags: List<String>) {
        project.tags = tags
        projectRepository.save(project)
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var secondProject: Project
    }
}
