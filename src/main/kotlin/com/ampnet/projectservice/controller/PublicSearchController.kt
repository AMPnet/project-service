package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.OrganizationResponse
import com.ampnet.projectservice.controller.pojo.response.SearchOrgAndProjectResponse
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.SearchService
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicSearchController(
    private val searchService: SearchService,
    private val imageProxyService: ImageProxyService
) {

    companion object : KLogging()

    @GetMapping("/public/search")
    fun findOrganizationsAndProjects(
        @RequestParam(name = "name") name: String,
        @RequestParam(name = "coop", required = false) coop: String?,
        pageable: Pageable
    ): ResponseEntity<SearchOrgAndProjectResponse> {
        logger.debug { "Searching for organization and projects with name: $name for cooperative with id: $coop" }
        val organizations = searchService.searchOrganizations(name, coop, pageable)
        logger.debug { "Found organizations = ${organizations.map { it.name }}" }
        val projects = searchService.searchProjects(name, coop, pageable)
        logger.debug { "Found projects = ${projects.map { it.name }}" }
        val organizationListResponse = organizations
            .map { OrganizationResponse(it, imageProxyService.generateImageResponse(it.headerImage)) }
            .toList()
        val searchResponse = SearchOrgAndProjectResponse(organizationListResponse, projects.toList())
        return ResponseEntity.ok(searchResponse)
    }
}
