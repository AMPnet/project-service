package com.ampnet.projectservice.service

import com.ampnet.projectservice.persistence.model.OrganizationFollower
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import java.util.UUID

interface OrganizationInviteService {
    fun sendInvitation(request: OrganizationInviteServiceRequest): OrganizationInvitation
    fun revokeInvitation(organizationId: Int, email: String)
    fun getAllInvitationsForUser(email: String): List<OrganizationInvitation>
    fun answerToInvitation(request: OrganizationInviteAnswerRequest)
    fun followOrganization(userUuid: UUID, organizationId: Int): OrganizationFollower
    fun unfollowOrganization(userUuid: UUID, organizationId: Int)
}
