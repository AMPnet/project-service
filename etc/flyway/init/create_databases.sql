DROP DATABASE IF EXISTS project_service;
CREATE DATABASE project_service ENCODING 'UTF-8';

DROP DATABASE IF EXISTS project_service_test;
CREATE DATABASE project_service_test ENCODING 'UTF-8';

DROP USER IF EXISTS project_service;
CREATE USER project_service WITH PASSWORD 'password';

DROP USER IF EXISTS project_service_test;
CREATE USER project_service_test WITH PASSWORD 'password';
