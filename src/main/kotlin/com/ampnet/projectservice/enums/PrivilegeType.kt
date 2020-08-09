package com.ampnet.projectservice.enums

enum class PrivilegeType {

    /*
    Def: <type>_privilege

    type:
        - PR - PERM_READ
        - PW - PERM_WRITE
        - PRO - PERM_READ_OWN
        - PWO - PER_WRITE_OWN
        - PRA - PERM_READ_ADMIN
        - PWA - PERM_WRITE_ADMIN
     */

    // Administration
    MONITORING
}

enum class OrganizationPrivilegeType {
    // Administration
    PR_USERS,
    PW_USERS,

    // Organization
    PW_ORG,

    PW_PROJECT
}
