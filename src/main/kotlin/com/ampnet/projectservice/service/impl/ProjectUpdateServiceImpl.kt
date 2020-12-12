package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.ProjectUpdate
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.ProjectRepository
import com.ampnet.projectservice.persistence.repository.ProjectUpdateRepository
import com.ampnet.projectservice.service.ProjectUpdateService
import com.ampnet.projectservice.service.pojo.CreateProjectUpdate
import mu.KLogging
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectUpdateServiceImpl(
    private val projectUpdateRepository: ProjectUpdateRepository,
    private val projectRepository: ProjectRepository,
    private val organizationMembershipRepository: OrganizationMembershipRepository
) : ProjectUpdateService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getProjectUpdates(project: UUID, pageable: Pageable): List<ProjectUpdate> =
        projectUpdateRepository.findByProjectUuid(project, pageable)

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun createProjectUpdate(request: CreateProjectUpdate): ProjectUpdate {
        verifyUserCanEditProject(request.user, request.project)
        val projectUpdate = ProjectUpdate(request.project, request.title, request.content, request.author, request.user)
        return projectUpdateRepository.save(projectUpdate)
    }

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun deleteProjectUpdate(user: UUID, projectUuid: UUID, id: Int) {
        verifyUserCanEditProject(user, projectUuid)
        projectUpdateRepository.deleteById(id)
    }

    private fun verifyUserCanEditProject(user: UUID, projectUuid: UUID) {
        val project = projectRepository.findById(projectUuid).orElseThrow {
            InvalidRequestException(ErrorCode.PRJ_MISSING, "Missing project: $projectUuid")
        }
        if (canUserWriteOrgProject(user, project.organization.uuid).not()) {
            throw InvalidRequestException(
                ErrorCode.USER_ROLE_INVALID, "User: $user does not have a privilege to edit project: ${project.uuid}"
            )
        }
    }

    private fun canUserWriteOrgProject(userUuid: UUID, organizationUuid: UUID): Boolean {
        val membership = ServiceUtils.wrapOptional(
            organizationMembershipRepository.findByOrganizationUuidAndUserUuid(organizationUuid, userUuid)
        )
        return membership?.hasPrivilegeToWriteProject() ?: false
    }
}
