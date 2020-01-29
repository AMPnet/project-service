ALTER TABLE project DROP COLUMN news_links;
ALTER TABLE project DROP COLUMN gallery;

CREATE TABLE project_news (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    link VARCHAR NOT NULL
);
CREATE TABLE project_gallery (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    image VARCHAR NOT NULL
);
