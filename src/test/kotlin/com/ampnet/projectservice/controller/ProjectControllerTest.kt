package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectLocationRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectFullResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProjectControllerTest : ControllerTestBase() {

    private val projectPath = "/project"

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
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserWithoutOrganizationMembership() {
        verify("Controller will forbidden for user without membership to create project") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserOrganizationMembership() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorIfCreatingProjectForMissingOrganization() {
        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(UUID.randomUUID(), "Error project")
            mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("Controller will return create project") {
            testContext.projectRequest = createProjectRequest(organization.uuid, "Das project")
            val result = mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(testContext.projectRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isNotNull()
            assertThat(projectResponse.name).isEqualTo(testContext.projectRequest.name)
            assertThat(projectResponse.description).isEqualTo(testContext.projectRequest.description)
            assertThat(projectResponse.roi.from).isEqualTo(testContext.projectRequest.roi.from)
            assertThat(projectResponse.roi.to).isEqualTo(testContext.projectRequest.roi.to)
            assertThat(projectResponse.startDate).isEqualTo(testContext.projectRequest.startDate)
            assertThat(projectResponse.endDate).isEqualTo(testContext.projectRequest.endDate)
            assertThat(projectResponse.expectedFunding).isEqualTo(testContext.projectRequest.expectedFunding)
            assertThat(projectResponse.currency).isEqualTo(testContext.projectRequest.currency)
            assertThat(projectResponse.minPerUser).isEqualTo(testContext.projectRequest.minPerUser)
            assertThat(projectResponse.maxPerUser).isEqualTo(testContext.projectRequest.maxPerUser)
            assertThat(projectResponse.active).isEqualTo(testContext.projectRequest.active)
            assertThat(projectResponse.mainImage).isNullOrEmpty()

            testContext.projectUuid = projectResponse.uuid
        }
        verify("Project is stored in database") {
            val optionalProject = projectRepository.findById(testContext.projectUuid)
            assertThat(optionalProject).isPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToUpdateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("Admin can update project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description",
                        ProjectLocationRequest(22.1, 0.3), ProjectRoiRequest(1.11, 5.55), false, listOf("tag"))
            val result = mockMvc.perform(
                put("$projectPath/${testContext.project.uuid}")
                    .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(projectResponse.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(projectResponse.location.lat).isEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(projectResponse.location.long).isEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(projectResponse.roi.from).isEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(projectResponse.roi.to).isEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(projectResponse.active).isEqualTo(testContext.projectUpdateRequest.active)
            assertThat(projectResponse.tags).containsAll(testContext.projectUpdateRequest.tags)
        }
        verify("Project is updated") {
            val optionalProject = projectRepository.findById(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            val updatedProject = optionalProject.get()
            assertThat(updatedProject.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(updatedProject.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(updatedProject.location.lat).isEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(updatedProject.location.long).isEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(updatedProject.roi.from).isEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(updatedProject.roi.to).isEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(updatedProject.active).isEqualTo(testContext.projectUpdateRequest.active)
            assertThat(updatedProject.tags).containsAll(testContext.projectUpdateRequest.tags)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddProjectTags() {
        suppose("User is admin in the organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("There is a project with tags") {
            testContext.project = createProject("Projectos", organization, userUuid)
            testContext.project.tags = listOf("wind", "green")
            projectRepository.save(testContext.project)
        }

        verify("User can add new tags to project") {
            testContext.tags = listOf("wind", "green", "gg")
            testContext.projectUpdateRequest = ProjectUpdateRequest(tags = testContext.tags)
            val result = mockMvc.perform(
                put("$projectPath/${testContext.project.uuid}")
                    .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val project: ProjectFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(project.tags).containsAll(testContext.tags)
        }
        verify("Tags are added to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().tags).containsAll(testContext.tags)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnForbiddenIfUserIsMissingOrgPrivileges() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("User cannot update project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description",
                        ProjectLocationRequest(12.234, 23.432), ProjectRoiRequest(4.44, 8.88), false)
            mockMvc.perform(
                put("$projectPath/${testContext.project.uuid}")
                    .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUpdatingNonExistingProject() {
        verify("User cannot update non existing project") {
            testContext.projectUpdateRequest =
                    ProjectUpdateRequest("new name", "description", null, null, false)
            val response = mockMvc.perform(
                put("$projectPath/${UUID.randomUUID()}")
                    .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddDocumentForProject() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("User can add document") {
            val result = mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/document")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
                    .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            val projectDocuments = optionalProject.get().documents ?: fail("Project documents must not be null")
            assertThat(projectDocuments).hasSize(1)

            val document = projectDocuments[0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveProjectDocument() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has some documents") {
            testContext.document =
                    createProjectDocument(testContext.project, userUuid, "Prj doc", testContext.documentLink)
            createProjectDocument(testContext.project, userUuid, "Sec.pdf", "Sec-some-link.pdf")
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.uuid}/document/${testContext.document.id}"))
                    .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val project = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(project).isPresent
            val documents = project.get().documents
            assertThat(documents).hasSize(1).doesNotContain(testContext.document)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/main")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/gallery")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).contains(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has gallery images") {
            testContext.project.gallery = listOf("image-link-1", "image-link-2", "image-link-3")
            projectRepository.save(testContext.project)
        }

        verify("User can remove gallery image") {
            val request = ImageLinkListRequest(listOf("image-link-1"))
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.uuid}/image/gallery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
        }
        verify("Gallery image is removed") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).contains("image-link-2", "image-link-3")
            assertThat(project.gallery).doesNotContain("image-link-1")
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddNews() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User can add news link") {
            val request = ProjectUpdateRequest(news = listOf("news-link"))
            val result = mockMvc.perform(
                put("$projectPath/${testContext.project.uuid}")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val project: ProjectFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(project.news).containsAll(listOf("news-link"))
        }
        verify("News link is added to project") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.newsLinks).containsAll(listOf("news-link"))
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustThrowExceptionForTooLongProjectTags() {
        suppose("User is admin in the organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("There is a project with tags") {
            testContext.project = createProject("Project ex", organization, userUuid)
            testContext.project.tags = listOf("too long")
            projectRepository.save(testContext.project)
        }

        verify("User can add new tags to project") {
            testContext.tags = listOf("wind".repeat(50))
            testContext.projectUpdateRequest = ProjectUpdateRequest(tags = testContext.tags)
            val result = mockMvc.perform(
                put("$projectPath/${testContext.project.uuid}")
                    .content(objectMapper.writeValueAsString(testContext.projectUpdateRequest))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_DB)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var projectRequest: ProjectRequest
        lateinit var projectUpdateRequest: ProjectUpdateRequest
        lateinit var multipartFile: MockMultipartFile
        lateinit var document: Document
        val documentLink = "link"
        val imageLink = "image-link"
        lateinit var projectUuid: UUID
        lateinit var tags: List<String>
    }
}
