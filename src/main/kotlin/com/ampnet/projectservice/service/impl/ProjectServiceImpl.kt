package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import java.time.ZonedDateTime
import java.util.UUID
import mu.KLogging
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val projectTagRepository: ProjectTagRepository,
    private val storageService: StorageService,
    private val applicationProperties: ApplicationProperties
) : ProjectService {

    companion object : KLogging()

    @Transactional
    override fun createProject(user: UUID, organization: Organization, request: ProjectRequest): Project {
        validateCreateProjectRequest(request)
        logger.debug { "Creating project: ${request.name}" }
        val project = createProjectFromRequest(user, organization, request)
        project.createdAt = ZonedDateTime.now()
        return projectRepository.save(project)
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
    override fun getAllProjectsForOrganization(organizationId: UUID): List<Project> {
        return projectRepository.findAllByOrganizationUuid(organizationId)
    }

    @Transactional(readOnly = true)
    override fun getAllProjects(pageable: Pageable): Page<Project> {
        return projectRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    override fun getActiveProjects(pageable: Pageable): Page<Project> {
        return projectRepository.findByActive(true, pageable)
    }

    @Transactional
    override fun updateProject(project: Project, request: ProjectUpdateRequest): Project {
        validateRoi(request.roi)
        request.name?.let { project.name = it }
        request.description?.let { project.description = it }
        request.location?.let {
            project.location.lat = it.lat
            project.location.long = it.long
        }
        request.roi?.let {
            project.roi.from = it.from
            project.roi.to = it.to
        }
        request.active?.let { project.active = it }
        request.tags?.let { it -> project.tags = it.toSet().map { tag -> tag.toLowerCase() } }
        request.news?.let { project.newsLinks = it }
        return projectRepository.save(project)
    }

    @Transactional
    override fun addMainImage(project: Project, name: String, content: ByteArray) {
        val link = storageService.saveImage(name, content)
        project.mainImage = link
        projectRepository.save(project)
    }

    @Transactional
    override fun addImageToGallery(project: Project, name: String, content: ByteArray) {
        val gallery = project.gallery.orEmpty().toMutableList()
        val link = storageService.saveImage(name, content)
        gallery.add(link)
        setProjectGallery(project, gallery)
    }

    @Transactional
    override fun removeImagesFromGallery(project: Project, images: List<String>) {
        val gallery = project.gallery.orEmpty().toMutableList()
        images.forEach {
            if (gallery.remove(it)) {
                storageService.deleteImage(it)
            }
        }
        setProjectGallery(project, gallery)
    }

    @Transactional
    override fun addDocument(project: Project, request: DocumentSaveRequest): Document {
        val document = storageService.saveDocument(request)
        addDocumentToProject(project, document)
        return document
    }

    @Transactional
    override fun removeDocument(project: Project, documentId: Int) {
        val storedDocuments = project.documents.orEmpty().toMutableList()
        storedDocuments.firstOrNull { it.id == documentId }.let {
            storedDocuments.remove(it)
            project.documents = storedDocuments
            projectRepository.save(project)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllProjectTags(): List<String> {
        return projectTagRepository.getAllTags()
    }

    @Transactional(readOnly = true)
    override fun getProjectsByTags(tags: List<String>, pageable: Pageable): Page<Project> {
        return projectRepository.findByTags(tags, tags.size.toLong(), pageable)
    }

    @Suppress("ThrowsCount")
    private fun validateCreateProjectRequest(request: ProjectRequest) {
        if (request.endDate.isBefore(request.startDate)) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before start date")
        }
        if (request.endDate.isBefore(ZonedDateTime.now())) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before present date")
        }
        if (request.minPerUser > request.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MIN_ABOVE_MAX,
                    "Min: ${request.minPerUser} > Max: ${request.maxPerUser}")
        }
        if (applicationProperties.investment.maxPerProject <= request.expectedFunding) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH,
                    "Max expected funding is: ${applicationProperties.investment.maxPerProject}")
        }
        if (applicationProperties.investment.maxPerUser <= request.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH,
                    "Max funds per user is: ${applicationProperties.investment.maxPerUser}")
        }
        validateRoi(request.roi)
    }

    private fun validateRoi(roiRequest: ProjectRoiRequest?) {
        roiRequest?.let {
            if (it.from > it.to) {
                throw InvalidRequestException(ErrorCode.PRJ_ROI, "ROI from is bigger than ROI to")
            }
        }
    }

    private fun addDocumentToProject(project: Project, document: Document) {
        val documents = project.documents.orEmpty().toMutableList()
        documents += document
        project.documents = documents
        projectRepository.save(project)
    }

    private fun createProjectFromRequest(user: UUID, organization: Organization, request: ProjectRequest) =
        Project(
            UUID.randomUUID(),
            organization,
            request.name,
            request.description,
            ProjectLocation(request.location.lat, request.location.long),
            ProjectRoi(request.roi.from, request.roi.to),
            request.startDate,
            request.endDate,
            request.expectedFunding,
            request.currency,
            request.minPerUser,
            request.maxPerUser,
            null,
            null,
            null,
            user,
            ZonedDateTime.now(),
            request.active,
            null,
            request.tags?.toSet()?.map { it.toLowerCase() }
        )

    private fun setProjectGallery(project: Project, gallery: List<String>) {
        project.gallery = gallery
        projectRepository.save(project)
    }
}
