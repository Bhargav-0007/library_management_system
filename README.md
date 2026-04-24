# 📚 Library Management System API

A backend application built with Spring Boot for managing books, members, and borrowing operations in a library system.

## 🚀 Features
* Create and manage books
* Register library members
* Borrow and return books
* Prevent duplicate borrowing and returning
* Track active loans
* Detect overdue loans
* Input validation and clear error handling

---
## 🧠 Business Logic
* A book cannot be borrowed if it is already loaned out
* A book cannot be returned twice
* Overdue loans are identified based on due date
* Each loan is associated with one book and one member

---

## 🛠️ Tech Stack
* Java
* Spring Boot
* Spring Data JPA
* H2 Database (in-memory)
* REST API architecture

---
## 📦 API Endpoints

### Books
* `POST /books` → Create a book
* `GET /books` → List all books

### Members
* `POST /members` → Create a member
* `GET /members` → List all members

### Loans
* `POST /borrow` → Borrow a book
* `POST /return` → Return a book
* `GET /loans` → List all loans
* `GET /loans/overdue` → List overdue loans
---
## ▶️ How to Run
bash
./mvnw spring-boot:run


Then open:
http://localhost:8080

## 🔮 Future Improvements

* Add fine calculation for overdue returns
* Introduce DTO layer (API response models)
* Migrate to PostgreSQL
* Add Swagger/OpenAPI documentation
* Implement authentication and authorization

---

## 📌 Project Status

Actively being improved as part of backend development practice.

---

## 👤 Author
Bhargav
GitHub: https://github.com/Bhargav-0007
