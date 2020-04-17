CREATE TABLE project_update(
    id SERIAL PRIMARY KEY,
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    author VARCHAR NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_project_update ON project_update(project_uuid);
