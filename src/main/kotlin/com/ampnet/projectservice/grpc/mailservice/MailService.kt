package com.ampnet.projectservice.grpc.mailservice

interface MailService {
    fun sendOrganizationInvitationMail(emails: List<String>, organizationName: String)
}
