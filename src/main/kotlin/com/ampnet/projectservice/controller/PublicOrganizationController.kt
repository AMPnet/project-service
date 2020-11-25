package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationListResponse
import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
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
class PublicOrganizationController(private val organizationService: OrganizationService) {

    companion object : KLogging()

    @GetMapping("/public/organization")
    fun getOrganizations(pageable: Pageable): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for all organizations" }
        val organizations = organizationService.getAllOrganizations(pageable).map { OrganizationResponse(it) }
        return ResponseEntity.ok(
            OrganizationListResponse(organizations.toList(), organizations.number, organizations.totalPages)
        )
    }

    @GetMapping("/public/organization/{uuid}")
    fun getOrganization(@PathVariable("uuid") uuid: UUID): ResponseEntity<OrganizationFullServiceResponse> {
        logger.debug { "Received request for organization with uuid: $uuid" }
        organizationService.findOrganizationWithProjectCountById(uuid)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }
}
