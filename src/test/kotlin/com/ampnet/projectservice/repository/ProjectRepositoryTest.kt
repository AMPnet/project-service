package com.ampnet.projectservice.repository

import com.ampnet.projectservice.config.PROJECT_CACHE
import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

class ProjectRepositoryTest : RepositoryTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
        fillProjectAndOrganization()
    }

    @Test
    fun mustGetAllByIdWitAllData() {
        verify("Jpa query returns project with documents and organization data") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.uuid)
            val project = optionalProject.get()
            assertThat(project).isNotNull
            assertThat(Hibernate.isInitialized(project.documents)).isTrue()
            assertThat(Hibernate.isInitialized(project.organization)).isTrue()
            assertThat(Hibernate.isInitialized(project.tags)).isTrue()
            assertThat(Hibernate.isInitialized(project.gallery)).isFalse()
            assertThat(Hibernate.isInitialized(project.newsLinks)).isFalse()
            val organization = project.organization
            assertThat(Hibernate.isInitialized(organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(organization.memberships)).isFalse()
        }
    }

    @Test
    fun mustFindAllByOrganizationUuid() {
        verify("Jpa query returns projects with organization data") {
            val projects = projectRepository.findAllByOrganizationUuid(testContext.organization.uuid, coop)
            assertThat(projects).hasSize(1)
            val project = projects.first()
            assertThat(Hibernate.isInitialized(project.organization)).isTrue()
            assertThat(Hibernate.isInitialized(project.tags)).isTrue()
            assertThat(Hibernate.isInitialized(project.documents)).isFalse()
            assertThat(Hibernate.isInitialized(project.gallery)).isFalse()
            assertThat(Hibernate.isInitialized(project.newsLinks)).isFalse()
            val organization = project.organization
            assertThat(Hibernate.isInitialized(organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(organization.memberships)).isFalse()
        }
    }

    @Test
    fun mustSaveProjectsInCacheWhenFindAllByCoop() {
        suppose("Jpa query returns list of projects and saves it in cache") {
            projectRepository.findAllByCoop(coop, defaultPageable)
        }
        suppose("Another project is created") {
            createProject("project2", testContext.organization, userUuid)
        }

        verify("On second call to project repository projects are returned from cache") {
            val key = coop + defaultPageable.hashCode()
            val pageImpl = cacheManager.getCache(PROJECT_CACHE)?.get(key)?.get() as PageImpl<*>
            val projects = pageImpl.content as List<*>
            assertThat(projects).hasSize(1)
        }
        verify("When calling project repository with different page size query hits the database") {
            val pageable = PageRequest.of(0, 10)
            val projects = projectRepository.findAllByCoop(coop, pageable)
            assertThat(projects).hasSize(2)
        }
    }

    private fun fillProjectAndOrganization() {
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizations()
        testContext.organization = createOrganization("Test org", testContext.uuid)
        createOrganizationDocument(testContext.organization, userUuid)
        addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        testContext.project = createProject("project1", testContext.organization, userUuid)
        createProjectDocument(testContext.project, userUuid)
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var project: Project
        val uuid: UUID = UUID.randomUUID()
    }
}
