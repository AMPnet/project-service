package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.controller.pojo.request.ProjectLocationRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.impl.ProjectServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class ProjectServiceTest : JpaServiceTestBase() {

    private val projectService: ProjectServiceImpl by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        ProjectServiceImpl(
            projectRepository, storageServiceImpl,
            applicationProperties, walletService, projectTagRepository
        )
    }
    private val imageContent = "data".toByteArray()

    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("Das Organization", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToCreateProject() {
        suppose("Service received a request to create a project") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Test project")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), organization, testContext.createProjectRequest
            )
        }

        verify("Project is created") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            val request = testContext.createProjectRequest
            assertThat(project.name).isEqualTo(request.name)
            assertThat(project.description).isEqualTo(request.description)
            assertThat(project.location.lat).isEqualTo(request.location.lat)
            assertThat(project.location.long).isEqualTo(request.location.long)
            assertThat(project.roi.from).isEqualTo(request.roi.from)
            assertThat(project.roi.to).isEqualTo(request.roi.to)
            assertThat(project.startDate).isEqualTo(request.startDate)
            assertThat(project.endDate).isEqualTo(request.endDate)
            assertThat(project.expectedFunding).isEqualTo(request.expectedFunding)
            assertThat(project.currency).isEqualTo(request.currency)
            assertThat(project.minPerUser).isEqualTo(request.minPerUser)
            assertThat(project.maxPerUser).isEqualTo(request.maxPerUser)
            assertThat(project.createdByUserUuid).isEqualTo(userUuid)
            assertThat(project.organization.uuid).isEqualTo(organization.uuid)
            assertThat(project.active).isEqualTo(request.active)
            assertThat(project.mainImage.isNullOrEmpty()).isTrue()
            assertThat(project.gallery.isNullOrEmpty()).isTrue()
            assertThat(project.documents.isNullOrEmpty()).isTrue()
            assertThat(project.createdByUserUuid).isEqualTo(userUuid)
            assertThat(project.coop).isEqualTo(COOP)
        }
    }

    @Test
    fun mustNotBeAbleToCreateProjectWithEndDateBeforeStartDate() {
        suppose("Request has end date before start date") {
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(2.22, 4.44),
                ZonedDateTime.now(),
                ZonedDateTime.now().minusDays(1),
                1000000,
                Currency.EUR,
                100,
                10000,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
        }
    }

    @Test
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), organization, testContext.createProjectRequest
            )
        }
        suppose("Main image is added to project") {
            testContext.imageLink = "link-main-image"
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.imageLink, imageContent)
            ).thenReturn(testContext.imageLink)
            projectService.addMainImage(testContext.project, testContext.imageLink, imageContent)
        }

        verify("Image is stored in project") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    fun mustBeAbleToAddImagesToGallery() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), organization, testContext.createProjectRequest
            )
        }
        suppose("Two images are added to project gallery") {
            testContext.gallery = listOf("link-1", "link-2")
            testContext.gallery.forEach {
                Mockito.`when`(cloudStorageService.saveFile(it, imageContent)).thenReturn(it)
                projectService.addImageToGallery(testContext.project, it, imageContent)
            }
        }

        verify("The project gallery contains added images") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).containsAll(testContext.gallery)
        }
    }

    @Test
    fun mustBeAbleToAppendNewImageToGallery() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), organization, testContext.createProjectRequest
            )
        }
        suppose("The project has gallery") {
            testContext.gallery = listOf("link-1", "link-2")
            testContext.project.gallery = testContext.gallery
            projectRepository.save(testContext.project)
        }
        suppose("Additional image is added to gallery") {
            testContext.imageLink = "link-new"
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.imageLink, imageContent)
            ).thenReturn(testContext.imageLink)
            projectService.addImageToGallery(testContext.project, testContext.imageLink, imageContent)
        }

        verify("Gallery has additional image") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).containsAll(testContext.gallery)
            assertThat(project.gallery).contains(testContext.imageLink)
        }
    }

    @Test
    fun mustBeAbleToRemoveImageFromGallery() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), organization, testContext.createProjectRequest
            )
        }
        suppose("The project has gallery") {
            testContext.gallery = listOf("link-1", "link-2", "link-3")
            testContext.project.gallery = testContext.gallery
            projectRepository.save(testContext.project)
        }
        suppose("Image is removed from gallery") {
            projectService.removeImagesFromGallery(testContext.project, listOf("link-1", "link-3"))
        }

        verify("Gallery does not have deleted image") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).doesNotContain("link-1", "link-3")
            assertThat(project.gallery).contains("link-2")
        }
    }

    @Test
    fun mustNotBeAbleToSetEndDateBeforePresent() {
        suppose("Request has end date before present date") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid end date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(1.11, 5.55),
                currentTime.minusDays(60),
                currentTime.minusDays(30),
                1000000,
                Currency.EUR,
                100,
                10000,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
        }
    }

    @Test
    fun mustNotBeAbleToSetMinPerUserAboveMaxPerUser() {
        suppose("Request has min per user value above max per user") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid end date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(6.66, 7.77),
                currentTime,
                currentTime.plusDays(30),
                1000000,
                Currency.EUR,
                1_000,
                1,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_ABOVE_MAX)
        }
    }

    @Test
    fun mustNotBeAbleToSetMaxPerUserAboveSystemMax() {
        suppose("Request has max per user value above system max") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid end date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(4.2, 9.99),
                currentTime,
                currentTime.plusDays(30),
                10_000_000_000_000,
                Currency.EUR,
                1,
                applicationProperties.investment.maxPerUser + 1,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH)
        }
    }

    @Test
    fun mustNotBeAbleToSetExpectedFundingAboveSystemMax() {
        suppose("Request has max per user value above system max") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid end date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(2.22, 3.23),
                currentTime,
                currentTime.plusDays(30),
                applicationProperties.investment.maxPerProject + 1,
                Currency.EUR,
                1,
                1_000_000_000,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH)
        }
    }

    @Test
    fun mustBeAbleToGetAllProjectTags() {
        suppose("Project has tags") {
            databaseCleanerService.deleteAllProjects()
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("First project"))
            testContext.tags = listOf("tag 1", "tag 2", "tag 3")
            project.tags = testContext.tags
            projectRepository.save(project)
        }
        suppose("Another project has tags") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("Second project"))
            project.tags = listOf("tag 1", "tag 4")
            projectRepository.save(project)
            testContext.tags.toMutableList().add("tag 4")
        }
        suppose("Project from another cooperative has tags") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid, coop = "another_coop"), organization, createProjectRequest("Third project"))
            project.tags = listOf("tag 1", "tag 4", "tag 5")
            projectRepository.save(project)
        }

        verify("Service can get all project tags") {
            val allTags = projectService.getAllProjectTags(COOP)
            assertThat(allTags).hasSize(4).containsAll(testContext.tags)
        }
    }

    @Test
    fun mustBeAbleToGetProjectsByTags() {
        suppose("Project has tags and is active") {
            databaseCleanerService.deleteAllProjects()
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("First project"))
            project.tags = listOf("tag 1", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Second project has tags and is active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("Second project"))
            project.tags = listOf("tag 1", "tag 2", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Third project has tags and is active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("Third project"))
            project.tags = listOf("tag 1", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Fourth project has tags and is not active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), organization, createProjectRequest("Fourth project"))
            project.tags = listOf("tag 1", "tag 2", "tag 3")
            project.active = false
            projectRepository.save(project)
        }
        suppose("Fifth project has tags and is active but from another cooperative") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid, coop = "another_coop"), organization, createProjectRequest("Fifth project"))
            project.tags = listOf("tag 1", "tag 2", "tag 3")
            project.active = true
            projectRepository.save(project)
        }

        verify("Service will return project for tags") {
            val tags = listOf("tag 1", "tag 2")
            val projects = projectService.getProjectsByTags(tags, COOP, defaultPageable)
            val project = projects.first()
            assertThat(project.tags).containsAll(tags)
        }
    }

    @Test
    fun mustValidateProjectRoiOnProjectCreate() {
        suppose("Request has roi from higher than roi to") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid roi",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(12.22, 2.23),
                currentTime,
                currentTime.plusDays(30),
                1000000,
                Currency.EUR,
                10,
                10000,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_ROI)
        }
    }

    @Test
    fun mustValidateProjectRoiOnProjectUpdate() {
        suppose("Update request has roi from higher than roi to") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid roi update",
                "Description",
                ProjectLocationRequest(12.34, 12.33),
                ProjectRoiRequest(12.22, 2.23),
                currentTime,
                currentTime.plusDays(30),
                1000000,
                Currency.EUR,
                10,
                10000,
                false,
                emptyList()
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), organization, testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_ROI)
        }
    }

    private fun createProjectRequest(name: String): ProjectRequest {
        return ProjectRequest(
            organization.uuid,
            name,
            "Description",
            ProjectLocationRequest(12.34, 3.1324),
            ProjectRoiRequest(1.23, 12.44),
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(30),
            1000000,
            Currency.EUR,
            100,
            10000,
            false,
            emptyList()
        )
    }

    private class TestContext {
        lateinit var createProjectRequest: ProjectRequest
        lateinit var project: Project
        lateinit var imageLink: String
        lateinit var gallery: List<String>
        lateinit var tags: List<String>
    }
}
