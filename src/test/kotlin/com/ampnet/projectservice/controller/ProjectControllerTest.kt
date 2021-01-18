package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectLocationRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectWithWalletFullResponse
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ErrorResponse
import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

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
    @WithMockCrowdfundUser
    fun mustReturnErrorForUserWithoutOrganizationMembership() {
        verify("Controller will forbidden for user without membership to create project") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnErrorForUserOrganizationMembership() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(organization.uuid, "Error project")
            mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnErrorIfCreatingProjectForMissingOrganization() {
        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(UUID.randomUUID(), "Error project")
            mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToCreateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Controller will return create project") {
            testContext.projectRequest = createProjectRequest(organization.uuid, "Das project")
            val result = mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(testContext.projectRequest))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
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
            assertThat(projectResponse.coop).isEqualTo(COOP)
            assertThat(projectResponse.shortDescription).isEqualTo(testContext.projectRequest.shortDescription)
            assertThat(projectResponse.organization.uuid).isEqualTo(organization.uuid)
            assertThat(projectResponse.wallet).isNull()

            testContext.projectUuid = projectResponse.uuid
        }
        verify("Project is stored in database") {
            val optionalProject = projectRepository.findById(testContext.projectUuid)
            assertThat(optionalProject).isPresent
            val project = optionalProject.get()
            assertThat(project.name).isEqualTo(testContext.projectRequest.name)
            assertThat(project.description).isEqualTo(testContext.projectRequest.description)
            assertThat(project.location.lat).isEqualTo(testContext.projectRequest.location.lat)
            assertThat(project.location.long).isEqualTo(testContext.projectRequest.location.long)
            assertThat(project.roi.from).isEqualTo(testContext.projectRequest.roi.from)
            assertThat(project.roi.to).isEqualTo(testContext.projectRequest.roi.to)
            assertThat(project.startDate).isEqualTo(testContext.projectRequest.startDate)
            assertThat(project.endDate).isEqualTo(testContext.projectRequest.endDate)
            assertThat(project.expectedFunding).isEqualTo(testContext.projectRequest.expectedFunding)
            assertThat(project.currency).isEqualTo(testContext.projectRequest.currency)
            assertThat(project.minPerUser).isEqualTo(testContext.projectRequest.minPerUser)
            assertThat(project.maxPerUser).isEqualTo(testContext.projectRequest.maxPerUser)
            assertThat(project.createdByUserUuid).isEqualTo(userUuid)
            assertThat(project.organization.uuid).isEqualTo(organization.uuid)
            assertThat(project.active).isEqualTo(testContext.projectRequest.active)
            assertThat(project.coop).isEqualTo(COOP)
            assertThat(project.shortDescription).isEqualTo(testContext.projectRequest.shortDescription)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustRejectRequestWithNegativeProjectValues() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("Controller will reject project") {
            testContext.projectRequest = createProjectRequest(
                organization.uuid, "Das project",
                expectedFunding = -100000, minPerUser = -100, maxPerUser = -10000
            )
            val result = mockMvc.perform(
                post(projectPath)
                    .content(objectMapper.writeValueAsString(testContext.projectRequest))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            val errorResponse: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(errorResponse.errCode).isEqualTo(getResponseErrorCode(ErrorCode.INT_REQUEST))
            assertThat(errorResponse.errors).hasSize(3)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("File service will store image") {
            testContext.imageMock = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.imageMock.originalFilename,
                    testContext.imageMock.bytes
                )
            ).thenReturn(testContext.imageLink)
        }
        suppose("File service will store documents") {
            testContext.documentMock1 = MockMultipartFile(
                "documents", "test.txt",
                "text/plain", "Document with bigger size".toByteArray()
            )
            testContext.documentMock2 = MockMultipartFile(
                "documents", "test2.pdf",
                "application/pdf", "Smaller document".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.documentMock1.originalFilename,
                    testContext.documentMock1.bytes
                )
            ).thenReturn(testContext.documentLink1)
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.documentMock2.originalFilename,
                    testContext.documentMock2.bytes
                )
            ).thenReturn(testContext.documentLink2)
        }
        suppose("Wallet service will return project wallet") {
            testContext.activeWallet = createWalletResponse(userUuid, testContext.project.uuid)
            Mockito.`when`(walletService.getWalletsByOwner(listOf(testContext.project.uuid)))
                .thenReturn(listOf(testContext.activeWallet))
        }

        verify("Admin can update project") {
            testContext.projectUpdateRequest = ProjectUpdateRequest(
                "new name", "description",
                ProjectLocationRequest(22.1, 0.3), ProjectRoiRequest(1.11, 5.55), false, listOf("tag"),
                shortDescription = "new short description"
            )
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(testContext.imageMock)
                    .file(testContext.documentMock1)
                    .file(testContext.documentMock2)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(projectResponse.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(projectResponse.location.lat).isEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(projectResponse.location.long).isEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(projectResponse.roi.from).isEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(projectResponse.roi.to).isEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(projectResponse.active).isEqualTo(testContext.projectUpdateRequest.active)
            assertThat(projectResponse.tags).containsAll(testContext.projectUpdateRequest.tags)
            assertThat(projectResponse.mainImage).contains(testContext.imageLink)
            assertThat(projectResponse.documents).hasSize(2)
            assertThat(projectResponse.coop).isEqualTo(COOP)
            assertThat(projectResponse.shortDescription).isEqualTo(testContext.projectUpdateRequest.shortDescription)
            assertThat(projectResponse.organization.uuid).isEqualTo(testContext.project.organization.uuid)
            val documents = projectResponse.documents.sortedByDescending { it.size }
            val document = documents.first()
            assertThat(document.id).isNotNull()
            assertThat(document.link).isEqualTo(testContext.documentLink1)
            assertThat(document.name).isEqualTo(testContext.documentMock1.originalFilename)
            assertThat(document.size).isEqualTo(testContext.documentMock1.size)
            assertThat(document.type).isEqualTo(testContext.documentMock1.contentType)
            val wallet = projectResponse.wallet ?: fail("Project does not have a wallet")
            assertThat(wallet.uuid).isEqualTo(testContext.activeWallet.uuid)
            assertThat(wallet.owner).isEqualTo(testContext.project.uuid)
        }
        verify("Project is updated") {
            val updatedProject = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(updatedProject.name).isEqualTo(testContext.projectUpdateRequest.name)
            assertThat(updatedProject.description).isEqualTo(testContext.projectUpdateRequest.description)
            assertThat(updatedProject.location.lat).isEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(updatedProject.location.long).isEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(updatedProject.roi.from).isEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(updatedProject.roi.to).isEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(updatedProject.active).isEqualTo(testContext.projectUpdateRequest.active)
            assertThat(updatedProject.tags).containsAll(testContext.projectUpdateRequest.tags)
            assertThat(updatedProject.mainImage).contains(testContext.imageLink)
            assertThat(updatedProject.shortDescription).isEqualTo(testContext.projectUpdateRequest.shortDescription)
            val documents = updatedProject.documents?.sortedByDescending { it.size } ?: fail("Missing documents")
            assertThat(documents).hasSize(2)
            val document = documents.last()
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentMock2.originalFilename)
            assertThat(document.size).isEqualTo(testContext.documentMock2.size)
            assertThat(document.type).isEqualTo(testContext.documentMock2.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink2)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateProjectMainImage() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("File service will store image") {
            testContext.imageMock = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.imageMock.originalFilename,
                    testContext.imageMock.bytes
                )
            ).thenReturn(testContext.imageLink)
        }

        verify("Admin can update project image") {
            testContext.projectUpdateRequest =
                ProjectUpdateRequest(null, null, null, null, null)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(requestJson)
                    .file(testContext.imageMock)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.uuid).isEqualTo(testContext.project.uuid)
            assertThat(projectResponse.name).isNotEqualTo(testContext.projectUpdateRequest.name)
            assertThat(projectResponse.description).isNotEqualTo(testContext.projectUpdateRequest.description)
            assertThat(projectResponse.location.lat).isNotEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(projectResponse.location.long).isNotEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(projectResponse.roi.from).isNotEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(projectResponse.roi.to).isNotEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(projectResponse.active).isNotEqualTo(testContext.projectUpdateRequest.active)
            assertThat(projectResponse.tags).isNotEqualTo(testContext.projectUpdateRequest.tags)
            assertThat(projectResponse.mainImage).contains(testContext.imageLink)
            assertThat(projectResponse.coop).isEqualTo(COOP)
            assertThat(projectResponse.shortDescription).isNotEqualTo(testContext.projectUpdateRequest.shortDescription)
        }
        verify("Only project image is updated") {
            val updatedProject = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(updatedProject.name).isNotEqualTo(testContext.projectUpdateRequest.name)
            assertThat(updatedProject.description).isNotEqualTo(testContext.projectUpdateRequest.description)
            assertThat(updatedProject.location.lat).isNotEqualTo(testContext.projectUpdateRequest.location?.lat)
            assertThat(updatedProject.location.long).isNotEqualTo(testContext.projectUpdateRequest.location?.long)
            assertThat(updatedProject.roi.from).isNotEqualTo(testContext.projectUpdateRequest.roi?.from)
            assertThat(updatedProject.roi.to).isNotEqualTo(testContext.projectUpdateRequest.roi?.to)
            assertThat(updatedProject.active).isNotEqualTo(testContext.projectUpdateRequest.active)
            assertThat(updatedProject.tags).isNotEqualTo(testContext.projectUpdateRequest.tags)
            assertThat(updatedProject.mainImage).contains(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateProjectWithDocuments() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("File service will store documents") {
            testContext.documentMock1 = MockMultipartFile(
                "documents", "test.txt",
                "text/plain", "Document with bigger size".toByteArray()
            )
            testContext.documentMock2 = MockMultipartFile(
                "documents", "test2.pdf",
                "application/pdf", "Smaller document".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.documentMock1.originalFilename,
                    testContext.documentMock1.bytes
                )
            ).thenReturn(testContext.documentLink1)
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.documentMock2.originalFilename,
                    testContext.documentMock2.bytes
                )
            ).thenReturn(testContext.documentLink2)
        }

        verify("Admin can update project") {
            testContext.projectUpdateRequest = ProjectUpdateRequest(null, null, null, null, null)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(testContext.documentMock1)
                    .file(testContext.documentMock2)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.documents).hasSize(2)
            val documents = projectResponse.documents.sortedByDescending { it.size }
            val document = documents.first()
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentMock1.originalFilename)
            assertThat(document.size).isEqualTo(testContext.documentMock1.size)
            assertThat(document.type).isEqualTo(testContext.documentMock1.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink1)
        }
        verify("Project is updated") {
            val updatedProject = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            val documents = updatedProject.documents?.sortedByDescending { it.size } ?: fail("Missing documents")
            assertThat(documents).hasSize(2)
            documents.sortedBy { it.size }
            val document = documents.last()
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentMock2.originalFilename)
            assertThat(document.size).isEqualTo(testContext.documentMock2.size)
            assertThat(document.type).isEqualTo(testContext.documentMock2.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink2)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateProjectWithTermsOfService() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("File service will store documents") {
            testContext.termsOfService = MockMultipartFile(
                "termsOfService", "ToC.txt",
                "text/plain", "Terms of service".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.termsOfService.originalFilename,
                    testContext.termsOfService.bytes
                )
            ).thenReturn(testContext.termsOfServiceLink)
        }

        verify("Admin can update project") {
            testContext.projectUpdateRequest = ProjectUpdateRequest(null, null, null, null, null)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(testContext.termsOfService)
            )
                .andExpect(status().isOk)
                .andReturn()

            val projectResponse: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.termsOfService?.link).isEqualTo(testContext.termsOfServiceLink)
        }
        verify("Project is updated") {
            val updatedProject = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            val termsOfServiceDocument = updatedProject.termsOfService ?: fail("Missing terms of service document")
            assertThat(termsOfServiceDocument.id).isNotNull()
            assertThat(termsOfServiceDocument.name).isEqualTo(testContext.termsOfService.originalFilename)
            assertThat(termsOfServiceDocument.size).isEqualTo(testContext.termsOfService.size)
            assertThat(termsOfServiceDocument.type).isEqualTo(testContext.termsOfService.contentType)
            assertThat(termsOfServiceDocument.link).isEqualTo(testContext.termsOfServiceLink)
        }
        verify("Old terms of services is deleted") {
            val tosId = testContext.project.termsOfService?.id ?: fail("Missing project old tos")
            val document = documentRepository.findById(tosId)
            assertThat(document).isEmpty
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAddProjectTags() {
        suppose("User is admin in the organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("There is a project with tags") {
            testContext.project = createProject("Projectos", organization, userUuid)
            testContext.project.tags = listOf("wind", "green")
            projectRepository.save(testContext.project)
        }

        verify("User can add new tags to project") {
            testContext.tags = listOf("wind", "green", "gg")
            testContext.projectUpdateRequest = ProjectUpdateRequest(tags = testContext.tags)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
            )
                .andExpect(status().isOk)
                .andReturn()

            val project: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(project.tags).containsAll(testContext.tags)
        }
        verify("Tags are added to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().tags).containsAll(testContext.tags)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnForbiddenIfUserIsMissingOrgPrivileges() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("User cannot update project") {
            testContext.projectUpdateRequest =
                ProjectUpdateRequest(
                    "new name", "description",
                    ProjectLocationRequest(12.234, 23.432), ProjectRoiRequest(4.44, 8.88), false
                )
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            mockMvc.perform(
                builder.file(requestJson)
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnErrorForUpdatingNonExistingProject() {
        verify("User cannot update non existing project") {
            testContext.projectUpdateRequest =
                ProjectUpdateRequest("new name", "description", null, null, false)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder =
                multipart("$projectPath/${UUID.randomUUID()}")
            builder.with { request ->
                request.method = "PUT"
                request
            }
            val response = mockMvc.perform(
                builder.file(requestJson)
            )
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAddDocumentForProject() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("File service will store document") {
            testContext.documentMock1 = MockMultipartFile(
                "file", "test.txt",
                "text/plain", "Some document data".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.documentMock1.originalFilename,
                    testContext.documentMock1.bytes
                )
            ).thenReturn(testContext.documentLink1)
        }

        verify("User can add document") {
            val result = mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/document")
                    .file(testContext.documentMock1)
            )
                .andExpect(status().isOk)
                .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.documentMock1.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.documentMock1.size)
            assertThat(documentResponse.type).isEqualTo(testContext.documentMock1.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink1)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            val projectDocuments = optionalProject.get().documents ?: fail("Project documents must not be null")
            assertThat(projectDocuments).hasSize(1)

            val document = projectDocuments[0]
            assertThat(document.name).isEqualTo(testContext.documentMock1.originalFilename)
            assertThat(document.size).isEqualTo(testContext.documentMock1.size)
            assertThat(document.type).isEqualTo(testContext.documentMock1.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink1)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRemoveProjectDocument() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("Project has some documents") {
            testContext.document =
                createProjectDocument(testContext.project, userUuid, "Prj doc", testContext.documentLink1)
            createProjectDocument(testContext.project, userUuid, "Sec.pdf", "Sec-some-link.pdf")
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                delete("$projectPath/${testContext.project.uuid}/document/${testContext.document.id}")
            )
                .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val project = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(project).isPresent
            val documents = project.get().documents
            assertThat(documents).hasSize(1).doesNotContain(testContext.document)
            val document = documentRepository.findById(testContext.document.id)
            assertThat(document).isEmpty
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRemoveProjectTermsOfServices() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("User admin can delete document") {
            val tosId = testContext.project.termsOfService?.id ?: fail("Missing project tos")
            mockMvc.perform(
                delete("$projectPath/${testContext.project.uuid}/document/$tosId")
            )
                .andExpect(status().isOk)
        }
        verify("Terms of services is deleted") {
            val tosId = testContext.project.termsOfService?.id ?: fail("Missing project tos")
            val document = documentRepository.findById(tosId)
            assertThat(document).isEmpty
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.imageMock = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.imageMock.originalFilename,
                    testContext.imageMock.bytes
                )
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/main")
                    .file(testContext.imageMock)
            )
                .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToAddGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.imageMock = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.imageMock.originalFilename,
                    testContext.imageMock.bytes
                )
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.uuid}/image/gallery")
                    .file(testContext.imageMock)
            )
                .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.gallery).contains(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToRemoveGalleryImage() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
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
                    .content(objectMapper.writeValueAsString(request))
            )
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
    @WithMockCrowdfundUser
    fun mustBeAbleToAddNews() {
        suppose("Project exists") {
            testContext.project = createProject("Project", organization, userUuid)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("User can add news link") {

            testContext.projectUpdateRequest = ProjectUpdateRequest(news = listOf("news-link"))
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
            )
                .andExpect(status().isOk)
                .andReturn()

            val project: ProjectWithWalletFullResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(project.news).containsAll(listOf("news-link"))
        }
        verify("News link is added to project") {
            val project = projectService.getProjectByIdWithAllData(testContext.project.uuid)
                ?: fail("Missing project")
            assertThat(project.newsLinks).containsAll(listOf("news-link"))
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustThrowExceptionForTooLongProjectTags() {
        suppose("User is admin in the organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("There is a project with tags") {
            testContext.project = createProject("Project ex", organization, userUuid)
            testContext.project.tags = listOf("too long")
            projectRepository.save(testContext.project)
        }

        verify("User can add new tags to project") {
            testContext.tags = listOf("wind".repeat(50))
            testContext.projectUpdateRequest = ProjectUpdateRequest(tags = testContext.tags)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(testContext.projectUpdateRequest)
            )
            val builder = getPutMultipartRequestBuilder()
            val result = mockMvc.perform(
                builder.file(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_DB)
        }
    }

    private fun getPutMultipartRequestBuilder(): MockMultipartHttpServletRequestBuilder {
        return multipart("$projectPath/${testContext.project.uuid}").apply {
            with { request ->
                request.method = "PUT"
                request
            }
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var projectRequest: ProjectRequest
        lateinit var projectUpdateRequest: ProjectUpdateRequest
        lateinit var imageMock: MockMultipartFile
        lateinit var document: Document
        lateinit var documentMock1: MockMultipartFile
        lateinit var documentMock2: MockMultipartFile
        lateinit var termsOfService: MockMultipartFile
        val documentLink1 = "document-link1"
        val documentLink2 = "document-link2"
        val imageLink = "image-link"
        val termsOfServiceLink = "terms-of-service-link"
        lateinit var projectUuid: UUID
        lateinit var tags: List<String>
        lateinit var activeWallet: WalletServiceResponse
    }
}
