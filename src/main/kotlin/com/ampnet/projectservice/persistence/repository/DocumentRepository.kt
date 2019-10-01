package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Document
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<Document, Int>
