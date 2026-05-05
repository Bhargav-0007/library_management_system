package com.mini_project.library.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String publisher;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(length = 50)
    private String language;

    @Column(name = "total_copies", nullable = false)
    @Builder.Default
    private int totalCopies = 1;

    @Column(name = "available_copies", nullable = false)
    @Builder.Default
    private int availableCopies = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "book_categories",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void decrementAvailableCopies() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No copies available to borrow");
        }
        availableCopies--;
    }

    public void incrementAvailableCopies() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Available copies cannot exceed total copies");
        }
        availableCopies++;
    }
}
