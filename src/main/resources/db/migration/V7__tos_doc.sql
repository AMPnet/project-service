ALTER TABLE project DROP COLUMN terms_of_service;
ALTER TABLE project ADD COLUMN tos_doc_id INT REFERENCES document(id);
