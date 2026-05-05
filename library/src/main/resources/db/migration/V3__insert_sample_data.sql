-- V3: Sample data for development
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (username, email, password, full_name, active)
VALUES ('admin', 'admin@library.com',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyB9gBBMi',
        'System Administrator', TRUE)
ON DUPLICATE KEY UPDATE username = username;

-- Password: Librarian@123 (BCrypt hash)
INSERT INTO users (username, email, password, full_name, active)
VALUES ('librarian', 'librarian@library.com',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyB9gBBMi',
        'Head Librarian', TRUE)
ON DUPLICATE KEY UPDATE username = username;

-- Assign roles (admin gets ADMIN + LIBRARIAN, librarian gets LIBRARIAN)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name IN ('ROLE_ADMIN', 'ROLE_LIBRARIAN')
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'librarian' AND r.name = 'ROLE_LIBRARIAN'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Sample authors
INSERT INTO authors (name, nationality) VALUES ('George Orwell', 'British')
    ON DUPLICATE KEY UPDATE name = name;
INSERT INTO authors (name, nationality) VALUES ('J.K. Rowling', 'British')
    ON DUPLICATE KEY UPDATE name = name;
INSERT INTO authors (name, nationality) VALUES ('Robert C. Martin', 'American')
    ON DUPLICATE KEY UPDATE name = name;

-- Sample categories
INSERT INTO categories (name) VALUES ('Fiction')      ON DUPLICATE KEY UPDATE name = name;
INSERT INTO categories (name) VALUES ('Non-Fiction')  ON DUPLICATE KEY UPDATE name = name;
INSERT INTO categories (name) VALUES ('Technology')   ON DUPLICATE KEY UPDATE name = name;
INSERT INTO categories (name) VALUES ('Fantasy')      ON DUPLICATE KEY UPDATE name = name;
