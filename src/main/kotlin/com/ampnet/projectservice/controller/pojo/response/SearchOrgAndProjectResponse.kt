package com.ampnet.projectservice.controller.pojo.response

data class SearchOrgAndProjectResponse(
    val organizations: List<OrganizationResponse>,
    val projects: List<ProjectResponse>
)
