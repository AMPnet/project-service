package com.ampnet.projectservice.service

interface CloudStorageService {
    fun saveFile(name: String, content: ByteArray): String
    fun deleteFile(link: String)
}
