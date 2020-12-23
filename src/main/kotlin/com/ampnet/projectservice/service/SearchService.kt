package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.pojo.ProjectServiceResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SearchService {
    fun searchOrganizations(name: String, coop: String?, pageable: Pageable, active: Boolean = true): Page<Organization>
    fun searchProjects(name: String, coop: String?, pageable: Pageable): Page<ProjectServiceResponse>
}
