package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.ProjectFullResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectResponse
import com.ampnet.projectservice.controller.pojo.response.TagsResponse
import com.ampnet.projectservice.service.ProjectService
import java.util.UUID
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicProjectController(private val projectService: ProjectService) {

    companion object : KLogging()

    @GetMapping("/public/project/{uuid}")
    fun getProject(@PathVariable uuid: UUID): ResponseEntity<ProjectFullResponse> {
        ProjectController.logger.debug { "Received request to get project with uuid: $uuid" }
        projectService.getProjectByIdWithAllData(uuid)?.let { project ->
            return ResponseEntity.ok(ProjectFullResponse(project))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/project")
    fun getAllProjects(
        @RequestParam(name = "tags") tags: List<String>?,
        pageable: Pageable
    ): ResponseEntity<ProjectListResponse> {
        val projects = if (tags?.isEmpty() != false) {
            ProjectController.logger.debug { "Received request to get project all projects" }
            projectService.getAllProjects(pageable)
        } else {
            ProjectController.logger.debug { "Received request to get project all projects by tags: $tags" }
            projectService.getProjectsByTags(tags, pageable)
        }
        val projectsResponse = projects.map { ProjectResponse(it) }
        val response = ProjectListResponse(
            projectsResponse.toList(),
            projectsResponse.number,
            projectsResponse.totalPages
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/public/project/tags")
    fun getAllProjectTags(): ResponseEntity<TagsResponse> {
        ProjectController.logger.debug { "Received request to get all project tags" }
        val tags = projectService.getAllProjectTags()
        return ResponseEntity.ok(TagsResponse(tags))
    }

    @GetMapping("/public/project/organization/{organizationUuid}")
    fun getAllProjectsForOrganization(@PathVariable organizationUuid: UUID): ResponseEntity<ProjectListResponse> {
        ProjectController.logger.debug { "Received request to get all projects for organization: $organizationUuid" }
        val projects = projectService
            .getAllProjectsForOrganization(organizationUuid)
            .map { ProjectResponse(it) }
        return ResponseEntity.ok(ProjectListResponse(projects))
    }
}
