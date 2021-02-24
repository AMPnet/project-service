package com.ampnet.projectservice.amqp.mailservice

interface MailService {
    fun sendOrganizationInvitationMail(message: MailOrgInvitationMessage)
}
