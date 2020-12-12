package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.exception.InternalException
import com.ampnet.projectservice.persistence.model.Document
import com.ampnet.projectservice.persistence.repository.DocumentRepository
import com.ampnet.projectservice.service.CloudStorageService
import com.ampnet.projectservice.service.StorageService
import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class StorageServiceImpl(
    private val documentRepository: DocumentRepository,
    private val cloudStorageService: CloudStorageService
) : StorageService {

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_DOCUMENT_TYPE_NAME = 16
    }

    @Throws(InternalException::class)
    override fun saveDocument(request: DocumentSaveRequest): Document {
        logger.debug { "Storing document: ${request.name}" }

        val fileLink = storeOnCloud(request.name, request.data)
        logger.debug { "Successfully stored document on cloud: $fileLink" }

        val document = Document::class.java.getDeclaredConstructor().newInstance()
        document.link = fileLink
        document.name = request.name
        document.size = request.size
        document.createdAt = ZonedDateTime.now()
        document.createdByUserUuid = request.userUuid
        document.type = request.type.take(MAX_DOCUMENT_TYPE_NAME)
        return documentRepository.save(document)
    }

    @Throws(InternalException::class)
    override fun saveImage(name: String, content: ByteArray): String {
        logger.debug { "Storing image: $name" }
        val link = storeOnCloud(name, content)
        logger.debug { "Successfully stored image on cloud: $link" }
        return link
    }

    @Throws(InternalException::class)
    override fun deleteImage(link: String) {
        logger.debug { "Deleting image: $link" }
        cloudStorageService.deleteFile(link)
    }

    private fun storeOnCloud(name: String, content: ByteArray): String = cloudStorageService.saveFile(name, content)
}
