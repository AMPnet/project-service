package com.ampnet.projectservice.controller

import com.ampnet.projectservice.TestBase
import com.ampnet.projectservice.config.DatabaseCleanerService
import com.ampnet.projectservice.controller.pojo.request.ProjectLocationRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ErrorResponse
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.persistence.repository.OrganizationInviteRepository
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.persistence.repository.RoleRepository
import com.ampnet.projectservice.service.CloudStorageService
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.userservice.proto.UserResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@ActiveProfiles("MailMockConfig, GrpcServiceMockConfig, CloudStorageMockConfig")
abstract class ControllerTestBase : TestBase() {

    protected val defaultEmail = "user@email.com"
    protected val userUuid: UUID = UUID.fromString("89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4")

    @Autowired
    protected lateinit var objectMapper: ObjectMapper
    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    protected lateinit var roleRepository: RoleRepository
    @Autowired
    protected lateinit var projectRepository: ProjectRepository
    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository
    @Autowired
    protected lateinit var membershipRepository: OrganizationMembershipRepository
    @Autowired
    protected lateinit var cloudStorageService: CloudStorageService
    @Autowired
    protected lateinit var organizationInviteRepository: OrganizationInviteRepository
    @Autowired
    protected lateinit var userService: UserService
    @Autowired
    protected lateinit var projectService: ProjectService
    @Autowired
    private lateinit var documentRepository: DocumentRepository

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .alwaysDo<DefaultMockMvcBuilder>(MockMvcRestDocumentation.document(
                        "{ClassName}/{methodName}",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ))
                .build()
    }

    protected fun getResponseErrorCode(errorCode: ErrorCode): String {
        return errorCode.categoryCode + errorCode.specificCode
    }

    protected fun verifyResponseErrorCode(result: MvcResult, errorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
        val expectedErrorCode = getResponseErrorCode(errorCode)
        assert(response.errCode == expectedErrorCode)
    }

    protected fun createOrganization(name: String, userUuid: UUID): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.uuid = UUID.randomUUID()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUserUuid = userUuid
        organization.documents = emptyList()
        return organizationRepository.save(organization)
    }

    protected fun addUserToOrganization(userUuid: UUID, organizationUuid: UUID, role: OrganizationRoleType) {
        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.userUuid = userUuid
        membership.organizationUuid = organizationUuid
        membership.role = roleRepository.getOne(role.id)
        membership.createdAt = ZonedDateTime.now()
        membershipRepository.save(membership)
    }

    protected fun createProject(
        name: String,
        organization: Organization,
        createdByUserUuid: UUID,
        active: Boolean = true,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: Long = 10_000_000,
        minPerUser: Long = 10,
        maxPerUser: Long = 10_000
    ): Project {
        val project = Project::class.java.getDeclaredConstructor().newInstance()
        project.uuid = UUID.randomUUID()
        project.organization = organization
        project.name = name
        project.description = "description"
        project.location = ProjectLocation(0.1, 1.0)
        project.roi = ProjectRoi(4.44, 9.99)
        project.startDate = startDate
        project.endDate = endDate
        project.expectedFunding = expectedFunding
        project.currency = Currency.EUR
        project.minPerUser = minPerUser
        project.maxPerUser = maxPerUser
        project.createdByUserUuid = createdByUserUuid
        project.active = active
        project.createdAt = startDate.minusMinutes(1)
        return projectRepository.save(project)
    }

    protected fun saveDocument(
        name: String,
        link: String,
        type: String,
        size: Int,
        createdByUserUuid: UUID
    ): Document {
        val document = Document::class.java.getDeclaredConstructor().newInstance()
        document.name = name
        document.link = link
        document.type = type
        document.size = size
        document.createdByUserUuid = createdByUserUuid
        document.createdAt = ZonedDateTime.now()
        return documentRepository.save(document)
    }

    protected fun createUserResponse(
        uuid: UUID,
        email: String = "email@mail.com",
        first: String = "First",
        last: String = "Last",
        enabled: Boolean = true
    ): UserResponse =
            UserResponse.newBuilder()
                    .setUuid(uuid.toString())
                    .setEmail(email)
                    .setFirstName(first)
                    .setLastName(last)
                    .setEnabled(enabled)
                    .build()

    protected fun createProjectRequest(organizationUuid: UUID, name: String): ProjectRequest {
        val time = ZonedDateTime.now()
        return ProjectRequest(
            organizationUuid,
            name,
            "description",
            ProjectLocationRequest(12.234, 23.432),
            ProjectRoiRequest(2.22, 7.77),
            time,
            time.plusDays(30),
            1_000_000,
            Currency.EUR,
            1,
            1_000_000,
            true
        )
    }

    protected fun createProjectDocument(
        project: Project,
        createdByUserUuid: UUID,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdByUserUuid)
        val documents = project.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        project.documents = documents
        projectRepository.save(project)
        return savedDocument
    }
}
