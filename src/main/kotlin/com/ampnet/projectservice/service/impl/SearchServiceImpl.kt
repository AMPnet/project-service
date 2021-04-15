package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.repository.OrganizationRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.SearchService
import com.ampnet.projectservice.service.pojo.ProjectServiceResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository,
    private val applicationProperties: ApplicationProperties,
    private val imageProxyService: ImageProxyService
) : SearchService {

    @Transactional(readOnly = true)
    override fun searchOrganizations(
        name: String,
        coop: String?,
        pageable: Pageable,
        active: Boolean
    ): Page<Organization> {
        return organizationRepository.findByNameContainingIgnoreCaseAndCoopAndActive(
            name, coop ?: applicationProperties.coop.default, active, pageable
        )
    }

    @Transactional(readOnly = true)
    override fun searchProjects(name: String, coop: String?, pageable: Pageable): Page<ProjectServiceResponse> {
        val projects = projectRepository.findByNameContainingIgnoreCaseAndCoop(
            name, coop ?: applicationProperties.coop.default, pageable
        )
        return projects.map { ProjectServiceResponse(it, imageProxyService.generateImageResponse(it.mainImage)) }
    }
}
