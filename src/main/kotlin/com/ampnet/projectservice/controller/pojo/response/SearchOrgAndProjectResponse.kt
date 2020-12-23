package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.service.pojo.ProjectServiceResponse

data class SearchOrgAndProjectResponse(
    val organizations: List<OrganizationResponse>,
    val projects: List<ProjectServiceResponse>
)
