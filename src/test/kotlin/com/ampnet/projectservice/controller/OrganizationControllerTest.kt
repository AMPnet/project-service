package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.security.WithMockCrowdfoundUser
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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"

    @Autowired
    private lateinit var organizationService: OrganizationService

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
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
            assertThat(organization.headerImage).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(organization.uuid).isNotNull()
            assertThat(organization.approved).isTrue()
            assertThat(organization.documents).isEmpty()
            assertThat(organization.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has document") {
            createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
        }

        verify("User can get organization with id") {
            val result = mockMvc.perform(get("$organizationPath/${testContext.organization.uuid}"))
                .andExpect(status().isOk)
                .andReturn()

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationWithDocumentResponse.legalInfo).isEqualTo(testContext.organization.legalInfo)
            assertThat(organizationWithDocumentResponse.uuid).isEqualTo(testContext.organization.uuid)
            assertThat(organizationWithDocumentResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationWithDocumentResponse.documents.size)
                .isEqualTo(testContext.organization.documents?.size)
            assertThat(organizationWithDocumentResponse.createdAt).isEqualTo(testContext.organization.createdAt)
        }
    }

    @Test
    @WithMockCrowdfoundUser
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
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForNonExistingOrganization() {
        verify("Response not found for non existing organization") {
            mockMvc.perform(get("$organizationPath/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPersonalOrganizations() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a member of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
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
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteOrganizationMember() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Organization has a member") {
            testContext.member = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can delete organization member") {
            mockMvc.perform(
                delete("$organizationPath/${testContext.organization.uuid}/members/${testContext.member}")
            )
                .andExpect(status().isOk)
        }
        verify("Member is delete from organization") {
            val memberships = membershipRepository.findByOrganizationUuid(testContext.organization.uuid)
            assertThat(memberships).hasSize(1)
            assertThat(memberships[0].userUuid).isNotEqualTo(testContext.member)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganizationMembers() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Organization has two members") {
            testContext.member = UUID.randomUUID()
            testContext.memberSecond = UUID.randomUUID()
            addUserToOrganization(testContext.member, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
            addUserToOrganization(
                testContext.memberSecond, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN
            )
        }
        suppose("User service will return user data") {
            val userResponse = createUserResponse(testContext.memberSecond, "email@mail.com", "first", "last", true)
            val memberResponse = createUserResponse(testContext.member, "email@mail.com", "ss", "ll", true)
            testContext.userResponses = listOf(userResponse, memberResponse)
            Mockito.`when`(userService.getUsers(listOf(testContext.memberSecond, testContext.member)))
                .thenReturn(testContext.userResponses)
            Mockito.`when`(userService.getUsers(listOf(testContext.member, testContext.memberSecond)))
                .thenReturn(testContext.userResponses)
        }

        verify("Controller returns all organization members") {
            val result = mockMvc.perform(get("$organizationPath/${testContext.organization.uuid}/members"))
                .andExpect(status().isOk)
                .andReturn()

            val members: OrganizationMembershipsResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(members.members.map { it.uuid }).hasSize(2)
                .containsAll(listOf(testContext.memberSecond, testContext.member))
            assertThat(members.members.map { it.role }).hasSize(2)
                .containsAll(listOf(OrganizationRoleType.ORG_ADMIN.name, OrganizationRoleType.ORG_MEMBER.name))
            assertThat(members.members.map { it.firstName }).containsAll(testContext.userResponses.map { it.firstName })
            assertThat(members.members.map { it.lastName }).containsAll(testContext.userResponses.map { it.lastName })
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToStoreDocumentForOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
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
                fileUpload("$organizationPath/${testContext.organization.uuid}/document")
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

            val document = organizationDocuments[0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
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
            addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_ADMIN)
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

    private fun createOrganizationDocument(
        organization: Organization,
        createdByUserUuid: UUID,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdByUserUuid)
        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        organization.documents = documents
        organizationRepository.save(organization)
        return savedDocument
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
    }
}
