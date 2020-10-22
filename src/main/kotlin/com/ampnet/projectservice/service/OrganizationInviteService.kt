package com.ampnet.projectservice.service

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.persistence.model.OrganizationFollower
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.service.pojo.OrganizationInvitationWithData
import com.ampnet.projectservice.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import java.util.UUID

interface OrganizationInviteService {
    fun sendInvitation(request: OrganizationInviteServiceRequest)
    fun revokeInvitation(organizationUuid: UUID, email: String)
    fun getAllInvitationsForUser(userPrincipal: UserPrincipal): List<OrganizationInvitationWithData>
    fun answerToInvitation(request: OrganizationInviteAnswerRequest)
    fun followOrganization(userUuid: UUID, organizationUuid: UUID): OrganizationFollower
    fun unfollowOrganization(userUuid: UUID, organizationUuid: UUID)
    fun getPendingInvitations(organizationUuid: UUID): List<OrganizationInvitation>
}
