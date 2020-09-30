package com.ampnet.projectservice.controller.pojo.request

import javax.validation.constraints.Size

data class OrganizationRequest(
    @field:Size(max = 256)
    val name: String,
    val description: String
)
