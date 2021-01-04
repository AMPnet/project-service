package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.response.CountActiveProjectsCount
import com.ampnet.projectservice.controller.pojo.response.ProjectListResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectWithWalletFullResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectsWalletsListResponse
import com.ampnet.projectservice.controller.pojo.response.TagsResponse
import com.ampnet.projectservice.service.ProjectService
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PublicProjectController(private val projectService: ProjectService) {

    companion object : KLogging()

    @GetMapping("/public/project/{uuid}")
    fun getProject(@PathVariable uuid: UUID): ResponseEntity<ProjectWithWalletFullResponse> {
        logger.debug { "Received request to get project with wallet with uuid: $uuid" }
        projectService.getProjectWithWallet(uuid)?.let { projectWithWallet ->
            return ResponseEntity.ok()
                .cacheControl(ControllerUtils.cacheControl)
                .body(ProjectWithWalletFullResponse(projectWithWallet.project, projectWithWallet.walletResponse))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/project")
    fun getAllProjects(
        @RequestParam(name = "tags") tags: List<String>?,
        @RequestParam(name = "coop", required = false) coop: String?,
        pageable: Pageable
    ): ResponseEntity<ProjectListResponse> {
        val projects = if (tags?.isEmpty() != false) {
            logger.debug {
                "Received request to get project all projects " +
                    "for cooperative with id: $coop"
            }
            projectService.getAllProjects(coop, pageable)
        } else {
            logger.debug {
                "Received request to get project all projects by tags: $tags " +
                    "for cooperative with id: $coop"
            }
            projectService.getProjectsByTags(tags, coop, pageable)
        }
        val response = ProjectListResponse(
            projects.toList(), projects.number, projects.totalPages
        )
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(response)
    }

    @GetMapping("/public/project/active")
    fun getAllActiveProjects(
        @RequestParam(name = "coop", required = false) coop: String?,
        pageable: Pageable
    ): ResponseEntity<ProjectsWalletsListResponse> {
        logger.debug { "Received request to get all active projects for cooperative with id: $coop" }
        val projectsWithWalletAndOrg = projectService.getActiveProjects(coop, pageable)
        val response = ProjectsWalletsListResponse(
            projectsWithWalletAndOrg.toList(),
            projectsWithWalletAndOrg.number,
            projectsWithWalletAndOrg.totalPages
        )
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(response)
    }

    @GetMapping("/public/project/active/count")
    fun countAllActiveProjects(
        @RequestParam(name = "coop", required = false) coop: String?
    ): ResponseEntity<CountActiveProjectsCount> {
        logger.debug { "Received request to get all active projects count for cooperative with id: $coop" }
        val count = projectService.countActiveProjects(coop)
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(CountActiveProjectsCount(count))
    }

    @GetMapping("/public/project/tags")
    fun getAllProjectTags(
        @RequestParam(name = "coop", required = false) coop: String?
    ): ResponseEntity<TagsResponse> {
        logger.debug { "Received request to get all project tags for cooperative with id: $coop" }
        val tags = projectService.getAllProjectTags(coop)
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(TagsResponse(tags))
    }

    @GetMapping("/public/project/organization/{organizationUuid}")
    fun getAllProjectsForOrganization(
        @PathVariable organizationUuid: UUID,
        @RequestParam(name = "coop", required = false) coop: String?
    ): ResponseEntity<ProjectsWalletsListResponse> {
        logger.debug {
            "Received request to get all projects for organization: $organizationUuid and cooperative with id: $coop"
        }
        val projects = projectService.getAllProjectsForOrganization(organizationUuid, coop)
        return ResponseEntity.ok()
            .cacheControl(ControllerUtils.cacheControl)
            .body(ProjectsWalletsListResponse(projects))
    }
}
