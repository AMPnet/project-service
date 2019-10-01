package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.service.SearchService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository
) : SearchService {

    @Transactional(readOnly = true)
    override fun searchOrganizations(name: String): List<Organization> {
        return organizationRepository.findByNameContainingIgnoreCase(name)
    }

    @Transactional(readOnly = true)
    override fun searchProjects(name: String): List<Project> {
        return projectRepository.findByNameContainingIgnoreCase(name)
    }
}
