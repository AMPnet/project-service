package com.ampnet.projectservice.service.pojo

data class OrganizationInvitationMailRequest(
    val emails: List<String>,
    val organizationName: String,
    val senderEmail: String,
    val coop: String
)
