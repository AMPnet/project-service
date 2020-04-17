package com.ampnet.projectservice.service.pojo

import java.util.UUID

data class CreateProjectUpdate(
    val user: UUID,
    val author: String,
    val project: UUID,
    val title: String,
    val content: String
)
