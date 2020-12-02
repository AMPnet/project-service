ALTER TABLE organization DROP CONSTRAINT organization_name_key;
ALTER TABLE organization ADD CONSTRAINT uc_organization_name_coop UNIQUE (name, coop);

DROP INDEX idx_organization_name;
DROP INDEX idx_project_name;
