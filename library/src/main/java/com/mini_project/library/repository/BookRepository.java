package com.mini_project.library.repository;

import com.mini_project.library.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("""
        SELECT DISTINCT b FROM Book b
        LEFT JOIN b.authors a
        LEFT JOIN b.categories c
        WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:author IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:category IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :category, '%')))
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:availableOnly = false OR b.availableCopies > 0)
        """)
    Page<Book> searchBooks(
        @Param("title") String title,
        @Param("author") String author,
        @Param("category") String category,
        @Param("isbn") String isbn,
        @Param("availableOnly") boolean availableOnly,
        Pageable pageable
    );
}
