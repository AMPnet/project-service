CREATE INDEX idx_document_link ON document(link);

CREATE INDEX idx_project_name ON project(name);
CREATE INDEX idx_project_organization_uuid ON project(organization_uuid);
CREATE INDEX idx_project_tag ON project_tag(project_uuid);

CREATE INDEX idx_organization_name ON organization(name);
CREATE INDEX idx_org_membership_org_uuid ON organization_membership(organization_uuid);
CREATE INDEX idx_org_membership_user_uuid ON organization_membership(user_uuid);
CREATE INDEX idx_org_invitation_email ON organization_invitation(email);
