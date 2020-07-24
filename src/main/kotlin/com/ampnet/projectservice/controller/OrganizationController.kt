package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import com.ampnet.projectservice.controller.pojo.response.DocumentResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import com.ampnet.projectservice.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
class OrganizationController(
    private val organizationService: OrganizationService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.projectservice.enums.PrivilegeType).PRA_ORG)")
    fun getOrganizations(pageable: Pageable): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for all organizations" }
        val organizations = organizationService.getAllOrganizations(pageable).map { OrganizationResponse(it) }
        return ResponseEntity.ok(
            OrganizationListResponse(organizations.toList(), organizations.number, organizations.totalPages)
        )
    }

    @GetMapping("/organization/personal")
    fun getPersonalOrganizations(): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for personal organizations" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val organizations = organizationService
            .findAllOrganizationsForUser(userPrincipal.uuid)
            .map { OrganizationResponse(it) }
        return ResponseEntity.ok(OrganizationListResponse(organizations))
    }

    @GetMapping("/organization/{uuid}")
    fun getOrganization(@PathVariable("uuid") uuid: UUID): ResponseEntity<OrganizationWithDocumentResponse> {
        logger.debug { "Received request for organization with uuid: $uuid" }
        organizationService.findOrganizationById(uuid)?.let {
            return ResponseEntity.ok(OrganizationWithDocumentResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/organization")
    fun createOrganization(
        @RequestBody @Valid request: OrganizationRequest
    ): ResponseEntity<OrganizationWithDocumentResponse> {
        logger.debug { "Received request to create organization: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        val serviceRequest = OrganizationServiceRequest(request, userPrincipal.uuid)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @GetMapping("/organization/{uuid}/members")
    fun getOrganizationMembers(
        @PathVariable("uuid") uuid: UUID
    ): ResponseEntity<OrganizationMembershipsResponse> {
        logger.debug { "Received request to get members for organization: $uuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, uuid) {
            val members = organizationService.getOrganizationMemberships(uuid)
            val membersWithoutMe = members.filter { userPrincipal.uuid != it.userUuid }
            val users = userService.getUsers(membersWithoutMe.map { it.userUuid })

            val membersResponse = membersWithoutMe.map {
                OrganizationMembershipResponse(it, users.firstOrNull { user -> user.uuid == it.userUuid.toString() })
            }
            OrganizationMembershipsResponse(membersResponse)
        }
    }

    @DeleteMapping("/organization/{organizationUuid}/members/{memberUuid}")
    fun deleteOrganizationMember(
        @PathVariable("organizationUuid") organizationUuid: UUID,
        @PathVariable("memberUuid") memberUuid: UUID
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to remove member: $memberUuid from organization: $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            organizationService.removeUserFromOrganization(memberUuid, organizationUuid)
        }
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

    private fun <T> ifUserHasPrivilegeWriteUserInOrganizationThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationService.getOrganizationMemberships(organizationUuid)
            .find { it.userUuid == userUuid }
            ?.let { orgMembership ->
                return if (orgMembership.hasPrivilegeToWriteOrganizationUsers()) {
                    val response = action()
                    ResponseEntity.ok(response)
                } else {
                    logger.info { "User does not have organization privilege to write users: PW_USERS" }
                    ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                }
            }
        logger.info { "User $userUuid is not a member of organization $organizationUuid" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    private fun <T> ifUserHasPrivilegeToWriteOrganizationThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationService.getOrganizationMemberships(organizationUuid)
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
