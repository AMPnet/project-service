package com.ampnet.projectservice.enums

enum class OrganizationRole(val id: Int) {

    ORG_ADMIN(1) {
        override fun getPrivileges(): List<OrganizationPrivilegeType> {
            return listOf(
                OrganizationPrivilegeType.PR_USERS,
                OrganizationPrivilegeType.PW_USERS,
                OrganizationPrivilegeType.PW_ORG,
                OrganizationPrivilegeType.PW_PROJECT
            )
        }
    },

    ORG_MEMBER(2) {
        override fun getPrivileges(): List<OrganizationPrivilegeType> {
            return listOf(
                OrganizationPrivilegeType.PR_USERS
            )
        }
    };

    companion object {
        private val map = values().associateBy(OrganizationRole::id)
        fun fromInt(type: Int) = map[type]
    }

    abstract fun getPrivileges(): List<OrganizationPrivilegeType>
}
