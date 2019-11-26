package com.ampnet.projectservice.grpc.mailservice

interface MailService {
    fun sendOrganizationInvitationMail(email: String, organizationName: String)
}
