package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.Organization
import java.util.UUID

data class OrganizationSmallServiceResponse(val uuid: UUID, val name: String) {
    constructor(organization: Organization) : this(organization.uuid, organization.name)
}
