package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project

interface SearchService {
    fun searchOrganizations(name: String): List<Organization>
    fun searchProjects(name: String): List<Project>
}
