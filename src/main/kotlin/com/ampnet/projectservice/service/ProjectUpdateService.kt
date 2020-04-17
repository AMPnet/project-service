package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.ProjectUpdate
import com.ampnet.projectservice.service.pojo.CreateProjectUpdate
import java.util.UUID
import org.springframework.data.domain.Pageable

interface ProjectUpdateService {
    fun getProjectUpdates(project: UUID, pageable: Pageable): List<ProjectUpdate>
    fun createProjectUpdate(request: CreateProjectUpdate): ProjectUpdate
    fun deleteProjectUpdate(user: UUID, projectUuid: UUID, id: Int)
}
