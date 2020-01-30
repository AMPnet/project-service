package com.ampnet.projectservice.controller.pojo.request

data class ProjectUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val locationText: String? = null,
    val returnOnInvestment: String? = null,
    val active: Boolean? = null,
    val tags: List<String>? = null,
    val news: List<String>? = null
)
