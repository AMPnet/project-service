package com.ampnet.projectservice.controller

import com.ampnet.projectservice.controller.pojo.request.ProjectUpdatesRequest
import com.ampnet.projectservice.controller.pojo.response.ProjectUpdateResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectUpdatesResponse
import com.ampnet.projectservice.service.ProjectUpdateService
import com.ampnet.projectservice.service.pojo.CreateProjectUpdate
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class ProjectUpdateController(private val projectUpdateService: ProjectUpdateService) {

    companion object : KLogging()

    @GetMapping("/public/project/{uuid}/updates")
    fun getProjectUpdates(@PathVariable uuid: UUID, pageable: Pageable): ResponseEntity<ProjectUpdatesResponse> {
        val updates = projectUpdateService.getProjectUpdates(uuid, pageable)
            .map { ProjectUpdateResponse(it) }
        return ResponseEntity.ok(ProjectUpdatesResponse(updates))
    }

    @PostMapping("/project/{uuid}/updates")
    fun createProjectUpdate(
        @PathVariable uuid: UUID,
        @RequestBody @Valid request: ProjectUpdatesRequest
    ): ResponseEntity<ProjectUpdateResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create Project Update: $request by user: ${userPrincipal.uuid}" }
        val serviceRequest = CreateProjectUpdate(
            userPrincipal.uuid, userPrincipal.name, uuid, request.title, request.content
        )
        val projectUpdate = projectUpdateService.createProjectUpdate(serviceRequest)
        return ResponseEntity.ok(ProjectUpdateResponse(projectUpdate))
    }

    @DeleteMapping("/project/{uuid}/updates/{id}")
    fun deleteProjectUpdate(@PathVariable uuid: UUID, @PathVariable id: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to delete Project Update: $id by user: ${userPrincipal.uuid}" }
        projectUpdateService.deleteProjectUpdate(userPrincipal.uuid, uuid, id)
        return ResponseEntity.ok().build()
    }
}
