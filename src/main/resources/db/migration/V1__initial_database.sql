-- Organization
CREATE TABLE organization (
    uuid UUID PRIMARY KEY,
    name VARCHAR NOT NULL UNIQUE,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    approved BOOLEAN NOT NULL,
    approved_by_user_uuid UUID,
    description VARCHAR,
    header_image VARCHAR,
    coop VARCHAR(64) NOT NULL
);
CREATE TABLE organization_membership (
    id SERIAL PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    user_uuid UUID NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uc_organization_membership_org_user UNIQUE(organization_uuid, user_uuid)
);
CREATE TABLE organization_follower (
    id SERIAL PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uc_organization_follower_org_user UNIQUE(organization_uuid, user_uuid)
);
CREATE TABLE organization_invitation (
    id SERIAL PRIMARY KEY,
    email VARCHAR NOT NULL,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    invited_by_user_uuid UUID NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uc_organization_invitation_email_org UNIQUE(email, organization_uuid)
);

-- Project
CREATE TABLE project (
    uuid UUID PRIMARY KEY,
    organization_uuid UUID REFERENCES organization(uuid) NOT NULL,
    name VARCHAR NOT NULL,
    description TEXT NOT NULL,
    location_lat FLOAT NOT NULL,
    location_long FLOAT NOT NULL,
    roi_from FLOAT NOT NULL,
    roi_to FLOAT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expected_funding BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    min_per_user BIGINT NOT NULL,
    max_per_user BIGINT NOT NULL,
    main_image VARCHAR,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL,
    coop VARCHAR(64) NOT NULL
);
CREATE TABLE project_tag (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    tag VARCHAR(128) NOT NULL
);
CREATE TABLE project_news (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    link VARCHAR NOT NULL
);
CREATE TABLE project_gallery (
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    image VARCHAR NOT NULL
);
CREATE TABLE project_update(
    id SERIAL PRIMARY KEY,
    project_uuid UUID REFERENCES project(uuid) NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    author VARCHAR NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Document
CREATE TABLE document (
    id SERIAL PRIMARY KEY,
    link VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR(16) NOT NULL,
    size INT NOT NULL,
    created_by_user_uuid UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    organization_uuid UUID,
    project_uuid UUID
);
