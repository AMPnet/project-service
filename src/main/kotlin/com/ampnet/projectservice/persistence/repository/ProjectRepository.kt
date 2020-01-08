package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Project
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ProjectRepository : JpaRepository<Project, UUID> {

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization WHERE project.uuid = ?1")
    fun findByIdWithOrganization(id: UUID): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.documents " +
            "WHERE project.uuid = ?1")
    fun findByIdWithAllData(id: UUID): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "WHERE organization.uuid = ?1")
    fun findAllByOrganizationUuid(organizationUuid: UUID): List<Project>

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Project>
}
