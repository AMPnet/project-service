package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.service.SearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository,
    private val applicationProperties: ApplicationProperties
) : SearchService {

    @Transactional(readOnly = true)
    override fun searchOrganizations(name: String, coop: String?, pageable: Pageable): Page<Organization> {
        return organizationRepository.findByNameContainingIgnoreCaseAndCoop(
            name, coop ?: applicationProperties.coop.default, pageable
        )
    }

    @Transactional(readOnly = true)
    override fun searchProjects(name: String, coop: String?, pageable: Pageable): Page<Project> {
        return projectRepository.findByNameContainingIgnoreCaseAndCoop(
            name, coop ?: applicationProperties.coop.default, pageable
        )
    }
}
