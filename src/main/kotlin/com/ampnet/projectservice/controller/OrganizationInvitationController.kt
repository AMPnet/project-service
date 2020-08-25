package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.projectservice.controller.pojo.response.OrganizationInviteResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.projectservice.controller.pojo.response.PendingInvitationResponse
import com.ampnet.projectservice.controller.pojo.response.PendingInvitationsListResponse
import com.ampnet.projectservice.service.OrganizationInviteService
import com.ampnet.projectservice.service.OrganizationMemberService
import com.ampnet.projectservice.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class OrganizationInvitationController(
    private val organizationInviteService: OrganizationInviteService,
    private val organizationMemberService: OrganizationMemberService
) {

    companion object : KLogging()

    @GetMapping("/invites/me")
    fun getMyInvitations(): ResponseEntity<OrganizationInvitesListResponse> {
        logger.debug { "Received request to list my invites" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val invites = organizationInviteService.getAllInvitationsForUser(userPrincipal.email)
            .map { OrganizationInviteResponse(it) }
        return ResponseEntity.ok(OrganizationInvitesListResponse(invites))
    }

    @PostMapping("/invites/me/{organizationUuid}/accept")
    fun acceptOrganizationInvitation(@PathVariable("organizationUuid") organizationUuid: UUID): ResponseEntity<Unit> {
        logger.debug { "Received request accept organization invite for organization: $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val request = OrganizationInviteAnswerRequest(userPrincipal.uuid, userPrincipal.email, true, organizationUuid)
        organizationInviteService.answerToInvitation(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/invites/me/{organizationUuid}/reject")
    fun rejectOrganizationInvitation(@PathVariable("organizationUuid") organizationUuid: UUID): ResponseEntity<Unit> {
        logger.debug { "Received request reject organization invite for organization: $organizationUuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val request = OrganizationInviteAnswerRequest(userPrincipal.uuid, userPrincipal.email, false, organizationUuid)
        organizationInviteService.answerToInvitation(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/invites/organization/{uuid}/invite")
    fun inviteToOrganization(
        @PathVariable("uuid") uuid: UUID,
        @RequestBody @Valid request: OrganizationInviteRequest
    ): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to invited user to organization $uuid by user: ${userPrincipal.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, uuid) {
            val serviceRequest = OrganizationInviteServiceRequest(request, uuid, userPrincipal.uuid)
            organizationInviteService.sendInvitation(serviceRequest)
            Unit
        }
    }

    @PostMapping("/invites/organization/{organizationUuid}/{revokeEmail}/revoke")
    fun revokeInvitationToOrganization(
        @PathVariable("organizationUuid") organizationUuid: UUID,
        @PathVariable("revokeEmail") revokeEmail: String
    ): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to invited user to organization $organizationUuid by user: ${userPrincipal.email}"
        }
        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            organizationInviteService.revokeInvitation(organizationUuid, revokeEmail)
        }
    }

    @GetMapping("/invites/organization/{organizationUuid}")
    fun getPendingInvitations(
        @PathVariable("organizationUuid") organizationUuid: UUID
    ): ResponseEntity<PendingInvitationsListResponse> {
        val invites = organizationInviteService.getPendingInvitations(organizationUuid)
            .map { PendingInvitationResponse(it) }
        return ResponseEntity.ok(PendingInvitationsListResponse(invites))
    }

    private fun <T> ifUserHasPrivilegeWriteUserInOrganizationThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationMemberService.getOrganizationMemberships(organizationUuid)
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
}
