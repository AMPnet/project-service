package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.ProjectUpdate
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectUpdateRepository : JpaRepository<ProjectUpdate, Int> {
    fun findByProjectUuid(projectUuid: UUID, pageable: Pageable): List<ProjectUpdate>
}
