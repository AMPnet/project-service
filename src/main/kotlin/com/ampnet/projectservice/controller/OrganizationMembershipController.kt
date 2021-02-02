package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.UpdateOrganizationRoleRequest
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.pojo.OrganizationMemberServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class OrganizationMembershipController(
    private val organizationMembershipService: OrganizationMembershipService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/organization/{uuid}/members")
    fun getOrganizationMembers(
        @PathVariable("uuid") uuid: UUID
    ): ResponseEntity<OrganizationMembershipsResponse> {
        logger.debug { "Received request to get members for organization: $uuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToSeeUserInOrganizationThenReturn(userPrincipal.uuid, uuid) {
            val members = organizationMembershipService.getOrganizationMemberships(uuid)
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

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            organizationMembershipService.removeUserFromOrganization(memberUuid, organizationUuid)
        }
    }

    @PostMapping("organization/{organizationUuid}/members")
    fun changeOrganizationRole(
        @PathVariable("organizationUuid") organizationUuid: UUID,
        @RequestBody request: UpdateOrganizationRoleRequest
    ): ResponseEntity<Unit> {
        logger.debug {
            "Received request to change role for member: ${request.memberUuid} " +
                "from organization: $organizationUuid"
        }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationUuid) {
            organizationMembershipService.updateOrganizationRole(
                OrganizationMemberServiceRequest(organizationUuid, request)
            )
        }
    }

    private fun <T> ifUserHasPrivilegeToSeeUserInOrganizationThenReturn(
        userUuid: UUID,
        organizationUuid: UUID,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationMembershipService.getOrganizationMemberships(organizationUuid)
            .find { it.userUuid == userUuid }
            ?.let { orgMembership ->
                return if (orgMembership.hasPrivilegeToSeeOrganizationUsers()) {
                    val response = action()
                    ResponseEntity.ok(response)
                } else {
                    logger.info { "User does not have organization privilege to see users: PR_USERS" }
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
