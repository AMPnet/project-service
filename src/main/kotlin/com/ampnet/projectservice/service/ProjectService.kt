package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ProjectService {
    fun createProject(user: UUID, organization: Organization, request: ProjectRequest): Project
    fun updateProject(project: Project, request: ProjectUpdateRequest): Project

    fun getProjectByIdWithAllData(id: UUID): Project?
    fun getAllProjectsForOrganization(organizationId: UUID): List<Project>
    fun getAllProjects(pageable: Pageable): Page<Project>
    fun getActiveProjects(pageable: Pageable): Page<Project>
    fun getProjectsByTags(tags: List<String>, pageable: Pageable): Page<Project>
    fun getAllProjectTags(): List<String>

    fun countActiveProjects(): Int

    fun addMainImage(project: Project, name: String, content: ByteArray)
    fun addImageToGallery(project: Project, name: String, content: ByteArray)
    fun removeImagesFromGallery(project: Project, images: List<String>)
    fun addDocument(project: Project, request: DocumentSaveRequest): Document
    fun removeDocument(project: Project, documentId: Int)
}
