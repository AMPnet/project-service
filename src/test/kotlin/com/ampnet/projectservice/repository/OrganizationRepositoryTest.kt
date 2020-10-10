package com.ampnet.projectservice.repository

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Organization
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OrganizationRepositoryTest : RepositoryTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
        fillOrganization()
    }

    @Test
    fun mustGetAllOrganizationsInWhichUserIsMember() {
        verify("Jpa query returns organizations in which user is member") {
            val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
            assertThat(organizations).hasSize(2)
            val organization = organizations.first()
            assertThat(Hibernate.isInitialized(organization.memberships)).isTrue()
            assertThat(Hibernate.isInitialized(organization.documents)).isFalse()
        }
    }

    @Test
    fun mustGetOrganizationByName() {
        verify("Jpa query returns organization") {
            val organization = organizationRepository.findByName(testContext.organization.name).get()
            assertThat(organization).isNotNull
            assertThat(Hibernate.isInitialized(organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(organization.memberships)).isFalse()
        }
    }

    @Test
    fun mustGetOrganizationWithDocument() {
        verify("Jpa query return organizations with document") {
            val organization = organizationRepository.findByIdWithDocuments(testContext.organization.uuid).get()
            assertThat(organization).isNotNull
            assertThat(Hibernate.isInitialized(organization.documents)).isTrue()
            assertThat(Hibernate.isInitialized(organization.memberships)).isFalse()
        }
    }

    private fun fillOrganization() {
        databaseCleanerService.deleteAllOrganizations()
        testContext.organization = createOrganization("Test org", testContext.uuid)
        testContext.anotherOrganization = createOrganization("Test org 2", testContext.uuid)
        createOrganizationInvite(
            defaultEmail, testContext.organization, testContext.uuid,
            OrganizationRoleType.ORG_MEMBER
        )
        createOrganizationInvite(
            defaultEmail, testContext.anotherOrganization, testContext.uuid,
            OrganizationRoleType.ORG_MEMBER
        )
        createOrganizationDocument(testContext.organization, userUuid)
        createOrganizationDocument(testContext.anotherOrganization, userUuid)
        addUserToOrganization(userUuid, testContext.organization.uuid, OrganizationRoleType.ORG_MEMBER)
        addUserToOrganization(userUuid, testContext.anotherOrganization.uuid, OrganizationRoleType.ORG_ADMIN)
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var anotherOrganization: Organization
        val uuid: UUID = UUID.randomUUID()
    }
}
