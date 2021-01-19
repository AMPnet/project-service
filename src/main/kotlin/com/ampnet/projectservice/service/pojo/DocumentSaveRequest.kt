package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.enums.DocumentPurpose
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class DocumentSaveRequest(
    val data: ByteArray,
    val name: String,
    val size: Int,
    val type: String,
    val userUuid: UUID,
    val purpose: DocumentPurpose
) {
    constructor(file: MultipartFile, userUuid: UUID, purpose: DocumentPurpose = DocumentPurpose.GENERIC) : this(
        file.bytes,
        file.originalFilename ?: file.name,
        file.size.toInt(),
        file.contentType ?: file.originalFilename?.split(".")?.lastOrNull() ?: "Unknown",
        userUuid,
        purpose
    )
}
