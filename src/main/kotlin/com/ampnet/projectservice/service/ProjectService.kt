package com.ampnet.projectservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.ProjectWithWallet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ProjectService {
    fun createProject(user: UserPrincipal, organization: Organization, request: ProjectRequest): Project
    fun updateProject(project: Project, request: ProjectUpdateRequest): Project

    fun getProjectByIdWithAllData(id: UUID): Project?
    fun getAllProjectsForOrganization(organizationId: UUID, coop: String?): List<Project>
    fun getAllProjects(pageable: Pageable): Page<Project>
    fun getActiveProjects(coop: String?, pageable: Pageable): Page<ProjectWithWallet>
    fun getProjectsByTags(tags: List<String>, coop: String?, pageable: Pageable, active: Boolean = true): Page<Project>
    fun getAllProjectTags(coop: String?): List<String>

    fun countActiveProjects(coop: String?): Int

    fun addMainImage(project: Project, name: String, content: ByteArray)
    fun addImageToGallery(project: Project, name: String, content: ByteArray)
    fun removeImagesFromGallery(project: Project, images: List<String>)
    fun addDocument(project: Project, request: DocumentSaveRequest): Document
    fun removeDocument(project: Project, documentId: Int)
}
