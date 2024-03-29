package com.ampnet.projectservice.service.impl

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InternalException
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.exception.PermissionDeniedException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.grpc.walletservice.WalletService
import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.FullProjectWithWallet
import com.ampnet.projectservice.service.pojo.ProjectServiceResponse
import com.ampnet.projectservice.service.pojo.ProjectUpdateServiceRequest
import com.ampnet.projectservice.service.pojo.ProjectValidation
import com.ampnet.projectservice.service.pojo.ProjectWithWallet
import mu.KLogging
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val storageService: StorageService,
    private val applicationProperties: ApplicationProperties,
    private val walletService: WalletService,
    private val projectTagRepository: ProjectTagRepository,
    private val organizationMembershipService: OrganizationMembershipService,
    private val organizationService: OrganizationService,
    private val organizationRepository: OrganizationRepository,
    private val imageProxyService: ImageProxyService
) : ProjectService {

    companion object : KLogging()

    @Transactional
    @Throws(InvalidRequestException::class, AccessDeniedException::class)
    override fun createProject(user: UserPrincipal, request: ProjectRequest): Project {
        throwExceptionIfUserHasNoPrivilegeToWriteInOrganization(user.uuid, request.organizationUuid)
        validateProjectRequest(ProjectValidation(request))
        logger.debug { "Creating project: ${request.name}" }
        val organization = getOrganization(request.organizationUuid)
        val project = createProjectFromRequest(user, organization, request)
        project.createdAt = ZonedDateTime.now()
        projectRepository.save(project)
        return project
    }

    @Transactional(readOnly = true)
    override fun getProjectByIdWithAllData(id: UUID): Project? {
        val project = ServiceUtils.wrapOptional(projectRepository.findByIdWithAllData(id))
            ?: return null
        Hibernate.initialize(project.gallery)
        Hibernate.initialize(project.newsLinks)
        return project
    }

    @Transactional(readOnly = true)
    override fun getAllProjectsForOrganization(organizationId: UUID, coop: String?): List<ProjectWithWallet> {
        val projects =
            projectRepository.findAllByOrganizationUuid(organizationId, coop ?: applicationProperties.coop.default)
        val walletsMap = walletService.getWalletsByOwner(projects.map { it.uuid }).associateBy { it.owner }
        return projects.map { project ->
            ProjectWithWallet(
                ProjectServiceResponse(project, imageProxyService.generateImageResponse(project.mainImage)),
                walletsMap[project.uuid]
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getAllProjects(coop: String?, pageable: Pageable): Page<ProjectServiceResponse> {
        val projects = projectRepository.findAllByCoop(coop ?: applicationProperties.coop.default, pageable)
        return projects.map { ProjectServiceResponse(it, imageProxyService.generateImageResponse(it.mainImage)) }
    }

    @Transactional(readOnly = true)
    override fun getPersonalProjects(user: UUID): List<ProjectServiceResponse> {
        val organizations = organizationRepository.findAllOrganizationsForUserUuid(user)
        if (organizations.isEmpty()) return listOf()
        val projects = projectRepository.findAllByOrganizations(organizations.map { it.uuid })
        return projects.map { ProjectServiceResponse(it, imageProxyService.generateImageResponse(it.mainImage)) }
    }

    @Transactional(readOnly = true)
    override fun getActiveProjects(coop: String?, pageable: Pageable): Page<ProjectWithWallet> {
        val activeProjects = projectRepository.findByActive(
            ZonedDateTime.now(), true, coop ?: applicationProperties.coop.default, pageable
        )
        val activeWallets = walletService.getWalletsByOwner(activeProjects.toList().map { it.uuid })
            .filter { isWalletActivate(it) }.associateBy { it.owner }
        val projectsWithWallets = activeProjects.toList().mapNotNull { project ->
            activeWallets[project.uuid]?.let { wallet ->
                ProjectWithWallet(
                    ProjectServiceResponse(project, imageProxyService.generateImageResponse(project.mainImage)),
                    wallet
                )
            }
        }
        return PageImpl(projectsWithWallets, pageable, activeProjects.totalElements)
    }

    @Transactional
    @Throws(
        InvalidRequestException::class, InternalException::class,
        ResourceNotFoundException::class, PermissionDeniedException::class
    )
    override fun updateProject(serviceRequest: ProjectUpdateServiceRequest): FullProjectWithWallet {
        val project = getProjectWithAllData(serviceRequest.projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(serviceRequest.userUuid, project.organization.uuid)
        serviceRequest.request?.let { validateProjectRequest(ProjectValidation(it)) }
        serviceRequest.request?.name?.let { project.name = it }
        serviceRequest.request?.description?.let { project.description = it }
        serviceRequest.request?.location?.let { project.location = ProjectLocation(it.lat, it.long) }
        serviceRequest.request?.roi?.let { project.roi = ProjectRoi(it.from, it.to) }
        serviceRequest.request?.startDate?.let { project.startDate = it }
        serviceRequest.request?.endDate?.let { project.endDate = it }
        serviceRequest.request?.expectedFunding?.let { project.expectedFunding = it }
        serviceRequest.request?.currency?.let { project.currency = it }
        serviceRequest.request?.minPerUser?.let { project.minPerUser = it }
        serviceRequest.request?.maxPerUser?.let { project.maxPerUser = it }
        serviceRequest.request?.active?.let { project.active = it }
        serviceRequest.request?.tags?.let { project.tags = it.toSet().map { tag -> tag.toLowerCase() } }
        serviceRequest.request?.news?.let { project.newsLinks = it }
        serviceRequest.request?.shortDescription?.let { project.shortDescription = it }
        serviceRequest.image?.let { addMainImageToProject(it, project) }
        serviceRequest.documentSaveRequests?.parallelStream()?.forEach {
            val document = storageService.saveDocument(it)
            addDocumentToProject(project, document)
        }
        val wallet = walletService.getWalletsByOwner(listOf(project.uuid))
        return FullProjectWithWallet(project, wallet.firstOrNull())
    }

    @Transactional
    @Throws(InternalException::class, AccessDeniedException::class)
    override fun addMainImage(projectUuid: UUID, userUuid: UUID, image: MultipartFile) {
        val project = getProjectWithAllData(projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(userUuid, project.organization.uuid)
        val imageName = ServiceUtils.getImageNameFromMultipartFile(image)
        val link = storageService.saveImage(imageName, image.bytes)
        project.mainImage = link
        projectRepository.save(project)
    }

    @Transactional
    @Throws(InternalException::class, AccessDeniedException::class)
    override fun addImageToGallery(projectUuid: UUID, userUuid: UUID, image: MultipartFile) {
        val project = getProjectWithAllData(projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(userUuid, project.organization.uuid)
        val gallery = project.gallery.orEmpty().toMutableList()
        val imageName = ServiceUtils.getImageNameFromMultipartFile(image)
        val link = storageService.saveImage(imageName, image.bytes)
        gallery.add(link)
        setProjectGallery(project, gallery)
    }

    @Transactional
    @Throws(InternalException::class, AccessDeniedException::class)
    override fun removeImagesFromGallery(projectUuid: UUID, userUuid: UUID, images: List<String>) {
        val project = getProjectWithAllData(projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(userUuid, project.organization.uuid)
        val gallery = project.gallery.orEmpty().toMutableList()
        images.forEach {
            if (gallery.remove(it)) {
                storageService.deleteImage(it)
            }
        }
        setProjectGallery(project, gallery)
    }

    @Transactional
    @Throws(InternalException::class, AccessDeniedException::class)
    override fun addDocument(projectUuid: UUID, request: DocumentSaveRequest): Document {
        val project = getProjectWithAllData(projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(request.userUuid, project.organization.uuid)
        val document = storageService.saveDocument(request)
        val updatedProject = addDocumentToProject(project, document)
        projectRepository.save(updatedProject)
        return document
    }

    @Transactional
    @Throws(InternalException::class, AccessDeniedException::class)
    override fun removeDocument(projectUuid: UUID, userUuid: UUID, documentId: Int) {
        val project = getProjectWithAllData(projectUuid)
        throwExceptionIfUserHasNoPrivilegeToWriteInProject(userUuid, project.organization.uuid)
        val storedDocuments = project.documents.orEmpty().toMutableList()
        storedDocuments.firstOrNull { it.id == documentId }?.let {
            storedDocuments.remove(it)
            project.documents = storedDocuments
            projectRepository.save(project)
            storageService.deleteFile(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllProjectTags(coop: String?): List<String> {
        return projectTagRepository.getAllTagsByCoop(coop ?: applicationProperties.coop.default)
    }

    @Transactional(readOnly = true)
    override fun getProjectsByTags(
        tags: List<String>,
        coop: String?,
        pageable: Pageable,
        active: Boolean
    ): Page<ProjectServiceResponse> {
        val projects = projectRepository.findByTags(
            tags, tags.size.toLong(), coop ?: applicationProperties.coop.default, active, pageable
        )
        return projects.map { ProjectServiceResponse(it, imageProxyService.generateImageResponse(it.mainImage)) }
    }

    @Transactional(readOnly = true)
    override fun getProjectWithWallet(id: UUID): FullProjectWithWallet? {
        val project = getProjectByIdWithAllData(id) ?: return null
        val wallet = walletService.getWalletsByOwner(listOf(project.uuid))
        return FullProjectWithWallet(project, wallet.firstOrNull())
    }

    @Transactional(readOnly = true)
    override fun countActiveProjects(coop: String?): Int =
        projectRepository.countAllActiveByDate(
            ZonedDateTime.now(), true, coop ?: applicationProperties.coop.default
        )

    @Suppress("ThrowsCount")
    private fun validateProjectRequest(projectValidation: ProjectValidation) {
        projectValidation.endDate?.let {
            if (it.isBefore(projectValidation.startDate)) {
                throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before start date")
            }
            if (it.isBefore(ZonedDateTime.now())) {
                throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before present date")
            }
        }
        if (projectValidation.minPerUser != null && projectValidation.maxPerUser != null &&
            projectValidation.minPerUser > projectValidation.maxPerUser
        )
            throw InvalidRequestException(
                ErrorCode.PRJ_MIN_ABOVE_MAX,
                "Min per user: ${projectValidation.minPerUser} > Max per user: ${projectValidation.maxPerUser}"
            )
        if (projectValidation.maxPerUser != null && projectValidation.expectedFunding != null &&
            projectValidation.maxPerUser > projectValidation.expectedFunding
        )
            throw InvalidRequestException(
                ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH,
                "Max per user: ${projectValidation.maxPerUser} " +
                    "> Expected funding: ${projectValidation.expectedFunding} "
            )
        projectValidation.expectedFunding?.let {
            if (applicationProperties.investment.maxPerProject <= it) {
                throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH,
                    "Max expected funding is: ${applicationProperties.investment.maxPerProject}"
                )
            }
        }
        projectValidation.maxPerUser?.let {
            if (applicationProperties.investment.maxPerUser <= projectValidation.maxPerUser) {
                throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH,
                    "Max funds per user is: ${applicationProperties.investment.maxPerUser}"
                )
            }
        }
        projectValidation.roi?.let {
            if (it.from > it.to) {
                throw InvalidRequestException(ErrorCode.PRJ_ROI, "ROI from is bigger than ROI to")
            }
        }
    }

    private fun addDocumentToProject(project: Project, document: Document): Project {
        val documents = project.documents.orEmpty().toMutableList()
        documents += document
        project.documents = documents
        return project
    }

    private fun addMainImageToProject(image: MultipartFile, project: Project) {
        val link = storageService.saveImage(
            ServiceUtils.getImageNameFromMultipartFile(image), image.bytes
        )
        project.mainImage = link
    }

    private fun createProjectFromRequest(user: UserPrincipal, organization: Organization, request: ProjectRequest) =
        Project(
            UUID.randomUUID(),
            organization,
            request.name,
            request.description,
            request.location?.let { ProjectLocation(request.location.lat, request.location.long) },
            request.roi?.let { ProjectRoi(request.roi.from, request.roi.to) },
            request.startDate,
            request.endDate,
            request.expectedFunding,
            request.currency,
            request.minPerUser,
            request.maxPerUser,
            null,
            null,
            null,
            user.uuid,
            ZonedDateTime.now(),
            request.active,
            null,
            request.tags?.toSet()?.map { it.toLowerCase() },
            user.coop,
            request.shortDescription
        )

    private fun setProjectGallery(project: Project, gallery: List<String>) {
        project.gallery = gallery
        projectRepository.save(project)
    }

    private fun isWalletActivate(walletResponse: WalletServiceResponse): Boolean {
        return walletResponse.hash.isNotEmpty()
    }

    private fun getUserMembershipInOrganization(userUuid: UUID, organizationUuid: UUID): OrganizationMembership? =
        organizationMembershipService.getOrganizationMemberships(organizationUuid).find { it.userUuid == userUuid }

    private fun throwExceptionIfUserHasNoPrivilegeToWriteInProject(
        userUuid: UUID,
        organizationUuid: UUID
    ) {
        val orgMembership = getUserMembershipInOrganization(userUuid, organizationUuid)
            ?: throw PermissionDeniedException(
                ErrorCode.ORG_MEM_MISSING,
                "User $userUuid is not a member of organization $organizationUuid"
            )
        if (orgMembership.hasPrivilegeToWriteProject().not()) {
            throw PermissionDeniedException(
                ErrorCode.PRJ_WRITE_PRIVILEGE,
                "User does not have organization privilege to write in project: PW_PROJECT"
            )
        }
    }

    private fun throwExceptionIfUserHasNoPrivilegeToWriteInOrganization(
        userUuid: UUID,
        organizationUuid: UUID
    ) {
        val orgMembership = getUserMembershipInOrganization(userUuid, organizationUuid)
            ?: throw PermissionDeniedException(
                ErrorCode.ORG_MEM_MISSING,
                "User $userUuid is not a member of organization $organizationUuid"
            )
        if (orgMembership.hasPrivilegeToWriteOrganization().not()) {
            throw PermissionDeniedException(
                ErrorCode.PRJ_WRITE_PRIVILEGE,
                "User does not have organization privilege to write organization: PW_ORG"
            )
        }
    }

    private fun getProjectWithAllData(projectUuid: UUID): Project =
        getProjectByIdWithAllData(projectUuid)
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectUuid")

    private fun getOrganization(organizationUuid: UUID): Organization =
        organizationService.findOrganizationById(organizationUuid)
            ?: throw ResourceNotFoundException(
                ErrorCode.ORG_MISSING, "Missing organization with id: $organizationUuid"
            )
}
