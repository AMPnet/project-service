package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.LinkRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectResponse
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.pojo.CreateProjectServiceRequest
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import javax.validation.Valid

@RestController
class ProjectController(
    private val projectService: ProjectService,
    private val organizationService: OrganizationService
) {

    companion object : KLogging()

    @GetMapping("/public/project/{id}")
    fun getProject(@PathVariable id: Int): ResponseEntity<ProjectResponse> {
        logger.debug { "Received request to get project with id: $id" }
        projectService.getProjectByIdWithAllData(id)?.let { project ->
            return ResponseEntity.ok(ProjectResponse(project))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/project")
    fun createProject(@RequestBody @Valid request: ProjectRequest): ResponseEntity<ProjectResponse> {
        logger.debug { "Received request to create project: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, request.organizationId) {
            createProject(request, userPrincipal.uuid)
        }
    }

    @PostMapping("/project/{projectId}")
    fun updateProject(
        @PathVariable("projectId") projectId: Int,
        @RequestBody @Valid request: ProjectUpdateRequest
    ): ResponseEntity<ProjectResponse> {
        logger.debug { "Received request to update project with id: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val updatedProject = projectService.updateProject(project, request)
            ProjectResponse(updatedProject)
        }
    }

    @GetMapping("/project")
    fun getAllProjects(): ResponseEntity<ProjectListResponse> {
        logger.debug { "Received request to get project all projects" }
        val projectsResponse = projectService.getAllProjects().map { ProjectResponse(it) }
        val response = ProjectListResponse(projectsResponse)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/project/organization/{organizationId}")
    fun getAllProjectsForOrganization(@PathVariable organizationId: Int): ResponseEntity<ProjectListResponse> {
        logger.debug { "Received request to get all projects for organization: $organizationId" }
        val projects = projectService.getAllProjectsForOrganization(organizationId).map { ProjectResponse(it) }
        return ResponseEntity.ok(ProjectListResponse(projects))
    }

    @PostMapping("/project/{projectId}/document")
    fun addDocument(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectByIdWithAllData(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val request = DocumentSaveRequest(file, userPrincipal.uuid)
            val document = projectService.addDocument(project, request)
            DocumentResponse(document)
        }
    }

    @DeleteMapping("/project/{projectId}/document/{documentId}")
    fun removeDocument(
        @PathVariable("projectId") projectId: Int,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for project $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectByIdWithAllData(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.removeDocument(project, documentId)
        }
    }

    @PostMapping("/project/{projectId}/image/main")
    fun addMainImage(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add main image to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val imageName = getImageNameFromMultipartFile(image)
            projectService.addMainImage(project, imageName, image.bytes)
        }
    }

    @PostMapping("/project/{projectId}/image/gallery")
    fun addGalleryImage(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add gallery image to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val imageName = getImageNameFromMultipartFile(image)
            projectService.addImageToGallery(project, imageName, image.bytes)
        }
    }

    @DeleteMapping("/project/{projectId}/image/gallery")
    fun removeImageFromGallery(
        @PathVariable("projectId") projectId: Int,
        @RequestBody request: ImageLinkListRequest
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete gallery images for project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.removeImagesFromGallery(project, request.images)
        }
    }

    @PostMapping("/project/{projectId}/news")
    fun addNews(
        @PathVariable("projectId") projectId: Int,
        @RequestBody request: LinkRequest
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add gallery image to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.addNews(project, request.link)
        }
    }

    @DeleteMapping("/project/{projectId}/news")
    fun removeNews(
        @PathVariable("projectId") projectId: Int,
        @RequestBody request: LinkRequest
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete gallery images for project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.removeNews(project, request.link)
        }
    }

    private fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
            multipartFile.originalFilename ?: multipartFile.name

    private fun createProject(request: ProjectRequest, userUuid: UUID): ProjectResponse {
        val organization = getOrganization(request.organizationId)
        val serviceRequest = CreateProjectServiceRequest(request, organization, userUuid)
        val project = projectService.createProject(serviceRequest)
        return ProjectResponse(project)
    }

    private fun getOrganization(organizationId: Int): Organization =
            organizationService.findOrganizationById(organizationId)
                    ?: throw ResourceNotFoundException(
                            ErrorCode.ORG_MISSING, "Missing organization with id: $organizationId")

    private fun getUserMembershipInOrganization(userUuid: UUID, organizationId: Int): OrganizationMembership? =
            organizationService.getOrganizationMemberships(organizationId).find { it.userUuid == userUuid }

    private fun getProjectById(projectId: Int): Project =
        projectService.getProjectById(projectId)
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectId")

    private fun getProjectByIdWithAllData(projectId: Int): Project =
            projectService.getProjectByIdWithAllData(projectId)
                    ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectId")

    private fun <T> ifUserHasPrivilegeToWriteInProjectThenReturn(
        userUuid: UUID,
        organizationId: Int,
        action: () -> (T)
    ): ResponseEntity<T> {
        getUserMembershipInOrganization(userUuid, organizationId)?.let { orgMembership ->
            return if (orgMembership.hasPrivilegeToWriteProject()) {
                val response = action()
                ResponseEntity.ok(response)
            } else {
                logger.info { "User does not have organization privilege to write users: PW_PROJECT" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User $userUuid is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
