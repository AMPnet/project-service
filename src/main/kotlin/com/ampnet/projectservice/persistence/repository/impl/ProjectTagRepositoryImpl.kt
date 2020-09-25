package com.ampnet.projectservice.persistence.repository.impl

import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
class ProjectTagRepositoryImpl : ProjectTagRepository {

    @PersistenceContext
    private lateinit var em: EntityManager

    override fun getAllTagsByCoop(coop: String): List<String> {
        val query = em.createNativeQuery(
            "SELECT DISTINCT(tag) FROM project INNER JOIN project_tag ON project.uuid = project_tag.project_uuid " +
                "WHERE project.coop = ?1"
        )
        query.setParameter(1, coop)
        return query.resultList.map { it.toString() }
    }
}
