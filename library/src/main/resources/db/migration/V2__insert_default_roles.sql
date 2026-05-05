-- V2: Seed default roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN')     ON DUPLICATE KEY UPDATE name = name;
INSERT INTO roles (name) VALUES ('ROLE_LIBRARIAN') ON DUPLICATE KEY UPDATE name = name;
INSERT INTO roles (name) VALUES ('ROLE_MEMBER')    ON DUPLICATE KEY UPDATE name = name;
