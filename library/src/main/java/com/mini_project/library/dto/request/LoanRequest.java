package com.mini_project.library.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoanRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "User ID is required")
    private Long userId;
}
