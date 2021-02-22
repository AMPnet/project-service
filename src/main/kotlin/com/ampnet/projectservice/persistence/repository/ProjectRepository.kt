package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.tags " +
            "LEFT JOIN FETCH project.documents " +
            "LEFT JOIN FETCH project.newsLinks " +
            "LEFT JOIN FETCH project.gallery " +
            "WHERE project.uuid = ?1"
    )
    fun findByIdWithAllData(id: UUID): Optional<Project>

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.tags " +
            "WHERE project.organization.uuid = ?1 AND project.coop = ?2"
    )
    fun findAllByOrganizationUuid(organizationUuid: UUID, coop: String): Set<Project>

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
        active: Boolean,
        pageable: Pageable
    ): Page<Project>

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization  " +
            "LEFT JOIN FETCH project.tags " +
            "WHERE project.startDate < :time AND project.endDate > :time " +
            "AND project.active = :active AND project.coop = :coop",
        countQuery = "SELECT COUNT(project.uuid) FROM Project project " +
            "WHERE project.startDate < :time AND project.endDate > :time " +
            "AND project.active = :active AND project.coop = :coop"
    )
    fun findByActive(time: ZonedDateTime, active: Boolean, coop: String, pageable: Pageable): Page<Project>

    @Query(
        "SELECT COUNT(project.uuid) FROM Project project " +
            "WHERE project.startDate < :time AND project.endDate > :time AND project.active = :active " +
            "AND project.coop = :coop"
    )
    fun countAllActiveByDate(time: ZonedDateTime, active: Boolean, coop: String): Int

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization  " +
            "LEFT JOIN FETCH project.tags " +
            "WHERE project.coop = :coop",
        countQuery = "SELECT COUNT(project.uuid) FROM Project project " +
            "WHERE project.coop = :coop"
    )
    fun findAllByCoop(coop: String, pageable: Pageable): Page<Project>

    @Query(
        "SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "LEFT JOIN FETCH project.tags " +
            "WHERE organization.uuid IN (:organizationUuids)"
    )
    fun findAllByOrganizations(organizationUuids: List<UUID>): Set<Project>

    @Query(
        "SELECT COUNT(project.uuid) FROM Project project " +
            "INNER JOIN project.organization organization WHERE organization.uuid = ?1"
    )
    fun countProjectsByOrganization(organizationUuid: UUID): Int
}
