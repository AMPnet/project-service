package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.config.PROJECT_CACHE
import com.ampnet.projectservice.persistence.model.Project
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.documents " +
            "WHERE project.uuid = ?1"
    )
    fun findByIdWithAllData(id: UUID): Optional<Project>

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "WHERE organization.uuid = ?1 AND organization.coop = ?2"
    )
    fun findAllByOrganizationUuid(organizationUuid: UUID, coop: String): List<Project>

    fun findByNameContainingIgnoreCaseAndCoop(name: String, coop: String, pageable: Pageable): Page<Project>

    @Query(
        "SELECT project FROM Project project JOIN project.tags t " +
            "WHERE t IN (:tags) AND project.coop = :coop AND project.active = :active " +
            "GROUP BY project.uuid HAVING COUNT (project.uuid) = :size"
    )
    fun findByTags(
        tags: Collection<String>,
        size: Long,
        coop: String,
        pageable: Pageable,
        active: Boolean = true
    ): Page<Project>

    @Query(
        "SELECT project FROM Project project " +
            "WHERE project.startDate <= :date AND project.endDate >= :date " +
            "AND project.active = :active AND project.coop = :coop"
    )
    @Cacheable(value = [PROJECT_CACHE], key = "#date.hashCode().toString() + #active + #coop + #pageable.hashCode()")
    fun findByActive(date: LocalDate, active: Boolean, coop: String, pageable: Pageable): Page<Project>

    @Query(
        "SELECT COUNT(project.uuid) FROM Project project " +
            "WHERE project.startDate <= :date AND project.endDate >= :date AND project.active = :active " +
            "AND project.coop = :coop"
    )
    fun countAllActiveByDate(date: LocalDate, active: Boolean, coop: String): Int

    @Cacheable(value = [PROJECT_CACHE], key = "#coop + #pageable.hashCode()")
    fun findAllByCoop(coop: String, pageable: Pageable): Page<Project>

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "WHERE organization.uuid IN (:organizationUuids)"
    )
    fun findAllByOrganizations(organizationUuids: List<UUID>): List<Project>

    @Query(
        "SELECT COUNT(project.uuid) FROM Project project " +
            "INNER JOIN project.organization organization WHERE organization.uuid = ?1"
    )
    fun countProjectsByOrganization(organizationUuid: UUID): Int
}
