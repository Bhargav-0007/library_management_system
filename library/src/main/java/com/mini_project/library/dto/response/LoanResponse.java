package com.mini_project.library.dto.response;

import com.mini_project.library.entity.enums.LoanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponse {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private Long userId;
    private String username;
    private String userFullName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private boolean overdue;
    private long daysOverdue;
    private BigDecimal fineAmount;
    private boolean finePaid;
    private LocalDateTime createdAt;
}
