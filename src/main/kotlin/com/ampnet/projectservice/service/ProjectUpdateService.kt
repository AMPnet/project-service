package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.ProjectUpdate
import com.ampnet.projectservice.service.pojo.CreateProjectUpdate
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ProjectUpdateService {
    fun getProjectUpdates(project: UUID, pageable: Pageable): List<ProjectUpdate>
    fun createProjectUpdate(request: CreateProjectUpdate): ProjectUpdate
    fun deleteProjectUpdate(user: UUID, projectUuid: UUID, id: Int)
}
