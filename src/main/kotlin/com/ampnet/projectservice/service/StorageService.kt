package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest

interface StorageService {
    fun saveDocument(request: DocumentSaveRequest): Document
    fun saveImage(name: String, content: ByteArray): String
    fun deleteImage(link: String)
    fun deleteFile(document: Document)
}
