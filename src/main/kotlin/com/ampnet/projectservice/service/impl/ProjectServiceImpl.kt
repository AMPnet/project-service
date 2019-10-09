package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.pojo.CreateProjectServiceRequest
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val storageService: StorageService,
    private val applicationProperties: ApplicationProperties
) : ProjectService {

    companion object : KLogging()

    @Transactional
    override fun createProject(request: CreateProjectServiceRequest): Project {
        validateCreateProjectRequest(request)
        // if (request.organization.wallet == null) {
        //     throw InvalidRequestException(ErrorCode.WALLET_MISSING,
        //             "Trying to create project without organization wallet. " +
        //                     "Organization: ${request.organization.id}")
        // }

        logger.debug { "Creating project: ${request.name}" }
        val project = createProjectFromRequest(request)
        project.createdAt = ZonedDateTime.now()
        return projectRepository.save(project)
    }

    @Transactional(readOnly = true)
    override fun getProjectById(id: UUID): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithOrganization(id))
    }

    @Transactional(readOnly = true)
    override fun getProjectByIdWithAllData(id: UUID): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithAllData(id))
    }

    @Transactional(readOnly = true)
    override fun getAllProjectsForOrganization(organizationId: UUID): List<Project> {
        return projectRepository.findAllByOrganizationUuid(organizationId)
    }

    @Transactional(readOnly = true)
    override fun getAllProjects(): List<Project> {
        return projectRepository.findAll()
    }

    @Transactional
    override fun updateProject(project: Project, request: ProjectUpdateRequest): Project {
        request.name?.let { project.name = it }
        request.description?.let { project.description = it }
        request.location?.let { project.location = it }
        request.locationText?.let { project.locationText = it }
        request.returnOnInvestment?.let { project.returnOnInvestment = it }
        request.active?.let { project.active = it }
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

    @Transactional
    override fun addNews(project: Project, link: String) {
        val news = project.newsLinks.orEmpty().toMutableList()
        news.add(link)
        project.newsLinks = news
        projectRepository.save(project)
    }

    @Transactional
    override fun removeNews(project: Project, link: String) {
        val news = project.newsLinks.orEmpty().toMutableList()
        news.remove(link)
        project.newsLinks = news
        projectRepository.save(project)
    }

    @Suppress("ThrowsCount")
    private fun validateCreateProjectRequest(request: CreateProjectServiceRequest) {
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
    }

    private fun addDocumentToProject(project: Project, document: Document) {
        val documents = project.documents.orEmpty().toMutableList()
        documents += document
        project.documents = documents
        projectRepository.save(project)
    }

    private fun createProjectFromRequest(request: CreateProjectServiceRequest) =
        Project(
            UUID.randomUUID(),
            request.organization,
            request.name,
            request.description,
            request.location,
            request.locationText,
            request.returnOnInvestment,
            request.startDate,
            request.endDate,
            request.expectedFunding,
            request.currency,
            request.minPerUser,
            request.maxPerUser,
            null,
            null,
            null,
            request.createdByUserUuid,
            ZonedDateTime.now(),
            request.active,
            null
        )

    private fun setProjectGallery(project: Project, gallery: List<String>) {
        project.gallery = gallery
        projectRepository.save(project)
    }
}
