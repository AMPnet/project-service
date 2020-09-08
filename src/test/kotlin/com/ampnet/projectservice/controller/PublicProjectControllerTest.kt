package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.CountActiveProjectsCount
import com.ampnet.projectservice.controller.pojo.response.ProjectFullResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectLocationResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectRoiResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.projectservice.controller.pojo.response.TagsResponse
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.walletservice.proto.WalletResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

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
            assertThat(projectResponse.location.lat).isEqualTo(testContext.project.location.lat)
            assertThat(projectResponse.location.long).isEqualTo(testContext.project.location.long)
            assertThat(projectResponse.roi.from).isEqualTo(testContext.project.roi.from)
            assertThat(projectResponse.roi.to).isEqualTo(testContext.project.roi.to)
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
                    .param("sort", "createdAt,desc")
            )
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
        suppose("Active project has active wallet") {
            testContext.project = createProject(
                "Active project", organization, userUuid,
                startDate = ZonedDateTime.now().minusDays(1)
            )
            testContext.activeWallet = createWalletResponse(userUuid, testContext.project.uuid)
        }
        suppose("Another active project has inactive wallet") {
            testContext.secondProject = createProject(
                "Second active project", organization, userUuid,
                startDate = ZonedDateTime.now().minusDays(1)
            )
            testContext.inactiveWallet = createWalletResponse(userUuid, testContext.secondProject.uuid)
        }
        suppose("Another organization has active project") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            testContext.thirdProject = createProject(
                "Second active project", secondOrganization, userUuid,
                startDate = ZonedDateTime.now().minusDays(1)
            )
        }
        suppose("One project is not active") {
            createProject("Not active", organization, userUuid, active = false)
        }
        suppose("Wallet service returns a list of wallets") {
            Mockito.`when`(
                walletService.getWalletsByOwner(
                    listOf(
                        testContext.thirdProject.uuid, testContext.secondProject.uuid, testContext.project.uuid
                    )
                )
            )
                .thenReturn(listOf(testContext.activeWallet))
        }

        verify("Controller will return active projects with active wallets") {
            val result = mockMvc.perform(
                get("$publicProjectPath/active")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val response: ProjectWithWalletListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.projectsWithWallet).hasSize(1)
            val projectWithWallet = response.projectsWithWallet.first()
            assertThat(projectWithWallet.project.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectWithWallet.project.name).isEqualTo(testContext.project.name)
            assertThat(projectWithWallet.project.description).isEqualTo(testContext.project.description)
            assertThat(projectWithWallet.project.location).isEqualTo(ProjectLocationResponse(testContext.project.location))
            assertThat(projectWithWallet.project.roi).isEqualTo(ProjectRoiResponse(testContext.project.roi))
            assertThat(projectWithWallet.project.startDate).isEqualTo(testContext.project.startDate)
            assertThat(projectWithWallet.project.endDate).isEqualTo(testContext.project.endDate)
            assertThat(projectWithWallet.project.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(projectWithWallet.project.currency).isEqualTo(testContext.project.currency)
            assertThat(projectWithWallet.project.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(projectWithWallet.project.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(projectWithWallet.project.mainImage).isIn(testContext.project.mainImage, "")
            assertThat(projectWithWallet.project.active).isEqualTo(testContext.project.active)
            assertThat(projectWithWallet.project.tags).isIn(testContext.project.tags, arrayListOf<String>())
            assertThat(projectWithWallet.wallet.uuid).isEqualTo(UUID.fromString(testContext.activeWallet.uuid))
            assertThat(projectWithWallet.wallet.owner).isEqualTo(testContext.project.uuid.toString())
            assertThat(projectWithWallet.wallet.activationData).isEqualTo(testContext.activeWallet.activationData)
            assertThat(projectWithWallet.wallet.type).isEqualTo(testContext.activeWallet.type.name)
            assertThat(projectWithWallet.wallet.currency).isEqualTo(testContext.activeWallet.currency)
            assertThat(projectWithWallet.wallet.hash).isEqualTo(testContext.activeWallet.hash)
        }
    }

    @Test
    fun mustBeAbleToCountActiveProjects() {
        suppose("There is active project") {
            testContext.project = createProject(
                "Active project", organization, userUuid,
                startDate = ZonedDateTime.now().minusDays(1)
            )
        }
        suppose("There is inactive project") {
            createProject("Not active", organization, userUuid, active = false)
        }
        suppose("There is ended project") {
            createProject(
                "Ended", organization, userUuid,
                startDate = ZonedDateTime.now().minusDays(30), endDate = ZonedDateTime.now().minusDays(1)
            )
        }
        suppose("There is project in future") {
            createProject(
                "Not active", organization, userUuid,
                startDate = ZonedDateTime.now().plusDays(1), endDate = ZonedDateTime.now().plusDays(30)
            )
        }

        verify("Controller will count all active projects") {
            val result = mockMvc.perform(get("$publicProjectPath/active/count"))
                .andExpect(status().isOk)
                .andReturn()

            val countResponse: CountActiveProjectsCount = objectMapper.readValue(result.response.contentAsString)
            assertThat(countResponse.activeProjects).isEqualTo(1)
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
        suppose("There is a deactivated project") {
            val project3 = createProject("Project 3", organization, userUuid, active = false)
            addTagsToProject(project3, listOf("wind", "green"))
        }

        verify("Controller will return projects containing all tags and are active") {
            val result = mockMvc.perform(
                get(publicProjectPath)
                    .param("tags", "wind", "green")
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
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
                    .param("sort", "name,asc")
            )
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
            assertThat(projectResponse.location.lat).isEqualTo(testContext.project.location.lat)
            assertThat(projectResponse.location.long).isEqualTo(testContext.project.location.long)
            assertThat(projectResponse.roi.from).isEqualTo(testContext.project.roi.from)
            assertThat(projectResponse.roi.to).isEqualTo(testContext.project.roi.to)
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
        lateinit var thirdProject: Project
        lateinit var activeWallet: WalletResponse
        lateinit var inactiveWallet: WalletResponse
    }
}
