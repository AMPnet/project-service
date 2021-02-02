package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.controller.pojo.request.ProjectLocationRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.exception.PermissionDeniedException
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
import org.springframework.mock.web.MockMultipartFile
import java.time.ZonedDateTime

class ProjectServiceTest : JpaServiceTestBase() {

    private val projectService: ProjectServiceImpl by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        ProjectServiceImpl(
            projectRepository, storageServiceImpl, applicationProperties, walletService,
            projectTagRepository, organizationMembershipService, organizationService
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Service received a request to create a project") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Test project")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), testContext.createProjectRequest
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
            assertThat(project.shortDescription).isEqualTo(request.shortDescription)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
        }
    }

    @Test
    fun mustNotBeAbleToCreateProjectWithMemberRole() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("User cannot create project with member role") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Test project")
            assertThrows<PermissionDeniedException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
        }
    }

    @Test
    fun mustBeAbleToAddMainImage() {
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), testContext.createProjectRequest
            )
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Main image is added to project") {
            testContext.imageLink = "link-main-image"
            val image = MockMultipartFile(
                "image", testContext.imageLink,
                "image/png", imageContent
            )
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.imageLink, imageContent)
            ).thenReturn(testContext.imageLink)
            projectService.addMainImage(testContext.project.uuid, userUuid, image)
        }

        verify("Image is stored in project") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    fun mustBeAbleToAddImagesToGallery() {
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), testContext.createProjectRequest
            )
        }
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Two images are added to project gallery") {
            testContext.gallery = listOf("image-link-1", "image-link-2")
            val firstImage = createImage(testContext.gallery.first(), imageContent)
            val secondImage = createImage(testContext.gallery.last(), imageContent)
            Mockito.`when`(cloudStorageService.saveFile(firstImage.originalFilename, imageContent))
                .thenReturn(firstImage.originalFilename)
            Mockito.`when`(cloudStorageService.saveFile(secondImage.originalFilename, imageContent))
                .thenReturn(secondImage.originalFilename)
            projectService.addImageToGallery(testContext.project.uuid, userUuid, firstImage)
            projectService.addImageToGallery(testContext.project.uuid, userUuid, secondImage)
        }

        verify("The project gallery contains added images") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).containsAll(testContext.gallery)
        }
    }

    @Test
    fun mustBeAbleToAppendNewImageToGallery() {
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), testContext.createProjectRequest
            )
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("The project has gallery") {
            testContext.gallery = listOf("link-1", "link-2")
            testContext.project.gallery = testContext.gallery.toSet()
            projectRepository.save(testContext.project)
        }
        suppose("Additional image is added to gallery") {
            testContext.imageLink = "link-new"
            val image = createImage(testContext.imageLink, imageContent)
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.imageLink, imageContent)
            ).thenReturn(testContext.imageLink)
            projectService.addImageToGallery(testContext.project.uuid, userUuid, image)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(
                createUserPrincipal(userUuid), testContext.createProjectRequest
            )
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("The project has gallery") {
            testContext.gallery = listOf("link-1", "link-2", "link-3")
            testContext.project.gallery = testContext.gallery.toSet()
            projectRepository.save(testContext.project)
        }
        suppose("Image is removed from gallery") {
            projectService.removeImagesFromGallery(testContext.project.uuid, userUuid, listOf("link-1", "link-3"))
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_ABOVE_MAX)
        }
    }

    @Test
    fun mustNotBeAbleToSetMaxPerUserAboveExpectedFunding() {
        suppose("Request has max per user above expected funding") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "Invalid end date",
                "Description",
                ProjectLocationRequest(12.34, 3.1324),
                ProjectRoiRequest(6.66, 7.77),
                currentTime,
                currentTime.plusDays(30),
                10000_00,
                Currency.EUR,
                1_00,
                1000000_00,
                false,
                emptyList()
            )
        }
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH)
        }
    }

    @Test
    fun mustBeAbleToGetAllProjectTags() {
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project has tags") {
            databaseCleanerService.deleteAllProjects()
            val project = projectService
                .createProject(createUserPrincipal(userUuid), createProjectRequest("First project"))
            testContext.tags = listOf("tag 1", "tag 2", "tag 3")
            project.tags = testContext.tags.toSet()
            projectRepository.save(project)
        }
        suppose("Another project has tags") {
            val project = projectService.createProject(
                createUserPrincipal(userUuid), createProjectRequest("Second project")
            )
            project.tags = setOf("tag 1", "tag 4")
            projectRepository.save(project)
            testContext.tags.toMutableList().add("tag 4")
        }
        suppose("Project from another cooperative has tags") {
            val project = projectService.createProject(
                createUserPrincipal(userUuid, coop = "another_coop"), createProjectRequest("Third project")
            )
            project.tags = setOf("tag 1", "tag 4", "tag 5")
            projectRepository.save(project)
        }

        verify("Service can get all project tags") {
            val allTags = projectService.getAllProjectTags(COOP)
            assertThat(allTags).hasSize(4).containsAll(testContext.tags)
        }
    }

    @Test
    fun mustBeAbleToGetProjectsByTags() {
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project has tags and is active") {
            databaseCleanerService.deleteAllProjects()
            val project = projectService
                .createProject(createUserPrincipal(userUuid), createProjectRequest("First project"))
            project.tags = setOf("tag 1", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Second project has tags and is active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), createProjectRequest("Second project"))
            project.tags = setOf("tag 1", "tag 2", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Third project has tags and is active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), createProjectRequest("Third project"))
            project.tags = setOf("tag 1", "tag 3")
            project.active = true
            projectRepository.save(project)
        }
        suppose("Fourth project has tags and is not active") {
            val project = projectService
                .createProject(createUserPrincipal(userUuid), createProjectRequest("Fourth project"))
            project.tags = setOf("tag 1", "tag 2", "tag 3")
            project.active = false
            projectRepository.save(project)
        }
        suppose("Fifth project has tags and is active but from another cooperative") {
            val project = projectService.createProject(
                createUserPrincipal(userUuid, coop = "another_coop"),
                createProjectRequest("Fifth project")
            )
            project.tags = setOf("tag 1", "tag 2", "tag 3")
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
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
        suppose("User is a admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_ROI)
        }
    }

    @Test
    fun mustThrowExceptionIfUserIsNotAMemberOfOrganization() {
        verify("Service will throw permission denied exception") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "name",
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
            val exception = assertThrows<PermissionDeniedException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MEM_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfUserHasNoPrivilegeToWriteInProject() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Service will throw permission denied exception") {
            val currentTime = ZonedDateTime.now()
            testContext.createProjectRequest = ProjectRequest(
                organization.uuid,
                "name",
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
            val exception = assertThrows<PermissionDeniedException> {
                projectService.createProject(createUserPrincipal(userUuid), testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_WRITE_PRIVILEGE)
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
            emptyList(),
            "short description"
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
