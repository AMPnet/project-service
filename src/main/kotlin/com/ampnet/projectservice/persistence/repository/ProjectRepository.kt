package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ProjectRepository : JpaRepository<Project, Int> {

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "WHERE project.id = ?1")
    fun findByIdWithOrganization(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.documents " +
            "WHERE project.id = ?1")
    fun findByIdWithAllData(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "WHERE organization.id = ?1")
    fun findAllByOrganizationId(organizationId: Int): List<Project>

    fun findByNameContainingIgnoreCase(name: String): List<Project>
}
