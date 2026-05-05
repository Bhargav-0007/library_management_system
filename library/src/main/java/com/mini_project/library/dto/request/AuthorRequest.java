package com.mini_project.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthorRequest {

    @NotBlank(message = "Author name is required")
    @Size(max = 100, message = "Author name must not exceed 100 characters")
    private String name;

    @Size(max = 5000, message = "Biography must not exceed 5000 characters")
    private String biography;

    @Size(max = 60, message = "Nationality must not exceed 60 characters")
    private String nationality;
}
