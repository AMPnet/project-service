package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Int>
