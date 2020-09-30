package com.ampnet.projectservice.controller.pojo.request

import javax.validation.constraints.Size

data class OrganizationUpdateRequest(
    @field:Size(max = 256)
    val description: String?
)
