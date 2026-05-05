-- ============================================================
-- V1: Initial schema for Library Management System
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    full_name    VARCHAR(100),
    phone_number VARCHAR(20),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS authors (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    biography   TEXT,
    nationality VARCHAR(60)
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS books (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    isbn             VARCHAR(20)  UNIQUE,
    description      TEXT,
    publisher        VARCHAR(120),
    published_year   INT,
    language         VARCHAR(50),
    total_copies     INT          NOT NULL DEFAULT 1,
    available_copies INT          NOT NULL DEFAULT 1,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id   BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id)   REFERENCES books(id)   ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS book_categories (
    book_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, category_id),
    FOREIGN KEY (book_id)     REFERENCES books(id)      ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS loans (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id     BIGINT         NOT NULL,
    user_id     BIGINT         NOT NULL,
    issued_by   BIGINT,
    issue_date  DATE           NOT NULL,
    due_date    DATE           NOT NULL,
    return_date DATE,
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    fine_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    fine_paid   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id)   REFERENCES books(id),
    FOREIGN KEY (user_id)   REFERENCES users(id),
    FOREIGN KEY (issued_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_loans_user_id    ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_book_id    ON loans(book_id);
CREATE INDEX IF NOT EXISTS idx_loans_status     ON loans(status);
CREATE INDEX IF NOT EXISTS idx_loans_due_date   ON loans(due_date);
CREATE INDEX IF NOT EXISTS idx_books_isbn       ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_users_username   ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email      ON users(email);
