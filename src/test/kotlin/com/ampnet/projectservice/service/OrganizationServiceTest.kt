package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import com.ampnet.projectservice.controller.pojo.request.OrganizationUpdateRequest
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.OrganizationMembershipServiceImpl
import com.ampnet.projectservice.service.impl.OrganizationServiceImpl
import com.ampnet.projectservice.service.impl.StorageServiceImpl
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val organizationMemberServiceImpl = OrganizationMembershipServiceImpl(membershipRepository)
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, organizationMemberServiceImpl, storageServiceImpl, projectRepository)
    }
    private val organizationMembershipService: OrganizationMembershipService by lazy {
        OrganizationMembershipServiceImpl(membershipRepository)
    }
    private lateinit var organization: Organization

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("test org", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustGetOrganizationWithDocument() {
        suppose("Organization has document") {
            testContext.document = createOrganizationDocument(organization, userUuid, "test doc", "link")
        }

        verify("Service returns organization with document") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.uuid)
                ?: fail("Organization must not be null")
            assertThat(organizationWithDocument.uuid).isEqualTo(organization.uuid)
            assertThat(organizationWithDocument.documents).hasSize(1)
            val document = organizationWithDocument.documents?.first() ?: fail("Organization must have one document")
            verifyDocument(document, testContext.document)
        }
    }

    @Test
    fun mustGetOrganizationWithHeaderImage() {
        suppose("Organization has an image") {
            createOrganizationImage(organization)
        }

        verify("Service returns organization with image") {
            val organizationWithImage = organizationService.findOrganizationWithProjectCountById(organization.uuid)
                ?: fail("Organization must not be null")
            assertThat(organizationWithImage.uuid).isEqualTo(organization.uuid)
            assertThat(organizationWithImage.headerImage).isNotNull()
        }
    }

    @Test
    fun mustGetOrganizationWithMultipleDocuments() {
        suppose("Organization has 3 documents") {
            createOrganizationDocument(organization, userUuid, "Doc 1", "link1")
            createOrganizationDocument(organization, userUuid, "Doc 2", "link2")
            createOrganizationDocument(organization, userUuid, "Doc 3", "link3")
        }

        verify("Service returns organization with documents") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.uuid)
                ?: fail("Organization must not be null")
            assertThat(organizationWithDocument.uuid).isEqualTo(organization.uuid)
            assertThat(organizationWithDocument.documents).hasSize(3)
            assertThat(organizationWithDocument.documents?.map { it.link })
                .containsAll(listOf("link1", "link2", "link3"))
        }
    }

    @Test
    fun mustNotBeAbleDocumentToNonExistingOrganization() {
        verify("Service will throw an exception that organization is missing") {
            val request = DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", userUuid)
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.addDocument(UUID.randomUUID(), request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustAppendNewDocumentToCurrentListOfDocuments() {
        suppose("Organization has 2 documents") {
            createOrganizationDocument(organization, userUuid, "Doc 1", "link1")
            createOrganizationDocument(organization, userUuid, "Doc 2", "link2")
        }
        suppose("File storage service will successfully store document") {
            testContext.documentSaveRequest =
                DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", userUuid)
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.documentSaveRequest.name, testContext.documentSaveRequest.data)
            ).thenReturn(testContext.documentLink)
        }

        verify("Service will append new document") {
            val document = organizationService.addDocument(organization.uuid, testContext.documentSaveRequest)
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentSaveRequest.name)
            assertThat(document.size).isEqualTo(testContext.documentSaveRequest.size)
            assertThat(document.type).isEqualTo(testContext.documentSaveRequest.type)

            assertThat(document.link).isEqualTo(testContext.documentLink)
            assertThat(document.createdByUserUuid).isEqualTo(userUuid)
            assertThat(document.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Organization has 3 documents") {
            val organizationWithDocuments = organizationService.findOrganizationById(organization.uuid)
                ?: fail("Organization documents must not be null")
            assertThat(organizationWithDocuments.documents).hasSize(3)
            assertThat(organizationWithDocuments.documents?.map { it.link }).contains(testContext.documentLink)
        }
    }

    @Test
    fun mustNotBeAbleToRemoveOrganizationDocumentForNonExistingOrganization() {
        verify("Service will throw an exception for non existing organization") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.removeDocument(UUID.randomUUID(), 0)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustNotBeAbleToCreateOrganizationWithSameName() {
        verify("Service will throw an exception for same name exception") {
            val multipartFile = MockMultipartFile(
                "image", "image.png",
                "image/png", "ImageData".toByteArray()
            )
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val request = OrganizationServiceRequest(
                    OrganizationRequest(organization.name, "description"),
                    createUserPrincipal(userUuid), multipartFile
                )
                organizationService.createOrganization(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_DUPLICATE_NAME)
        }
    }

    @Test
    fun mustNotBeAbleToUpdateOrganizationWithNullFields() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationMembershipService
                .addUserToOrganization(userUuid, organization.uuid, OrganizationRole.ORG_MEMBER)
        }

        verify("Description and header image cannot be updated with null values") {
            val request = OrganizationUpdateServiceRequest(
                organization.uuid,
                null,
                OrganizationUpdateRequest(null)
            )
            val updatedOrganization = organizationService.updateOrganization(request)
            assertThat(updatedOrganization.description).isEqualTo(organization.description)
            assertThat(updatedOrganization.headerImage).isEqualTo(organization.headerImage)
        }
        verify("Organization is not updated in in database") {
            val storedOrganization = organizationService.findOrganizationWithProjectCountById(organization.uuid)
                ?: fail("Organization must no be null")
            assertThat(storedOrganization.description).isEqualTo(organization.description)
            assertThat(storedOrganization.headerImage).isEqualTo(organization.headerImage)
        }
    }

    private fun verifyDocument(receivedDocument: Document, savedDocument: Document) {
        assertThat(receivedDocument.id).isEqualTo(savedDocument.id)
        assertThat(receivedDocument.link).isEqualTo(savedDocument.link)
        assertThat(receivedDocument.name).isEqualTo(savedDocument.name)
        assertThat(receivedDocument.size).isEqualTo(savedDocument.size)
        assertThat(receivedDocument.type).isEqualTo(savedDocument.type)
        assertThat(receivedDocument.createdAt).isEqualTo(savedDocument.createdAt)
        assertThat(receivedDocument.createdByUserUuid).isEqualTo(savedDocument.createdByUserUuid)
    }

    private fun createOrganizationDocument(
        organization: Organization,
        createdByUserUuid: UUID,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val document = saveDocument(name, link, createdByUserUuid, type, size)
        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(document)
        organization.documents = documents
        organizationRepository.save(organization)
        return document
    }

    private fun createOrganizationImage(
        organization: Organization,
        imageName: String = "Image name",
        imageContent: ByteArray = "Image content".toByteArray()

    ) {
        cloudStorageService.saveFile(imageName, imageContent)
        organization.headerImage = imageName
        organizationRepository.save(organization)
    }

    private class TestContext {
        lateinit var document: Document
        lateinit var documentSaveRequest: DocumentSaveRequest
        val documentLink = "link"
    }
}
