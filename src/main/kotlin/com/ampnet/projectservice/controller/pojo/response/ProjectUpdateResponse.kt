package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.persistence.model.ProjectUpdate
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectUpdateResponse(
    val id: Int,
    val projectUuid: UUID,
    val title: String,
    val content: String,
    val author: String,
    val date: ZonedDateTime
) {
    constructor(update: ProjectUpdate) :
        this(update.id, update.projectUuid, update.title, update.content, update.author, update.createdAt)
}
data class ProjectUpdatesResponse(val updates: List<ProjectUpdateResponse>)
