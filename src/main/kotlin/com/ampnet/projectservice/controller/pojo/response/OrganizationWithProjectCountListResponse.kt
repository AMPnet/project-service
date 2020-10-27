package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.service.pojo.OrganizationWitProjectCountServiceResponse

class OrganizationWithProjectCountListResponse(
    val organizations: List<OrganizationWitProjectCountServiceResponse>,
    val page: Int = 0,
    val totalPages: Int = 1
)
