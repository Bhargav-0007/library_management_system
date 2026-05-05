package com.mini_project.library.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class BookRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "ISBN is required")
    @Size(max = 20, message = "ISBN must not exceed 20 characters")
    private String isbn;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 120, message = "Publisher must not exceed 120 characters")
    private String publisher;

    @Min(value = 1000, message = "Published year must be after 1000")
    @Max(value = 2100, message = "Published year must be before 2100")
    private Integer publishedYear;

    @Size(max = 50)
    private String language;

    @Min(value = 1, message = "Total copies must be at least 1")
    @Max(value = 9999, message = "Total copies must not exceed 9999")
    private int totalCopies = 1;

    private Set<Long> authorIds;

    private Set<Long> categoryIds;
}
