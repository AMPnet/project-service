CREATE TABLE project_tag (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    tag VARCHAR(128)
);
