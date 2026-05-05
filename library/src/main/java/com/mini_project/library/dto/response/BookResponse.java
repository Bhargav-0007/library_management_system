package com.mini_project.library.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String isbn;
    private String description;
    private String publisher;
    private Integer publishedYear;
    private String language;
    private int totalCopies;
    private int availableCopies;
    private boolean available;
    private Set<AuthorResponse> authors;
    private Set<CategoryResponse> categories;
    private LocalDateTime createdAt;
}
