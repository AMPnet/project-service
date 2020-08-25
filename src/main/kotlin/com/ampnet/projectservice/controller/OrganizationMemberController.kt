package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.service.OrganizationMemberService
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class OrganizationMemberController(
    private val organizationMemberService: OrganizationMemberService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/organization/{uuid}/members")
    fun getOrganizationMembers(
        @PathVariable("uuid") uuid: UUID
    ): ResponseEntity<OrganizationMembershipsResponse> {
        logger.debug { "Received request to get members for organization: $uuid" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, uuid) {
            val members = organizationMemberService.getOrganizationMemberships(uuid)
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
            organizationMemberService.removeUserFromOrganization(memberUuid, organizationUuid)
        }
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
