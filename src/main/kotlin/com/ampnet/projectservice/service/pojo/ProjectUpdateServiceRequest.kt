package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.projectservice.persistence.model.Project
import org.springframework.web.multipart.MultipartFile

data class ProjectUpdateServiceRequest(
    val project: Project,
    val request: ProjectUpdateRequest,
    val image: MultipartFile?,
    val documentSaveRequests: List<DocumentSaveRequest>?
)
