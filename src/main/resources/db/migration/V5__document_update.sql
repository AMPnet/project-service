DROP TABLE organization_document;
DROP TABLE project_document;
ALTER TABLE document ADD COLUMN organization_uuid UUID;
ALTER TABLE document ADD COLUMN project_uuid UUID;
