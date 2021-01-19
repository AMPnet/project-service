package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.exception.InternalException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.service.CloudStorageService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class StorageServiceImpl(
    private val documentRepository: DocumentRepository,
    private val cloudStorageService: CloudStorageService
) : StorageService {

    companion object : KLogging()

    @Throws(InternalException::class)
    override fun saveDocument(request: DocumentSaveRequest): Document {
        logger.debug { "Storing document: ${request.name}" }

        val fileLink = storeOnCloud(request.name, request.data)
        logger.debug { "Successfully stored document on cloud: $fileLink" }

        val document = Document(fileLink, request)
        return documentRepository.save(document)
    }

    @Throws(InternalException::class)
    override fun saveImage(name: String, content: ByteArray): String {
        logger.debug { "Storing image: $name" }
        val link = storeOnCloud(name, content)
        logger.debug { "Successfully stored image on cloud: $link" }
        return link
    }

    override fun deleteImage(link: String) {
        logger.debug { "Deleting image: $link" }
        cloudStorageService.deleteFile(link)
    }

    override fun deleteFile(document: Document) {
        logger.debug { "Deleting file: ${document.link}" }
        cloudStorageService.deleteFile(document.link)
        documentRepository.delete(document)
    }

    private fun storeOnCloud(name: String, content: ByteArray): String = cloudStorageService.saveFile(name, content)
}
