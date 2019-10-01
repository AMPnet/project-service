-- Role
CREATE TABLE role (
  id INT PRIMARY KEY,
  name VARCHAR NOT NULL,
  description VARCHAR NOT NULL
);
INSERT INTO role VALUES
  (1, 'ORG_ADMIN', 'Administrators can manage users in organization.');
INSERT INTO role VALUES
  (2, 'ORG_MEMBER', 'Members can use organization.');

-- Organization
CREATE TABLE organization (
    uuid UUID PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    legal_info VARCHAR
);
CREATE TABLE organization_membership (
    id SERIAL PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    user_uuid UUID NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_follower (
    id SERIAL PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE organization_invitation (
    id SERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    invited_by_user_uuid UUID NOT NULL,
    role_id INT REFERENCES role(id),
    created_at TIMESTAMP NOT NULL
);

-- Project
CREATE TABLE project (
    uuid UUID PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    name VARCHAR NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(128) NOT NULL,
    location_text VARCHAR NOT NULL,
    return_on_investment VARCHAR(16) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expected_funding BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    min_per_user BIGINT NOT NULL,
    max_per_user BIGINT NOT NULL,
    main_image VARCHAR,
    gallery TEXT,
    news_links TEXT,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL
);

-- Document
CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    link VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR(16) NOT NULL,
    size INT NOT NULL,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE project_document(
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (project_uuid, document_id)
);
CREATE TABLE organization_document(
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    document_id INT REFERENCES document(id) NOT NULL,

    PRIMARY KEY (organization_uuid, document_id)
);
