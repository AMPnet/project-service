package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ImageLinkListRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectWithWalletFullResponse
import com.ampnet.projectservice.enums.DocumentPurpose
import com.ampnet.projectservice.service.ProjectService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.ProjectUpdateServiceRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @PostMapping("/project")
    fun createProject(@RequestBody @Valid request: ProjectRequest): ResponseEntity<ProjectWithWalletFullResponse> {
        logger.debug { "Received request to create project: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = projectService.createProject(userPrincipal, request)
        return ResponseEntity.ok(ProjectWithWalletFullResponse(project, null))
    }

    @PutMapping("/project/{projectUuid}", consumes = ["multipart/form-data"])
    fun updateProject(
        @PathVariable("projectUuid") projectUuid: UUID,
        @RequestPart("request", required = false) request: ProjectUpdateRequest?,
        @RequestParam("image", required = false) image: MultipartFile?,
        @RequestParam("documents", required = false) documents: List<MultipartFile>?,
        @RequestParam("termsOfService", required = false) termsOfService: MultipartFile?
    ): ResponseEntity<ProjectWithWalletFullResponse> {
        logger.debug { "Received request to update project with uuid: $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val documentSaveRequests = documents?.map { DocumentSaveRequest(it, userPrincipal.uuid) }
            .orEmpty().toMutableList()
        termsOfService?.let {
            documentSaveRequests.add(DocumentSaveRequest(it, userPrincipal.uuid, DocumentPurpose.TERMS))
        }
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
        val request = DocumentSaveRequest(file, userPrincipal.uuid)
        val document = projectService.addDocument(projectUuid, request)
        return ResponseEntity.ok(DocumentResponse(document))
    }

    @DeleteMapping("/project/{projectUuid}/document/{documentId}")
    fun removeDocument(
        @PathVariable("projectUuid") projectUuid: UUID,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for project $projectUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        projectService.removeDocument(projectUuid, userPrincipal.uuid, documentId)
        return ResponseEntity.ok().build()
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

    @GetMapping("/project/personal")
    fun getPersonalProjects(): ResponseEntity<ProjectListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get personal projects for user: ${userPrincipal.uuid}" }
        val projects = projectService.getPersonalProjects(userPrincipal.uuid)
        return ResponseEntity.ok(ProjectListResponse(projects))
    }
}
