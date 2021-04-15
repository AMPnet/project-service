package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipInfoResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationMembershipsInfoResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.pojo.OrganizationFullServiceResponse
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PublicOrganizationController(
    private val organizationService: OrganizationService,
    private val organizationMembershipService: OrganizationMembershipService,
    private val userService: UserService,
    private val imageProxyService: ImageProxyService
) {

    companion object : KLogging()

    @GetMapping("/public/organization")
    fun getOrganizations(pageable: Pageable): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for all active organizations" }
        val organizations = organizationService.getAllByActive(pageable)
            .map { OrganizationResponse(it, imageProxyService.generateImageResponse(it.headerImage)) }
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(OrganizationListResponse(organizations.toList(), organizations.number, organizations.totalPages))
    }

    @GetMapping("/public/organization/{uuid}")
    fun getOrganization(@PathVariable("uuid") uuid: UUID): ResponseEntity<OrganizationFullServiceResponse> {
        logger.debug { "Received request for organization with uuid: $uuid" }
        organizationService.findOrganizationWithProjectCountById(uuid)?.let {
            return ResponseEntity.ok()
                .cacheControl(ControllerUtils.cacheControl)
                .body(it)
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/organization/{uuid}/members")
    fun getOrganizationMembers(
        @PathVariable("uuid") uuid: UUID
    ): ResponseEntity<OrganizationMembershipsInfoResponse> {
        logger.debug { "Received request to get members for organization: $uuid" }
        val members = organizationMembershipService.getOrganizationMemberships(uuid)
        val users = userService.getUsers(members.map { it.userUuid })
        val membersResponse = members.map {
            OrganizationMembershipInfoResponse(it, users.firstOrNull { user -> user.uuid == it.userUuid.toString() })
        }
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(OrganizationMembershipsInfoResponse(membersResponse))
    }
}
