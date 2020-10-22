CREATE INDEX idx_project_name ON project(name);
CREATE INDEX idx_organization_name ON organization(name);

CREATE INDEX idx_project_organization_uuid_coop ON project(organization_uuid, coop);
CREATE INDEX idx_org_membership_org_uuid ON organization_membership(organization_uuid);
CREATE INDEX idx_org_membership_user_uuid ON organization_membership(user_uuid);

