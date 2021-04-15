package com.ampnet.projectservice.service

import com.ampnet.projectservice.controller.COOP
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.service.impl.SearchServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchServiceTest : JpaServiceTestBase() {

    private val searchService: SearchService by lazy {
        SearchServiceImpl(organizationRepository, projectRepository, applicationProperties, imageProxyService)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun inti() {
        testContext = TestContext()
    }

    /* Organization search */
    @Test
    fun mustReturnEmptyListForNonExistingOrganization() {
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
            createOrganization("Org 2", userUuid)
            createOrganization("Org 3", userUuid)
        }

        verify("Service will return empty list") {
            val organizations = searchService.searchOrganizations("Non existing", COOP, defaultPageable)
            assertThat(organizations).hasSize(0)
        }
    }

    @Test
    fun mustReturnOrganizationBySearchedName() {
        suppose("Some organization exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.organizationName = "Das X"
            createOrganization(testContext.organizationName, userUuid)
        }
        suppose("Hidden organization exists") {
            createOrganization("Hidden org", userUuid, false)
        }

        verify("Service will find one organization") {
            val organizations = searchService
                .searchOrganizations(testContext.organizationName, COOP, defaultPageable)
            assertThat(organizations).hasSize(1)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.organizationName)
            assertThat(org.coop).isEqualTo(COOP)
        }
    }

    @Test
    fun mustReturnOrganizationBySimilarSearchedName() {
        suppose("Some organization exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
            createOrganization("Org 2", userUuid)
            createOrganization("Org 3", userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.organizationName = "Das X"
            createOrganization(testContext.organizationName, userUuid)
        }
        suppose("Hidden organization exist") {
            createOrganization("Hidden org 4", userUuid, false)
        }

        verify("Service will find by name in lower case") {
            val organizations = searchService
                .searchOrganizations(testContext.organizationName.toLowerCase(), COOP, defaultPageable)
            assertThat(organizations).hasSize(1)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.organizationName)
            assertThat(org.coop).isEqualTo(COOP)
        }
        verify("Service will find by similar in upper case") {
            val organizations = searchService
                .searchOrganizations(testContext.organizationName.toUpperCase(), COOP, defaultPageable)
            assertThat(organizations).hasSize(1)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.organizationName)
            assertThat(org.coop).isEqualTo(COOP)
        }
        verify("Service will find by name if the last word is missing") {
            val organizations = searchService
                .searchOrganizations(testContext.organizationName.split(" ")[0], COOP, defaultPageable)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.organizationName)
            assertThat(org.coop).isEqualTo(COOP)
        }
        verify("Service will find by name if the first word is missing") {
            val organizations = searchService
                .searchOrganizations(testContext.organizationName.split(" ")[1], COOP, defaultPageable)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.organizationName)
            assertThat(org.coop).isEqualTo(COOP)
        }
        verify("Service will find multiple organizations with similar name") {
            val organizations = searchService.searchOrganizations("Org", COOP, defaultPageable)
            assertThat(organizations).hasSize(3)
        }
    }

    /*Project search */
    @Test
    fun mustReturnEmptyListForNonExistingProject() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
            createProject("Project 2", testContext.organization, userUuid)
            createProject("Project 3", testContext.organization, userUuid)
        }

        verify("Service will return empty list") {
            val projects = searchService.searchProjects("Non existing", COOP, defaultPageable)
            assertThat(projects).hasSize(0)
        }
    }

    @Test
    fun mustReturnProjectBySearchedName() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.projectName = "Prj with name"
            createProject(testContext.projectName, testContext.organization, userUuid)
        }

        verify("Service will find one organization") {
            val organizations = searchService.searchProjects(testContext.projectName, COOP, defaultPageable)
            assertThat(organizations).hasSize(1)
            val org = organizations.first()
            assertThat(org.name).isEqualTo(testContext.projectName)
            assertThat(org.coop).isEqualTo(COOP)
        }
    }

    @Test
    fun mustReturnProjectBySimilarSearchedName() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.projectName = "Prj with name"
            createProject(testContext.projectName, testContext.organization, userUuid)
        }

        verify("Service will find by name in lower case") {
            val projects = searchService
                .searchProjects(testContext.projectName.toLowerCase(), COOP, defaultPageable)
            assertThat(projects).hasSize(1)
            val project = projects.first()
            assertThat(project.name).isEqualTo(testContext.projectName)
            assertThat(project.coop).isEqualTo(COOP)
        }
        verify("Service will find by similar in upper case") {
            val projects = searchService
                .searchProjects(testContext.projectName.toUpperCase(), COOP, defaultPageable)
            assertThat(projects).hasSize(1)
            val project = projects.first()
            assertThat(project.name).isEqualTo(testContext.projectName)
            assertThat(project.coop).isEqualTo(COOP)
        }
        verify("Service will find by name if the last word is missing") {
            val projects = searchService
                .searchProjects(testContext.projectName.split(" ")[0], COOP, defaultPageable)
            assertThat(projects).hasSize(1)
            val project = projects.first()
            assertThat(project.name).isEqualTo(testContext.projectName)
            assertThat(project.coop).isEqualTo(COOP)
        }
        verify("Service will find by name if the first word is missing") {
            val projects = searchService
                .searchProjects(testContext.projectName.split(" ")[1], COOP, defaultPageable)
            assertThat(projects).hasSize(1)
            val project = projects.first()
            assertThat(project.name).isEqualTo(testContext.projectName)
            assertThat(project.coop).isEqualTo(COOP)
        }
        verify("Service will return 2 projects that start with same letter") {
            val projects = searchService.searchProjects("P", COOP, defaultPageable)
            assertThat(projects).hasSize(2)
        }
    }

    private class TestContext {
        lateinit var organizationName: String
        lateinit var organization: Organization
        lateinit var projectName: String
    }
}
