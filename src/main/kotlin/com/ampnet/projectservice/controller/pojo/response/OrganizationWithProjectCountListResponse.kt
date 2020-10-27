package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.service.pojo.OrganizationWitProjectCountServiceResponse

data class OrganizationWithProjectCountListResponse(
    val organizations: List<OrganizationWitProjectCountServiceResponse>
)
