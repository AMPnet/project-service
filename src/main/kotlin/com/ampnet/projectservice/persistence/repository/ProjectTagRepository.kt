package com.ampnet.projectservice.persistence.repository

interface ProjectTagRepository {
    fun getAllTags(): List<String>
}
