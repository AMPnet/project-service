package com.ampnet.projectservice.repository

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.Organization
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OrganizationInviteRepositoryTest : RepositoryTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
        fillOrganization()
    }

    @Test
    fun mustGetAllInvitationsByUserEmail() {
        verify("Jpa query returns organization invites for user email") {
            val invites = organizationInviteRepository.findAllByEmail(defaultEmail)
            assertThat(invites).hasSize(2)
            val invite = invites.first()
            assertThat(Hibernate.isInitialized(invite.organization)).isTrue()
            assertThat(Hibernate.isInitialized(invite.organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(invite.organization.memberships)).isFalse()
        }
    }

    @Test
    fun mustGetAllInvitationsByOrganizationUuid() {
        verify("Jpa query returns organization invites for the organization") {
            val invites = organizationInviteRepository.findByOrganizationUuid(testContext.organization.uuid)
            assertThat(invites).hasSize(1)
            val invite = invites.first()
            assertThat(Hibernate.isInitialized(invite.organization)).isFalse()
            assertThat(Hibernate.isInitialized(invite.organization.documents)).isFalse()
            assertThat(Hibernate.isInitialized(invite.organization.memberships)).isFalse()
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
