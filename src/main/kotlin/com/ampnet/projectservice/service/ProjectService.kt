package com.ampnet.projectservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.FullProjectWithWallet
import com.ampnet.projectservice.service.pojo.ProjectServiceResponse
import com.ampnet.projectservice.service.pojo.ProjectUpdateServiceRequest
import com.ampnet.projectservice.service.pojo.ProjectWithWallet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface ProjectService {

    fun createProject(
        user: UserPrincipal,
        organization: Organization,
        request: ProjectRequest
    ): Project

    fun updateProject(serviceRequest: ProjectUpdateServiceRequest): FullProjectWithWallet

    fun getProjectByIdWithAllData(id: UUID): Project?
    fun getAllProjectsForOrganization(organizationId: UUID, coop: String?): List<ProjectWithWallet>
    fun getAllProjects(coop: String?, pageable: Pageable): Page<ProjectServiceResponse>

    fun getActiveProjects(coop: String?, pageable: Pageable): Page<ProjectWithWallet>
    fun getProjectsByTags(
        tags: List<String>,
        coop: String?,
        pageable: Pageable,
        active: Boolean = true
    ): Page<ProjectServiceResponse>

    fun getAllProjectTags(coop: String?): List<String>
    fun getProjectWithWallet(id: UUID): FullProjectWithWallet?

    fun countActiveProjects(coop: String?): Int

    fun addMainImage(projectUuid: UUID, userUuid: UUID, image: MultipartFile)
    fun addImageToGallery(projectUuid: UUID, userUuid: UUID, image: MultipartFile)
    fun removeImagesFromGallery(project: Project, images: List<String>)
    fun addDocument(project: Project, request: DocumentSaveRequest): Document
    fun removeDocument(project: Project, documentId: Int)
}
