ALTER TABLE organization ADD COLUMN header_image VARCHAR;
ALTER TABLE organization RENAME COLUMN legal_info TO description;
