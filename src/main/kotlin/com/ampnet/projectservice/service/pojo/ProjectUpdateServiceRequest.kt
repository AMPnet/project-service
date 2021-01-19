package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class ProjectUpdateServiceRequest(
    val projectUuid: UUID,
    val userUuid: UUID,
    val request: ProjectUpdateRequest?,
    val image: MultipartFile?,
    val documentSaveRequests: List<DocumentSaveRequest>?
)
