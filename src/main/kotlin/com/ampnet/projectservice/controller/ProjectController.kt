package com.ampnet.projectservice.controller

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectWithWalletFullResponse
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.ProjectUpdateServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import javax.validation.Valid

@RestController
class ProjectController(
    private val projectService: ProjectService,
    private val organizationService: OrganizationService,
    private val organizationMembershipService: OrganizationMembershipService
) {

    companion object : KLogging()

    @PostMapping("/project")
    fun createProject(@RequestBody @Valid request: ProjectRequest): ResponseEntity<ProjectWithWalletFullResponse> {
        logger.debug { "Received request to create project: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, request.organizationUuid) {
            createProject(request, userPrincipal)
        }
    }

    @PutMapping("/project/{projectUuid}", consumes = ["multipart/form-data"])
    fun updateProject(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestPart("request", required = false) request: ProjectUpdateRequest?,
        @RequestParam("image", required = false) image: MultipartFile?,
        @RequestParam("documents", required = false) documents: List<MultipartFile>?
    ): ResponseEntity<ProjectWithWalletFullResponse> {
        logger.debug { "Received request to update project with uuid: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val documentSaveRequests = documents?.map { DocumentSaveRequest(it, userPrincipal.uuid) }
        val serviceRequest = ProjectUpdateServiceRequest(
            projectUuid, userPrincipal.uuid, request, image, documentSaveRequests
        )
        val updatedProject = projectService.updateProject(serviceRequest)
        return ResponseEntity.ok(ProjectWithWalletFullResponse(updatedProject.project, updatedProject.walletResponse))
    }

    @PostMapping("/project/{projectUuid}/document")
    fun addDocument(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document to project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectByIdWithAllData(projectUuid)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.uuid) {
            val request = DocumentSaveRequest(file, userPrincipal.uuid)
            val document = projectService.addDocument(project, request)
            DocumentResponse(document)
        }
    }

    @DeleteMapping("/project/{projectUuid}/document/{documentId}")
    fun removeDocument(
        @PathVariable("projectUuid") projectUuid: UUID,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for project $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectByIdWithAllData(projectUuid)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.uuid) {
            projectService.removeDocument(project, documentId)
        }
    }

    @PostMapping("/project/{projectUuid}/image/main")
    fun addMainImage(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add main image to project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        projectService.addMainImage(projectUuid, userPrincipal.uuid, image)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/project/{projectUuid}/image/gallery")
    fun addGalleryImage(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add gallery image to project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        projectService.addImageToGallery(projectUuid, userPrincipal.uuid, image)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/project/{projectUuid}/image/gallery")
    fun removeImageFromGallery(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestBody request: ImageLinkListRequest
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete gallery images for project: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        projectService.removeImagesFromGallery(projectUuid, userPrincipal.uuid, request.images)
        return ResponseEntity.ok().build()
    }

    private fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
        multipartFile.originalFilename ?: multipartFile.name

    private fun createProject(request: ProjectRequest, user: UserPrincipal): ProjectWithWalletFullResponse {
        val organization = getOrganization(request.organizationUuid)
        val project = projectService.createProject(user, organization, request)
        return ProjectWithWalletFullResponse(project, null)
    }

    private fun getOrganization(organizationUuid: UUID): Organization =
        organizationService.findOrganizationById(organizationUuid)
            ?: throw ResourceNotFoundException(
                ErrorCode.ORG_MISSING, "Missing organization with id: $organizationUuid"
            )

    private fun getUserMembershipInOrganization(userUuid: UUID, organizationUuid: UUID): OrganizationMembership? =
        organizationMembershipService.getOrganizationMemberships(organizationUuid).find { it.userUuid == userUuid }

    private fun getProjectByIdWithAllData(projectUuid: UUID): Project =
        projectService.getProjectByIdWithAllData(projectUuid)
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectUuid")

    private fun <T> ifUserHasPrivilegeToWriteInProjectThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        getUserMembershipInOrganization(userUuid, organizationUuid)?.let { orgMembership ->
            return if (orgMembership.hasPrivilegeToWriteProject()) {
                val response = action()
                ResponseEntity.ok(response)
            } else {
                logger.info { "User does not have organization privilege to write users: PW_PROJECT" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User $userUuid is not a member of organization $organizationUuid" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
