package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import com.ampnet.projectservice.controller.pojo.request.OrganizationUpdateRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationWithProjectCountListResponse
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import com.ampnet.projectservice.service.pojo.OrganizationUpdateServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import javax.validation.Valid

@RestController
class OrganizationController(
    private val organizationMembershipService: OrganizationMembershipService,
    private val organizationService: OrganizationService
) {

    companion object : KLogging()

    @GetMapping("/organization/personal")
    fun getPersonalOrganizations(): ResponseEntity<OrganizationWithProjectCountListResponse> {
        logger.debug { "Received request for personal organizations" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val organizations =
            organizationService.findAllOrganizationsForUser(userPrincipal.uuid)
        return ResponseEntity.ok(OrganizationWithProjectCountListResponse(organizations))
    }

    @PostMapping("/organization", consumes = ["multipart/form-data"])
    fun createOrganization(
        @RequestPart @Valid request: OrganizationRequest,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<OrganizationWithDocumentResponse> {
        logger.debug { "Received request to create organization: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val serviceRequest = OrganizationServiceRequest(request, userPrincipal, image)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @PostMapping("/organization/{organizationUuid}/document")
    fun addDocument(
        @PathVariable("organizationUuid") organizationUuid: UUID,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document: ${file.name} to organization: $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            val documentSaveRequest = DocumentSaveRequest(file, userPrincipal.uuid)
            val document = organizationService.addDocument(organizationUuid, documentSaveRequest)
            DocumentResponse(document)
        }
    }

    @DeleteMapping("/organization/{organizationId}/document/{documentId}")
    fun removeDocument(
        @PathVariable("organizationId") organizationUuid: UUID,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for organization $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            organizationService.removeDocument(organizationUuid, documentId)
        }
    }

    @PostMapping(value = ["/organization/{organizationId}/updates"], consumes = ["multipart/form-data"])
    fun updateOrganization(
        @PathVariable("organizationId") organizationUuid: UUID,
        @RequestPart request: OrganizationUpdateRequest,
        @RequestParam("image") image: MultipartFile?
    ): ResponseEntity<OrganizationResponse> {
        logger.debug { "Received request to update organization with uuid: $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            val organization = organizationService.updateOrganization(
                OrganizationUpdateServiceRequest(organizationUuid, image, request)
            )
            OrganizationResponse(organization)
        }
    }

    private fun <T> ifUserHasPrivilegeToWriteOrganizationThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationMembershipService.getOrganizationMemberships(organizationUuid)
            .find { it.userUuid == userUuid }
            ?.let { orgMembership ->
                return if (orgMembership.hasPrivilegeToWriteOrganization()) {
                    val response = action()
                    ResponseEntity.ok(response)
                } else {
                    logger.info { "User does not have organization privilege: PW_ORG" }
                    ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                }
            }
        logger.info { "User $userUuid is not a member of organization $organizationUuid" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
