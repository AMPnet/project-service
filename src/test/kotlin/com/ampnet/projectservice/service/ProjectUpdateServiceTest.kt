package com.ampnet.projectservice.service

import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.InvalidRequestException
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectUpdate
import com.ampnet.projectservice.persistence.repository.ProjectUpdateRepository
import com.ampnet.projectservice.service.impl.ProjectUpdateServiceImpl
import com.ampnet.projectservice.service.pojo.CreateProjectUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ProjectUpdateServiceTest : JpaServiceTestBase() {

    @Autowired
    private lateinit var projectUpdateRepository: ProjectUpdateRepository

    private val service: ProjectUpdateServiceImpl by lazy {
        ProjectUpdateServiceImpl(projectUpdateRepository, projectRepository, membershipRepository)
    }
    private val project: Project by lazy {
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllProjects()
        val organization = createOrganization("Update org", userUuid)
        createProject("Project update", organization, userUuid)
    }
    private lateinit var projectUpdate: ProjectUpdate

    @Test
    // @Transactional
    fun mustNotBeAbleToCreateProjectUpdateWithoutAdminPrivilege() {
        suppose("User is not project admin") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }

        verify("User without project admin privilege cannot create project update") {
            val exception = assertThrows<InvalidRequestException> {
                val serviceRequest = CreateProjectUpdate(userUuid, "author", project.uuid, "title", "content")
                service.createProjectUpdate(serviceRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_ROLE_INVALID)
        }
    }

    @Test
    // @Transactional
    fun mustNotBeAbleToDeleteProjectUpdateWithoutAdminPrivilege() {
        suppose("User is not project admin") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("Project update is created") {
            projectUpdate = ProjectUpdate(project.uuid, "t", "c", "author", userUuid)
        }

        verify("User without project admin privilege cannot delete project update") {
            val exception = assertThrows<InvalidRequestException> {
                service.deleteProjectUpdate(userUuid, project.uuid, projectUpdate.id)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_ROLE_INVALID)
        }
    }
}
