package com.ampnet.projectservice.repository

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.Project
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
            assertThat(Hibernate.isInitialized(project.termsOfService)).isTrue()
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
            assertThat(Hibernate.isInitialized(project.termsOfService)).isFalse()
            val organization = project.organization
            assertThat(Hibernate.isInitialized(organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(organization.memberships)).isFalse()
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var project: Project
        val uuid: UUID = UUID.randomUUID()
    }

    private fun fillProjectAndOrganization() {
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizations()
        testContext.organization = createOrganization("Test org", testContext.uuid)
        createOrganizationDocument(testContext.organization, userUuid)
        addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRole.ORG_MEMBER)
        testContext.project = createProject("project1", testContext.organization, userUuid)
        createProjectDocument(testContext.project, userUuid)
        createTermsOfServiceDocument(testContext.project, userUuid)
    }
}
