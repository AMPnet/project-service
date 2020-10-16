package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import com.ampnet.projectservice.controller.pojo.request.OrganizationUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.security.WithMockCrowdfundUser
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.userservice.proto.UserResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"
    private val updates = "/updates"

    @Autowired
    private lateinit var organizationService: OrganizationService

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToCreateOrganization() {
        suppose("Organization does not exist") {
            databaseCleanerService.deleteAllOrganizations()
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.imageLink)
        }

        verify("User can create organization") {
            val name = "Organization name"
            val description = "Organization description"
            testContext.organizationRequest = OrganizationRequest(name, description)
            val organizationRequestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                jacksonObjectMapper().writeValueAsBytes(testContext.organizationRequest)
            )
            val result = mockMvc.perform(
                multipart(organizationPath)
                    .file(testContext.multipartFile)
                    .file(organizationRequestJson)
            )
                .andExpect(status().isOk)
                .andReturn()

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organizationWithDocumentResponse.description).isEqualTo(testContext.organizationRequest.description)
            assertThat(organizationWithDocumentResponse.headerImage).isEqualTo(testContext.imageLink)
            assertThat(organizationWithDocumentResponse.uuid).isNotNull()
            assertThat(organizationWithDocumentResponse.approved).isTrue()
            assertThat(organizationWithDocumentResponse.documents).isEmpty()
            assertThat(organizationWithDocumentResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.createdOrganizationUuid = organizationWithDocumentResponse.uuid
        }
        verify("Organization is stored in database") {
            val organization = organizationService.findOrganizationById(testContext.createdOrganizationUuid)
                ?: fail("Organization must no be null")
            assertThat(organization.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organization.description).isEqualTo(testContext.organizationRequest.description)
            assertThat(organization.headerImage).isEqualTo(testContext.imageLink)
            assertThat(organization.uuid).isNotNull()
            assertThat(organization.approved).isTrue()
            assertThat(organization.documents).isEmpty()
            assertThat(organization.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnListOfOrganizations() {
        suppose("Multiple organizations exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
            createOrganization("test 2", userUuid)
            createOrganization("test 3", userUuid)
        }

        verify("User can get all organizations") {
            val result = mockMvc.perform(
                get(organizationPath)
                    .param("size", "10")
                    .param("page", "0")
                    .param("sort", "createdAt,desc")
            )
                .andExpect(status().isOk)
                .andReturn()

            val organizationResponse: OrganizationListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.organizations).hasSize(3)
            assertThat(organizationResponse.organizations.map { it.name }).contains(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustReturnNotFoundForNonExistingOrganization() {
        verify("Response not found for non existing organization") {
            mockMvc.perform(get("$organizationPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetPersonalOrganizations() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a member of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        }
        suppose("Another organization exists") {
            createOrganization("new organization", userUuid)
        }

        verify("User will organization that he is a member") {
            val result = mockMvc.perform(get("$organizationPath/personal"))
                .andExpect(status().isOk)
                .andReturn()

            val organizationResponse: OrganizationListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.organizations).hasSize(1)
            assertThat(organizationResponse.organizations.map { it.name }).contains(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has document") {
            createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
        }

        verify("User can get organization with id") {
            val result = mockMvc.perform(MockMvcRequestBuilders.get("$organizationPath/${testContext.organization.uuid}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationWithDocumentResponse.description).isEqualTo(testContext.organization.description)
            assertThat(organizationWithDocumentResponse.headerImage).isEqualTo(testContext.organization.headerImage)
            assertThat(organizationWithDocumentResponse.uuid).isEqualTo(testContext.organization.uuid)
            assertThat(organizationWithDocumentResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationWithDocumentResponse.documents.size)
                .isEqualTo(testContext.organization.documents?.size)
            assertThat(organizationWithDocumentResponse.createdAt).isEqualTo(testContext.organization.createdAt)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToStoreDocumentForOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile(
                "file", "test.txt",
                "text/plain", "Some document data".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.documentLink)
        }

        verify("User can add document to organization") {
            val result = mockMvc.perform(
                multipart("$organizationPath/${testContext.organization.uuid}/document")
                    .file(testContext.multipartFile)
            )
                .andExpect(status().isOk)
                .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to organization") {
            val organizationDocuments = organizationService.findOrganizationById(testContext.organization.uuid)?.documents
                ?: fail("Organization documents must not be null")
            assertThat(organizationDocuments).hasSize(1)

            val document = organizationDocuments.first()
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToDeleteOrganizationDocument() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has 2 documents") {
            testContext.document =
                createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
            createOrganizationDocument(testContext.organization, userUuid, "second.pdf", "second-link.pdf")
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                delete("$organizationPath/${testContext.organization.uuid}/document/${testContext.document.id}")
            )
                .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val organizationWithDocument = organizationService.findOrganizationById(testContext.organization.uuid)
            assertThat(organizationWithDocument?.documents).hasSize(1).doesNotContain(testContext.document)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToUpdateOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            Mockito.`when`(
                cloudStorageService.saveFile(
                    testContext.multipartFile.originalFilename,
                    testContext.multipartFile.bytes
                )
            ).thenReturn(testContext.imageLink)
        }

        verify("User can update organization") {
            val description = "Organization description"
            testContext.organizationUpdateRequest = OrganizationUpdateRequest(description)
            val organizationRequestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                jacksonObjectMapper().writeValueAsBytes(testContext.organizationUpdateRequest)
            )
            val result = mockMvc.perform(
                multipart("$organizationPath/${testContext.organization.uuid}$updates")
                    .file(organizationRequestJson)
                    .file(testContext.multipartFile)
            )
                .andExpect(status().isOk)
                .andReturn()

            val organizationResponse: OrganizationResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationResponse.description).isEqualTo(testContext.organizationUpdateRequest.description)
            assertThat(organizationResponse.headerImage).isEqualTo(testContext.imageLink)
            assertThat(organizationResponse.uuid).isNotNull()
            assertThat(organizationResponse.approved).isTrue()
            assertThat(organizationResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.createdOrganizationUuid = organizationResponse.uuid
        }
        verify("Organization is stored in database") {
            val organization = organizationService.findOrganizationById(testContext.createdOrganizationUuid)
                ?: fail("Organization must no be null")
            assertThat(organization.name).isEqualTo(testContext.organization.name)
            assertThat(organization.description).isEqualTo(testContext.organizationUpdateRequest.description)
            assertThat(organization.headerImage).isEqualTo(testContext.imageLink)
            assertThat(organization.uuid).isNotNull()
            assertThat(organization.approved).isTrue()
            assertThat(organization.documents).isEmpty()
            assertThat(organization.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    private class TestContext {
        lateinit var organizationRequest: OrganizationRequest
        lateinit var createdOrganizationUuid: UUID
        lateinit var organization: Organization
        val documentLink = "link"
        lateinit var document: Document
        lateinit var multipartFile: MockMultipartFile
        lateinit var member: UUID
        lateinit var memberSecond: UUID
        var userResponses: List<UserResponse> = emptyList()
        val imageLink = "image link"
        lateinit var organizationUpdateRequest: OrganizationUpdateRequest
    }
}
