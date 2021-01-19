ALTER TABLE project DROP COLUMN terms_of_service;
ALTER TABLE document ADD COLUMN purpose_id INT NOT NULL DEFAULT 0;
