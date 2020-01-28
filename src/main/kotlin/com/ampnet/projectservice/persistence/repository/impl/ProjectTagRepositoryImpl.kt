package com.ampnet.projectservice.persistence.repository.impl

import com.ampnet.projectservice.persistence.repository.ProjectTagRepository
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.springframework.stereotype.Repository

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
