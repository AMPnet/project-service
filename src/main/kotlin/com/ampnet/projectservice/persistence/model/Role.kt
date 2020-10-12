package com.ampnet.projectservice.persistence.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "role")
class Role(
    @Id
    val id: Int,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val description: String
)
