package com.ampnet.projectservice.persistence.repository

interface ProjectTagRepository {
    fun getAllTagsByCoop(coop: String): List<String>
}
