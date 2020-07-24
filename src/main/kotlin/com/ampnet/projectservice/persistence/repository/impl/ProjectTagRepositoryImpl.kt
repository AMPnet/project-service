package com.ampnet.projectservice.persistence.repository.impl

import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Repository
class ProjectTagRepositoryImpl :
    ProjectTagRepository {

    @PersistenceContext
    private lateinit var em: EntityManager

    override fun getAllTags(): List<String> {
        val query = em.createNativeQuery("SELECT tag FROM project_tag GROUP BY tag")
        return query.resultList.map { it.toString() }
    }
}
