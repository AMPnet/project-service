package com.ampnet.projectservice.controller.pojo.request

import javax.validation.constraints.Size

data class ProjectUpdatesRequest(
    @field:Size(max = 256)
    val title: String,
    val content: String
)
