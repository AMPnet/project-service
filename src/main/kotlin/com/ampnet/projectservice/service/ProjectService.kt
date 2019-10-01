package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.pojo.CreateProjectServiceRequest
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import java.util.UUID

interface ProjectService {
    fun createProject(request: CreateProjectServiceRequest): Project
    fun getProjectById(id: UUID): Project?
    fun getProjectByIdWithAllData(id: UUID): Project?
    fun getAllProjectsForOrganization(organizationId: UUID): List<Project>
    fun getAllProjects(): List<Project>
    fun updateProject(project: Project, request: ProjectUpdateRequest): Project

    fun addMainImage(project: Project, name: String, content: ByteArray)
    fun addImageToGallery(project: Project, name: String, content: ByteArray)
    fun removeImagesFromGallery(project: Project, images: List<String>)
    fun addDocument(project: Project, request: DocumentSaveRequest): Document
    fun removeDocument(project: Project, documentId: Int)
    fun addNews(project: Project, link: String)
    fun removeNews(project: Project, link: String)
}
