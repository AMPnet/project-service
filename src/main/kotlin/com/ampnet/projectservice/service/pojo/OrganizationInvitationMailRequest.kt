package com.ampnet.projectservice.service.pojo

class OrganizationInvitationMailRequest(
    val emails: List<String>,
    val organizationName: String,
    val senderEmail: String,
    val coop: String
)
