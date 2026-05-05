package com.mini_project.library.controller;

import com.mini_project.library.dto.request.LoanRequest;
import com.mini_project.library.dto.response.ApiResponse;
import com.mini_project.library.dto.response.LoanResponse;
import com.mini_project.library.entity.enums.LoanStatus;
import com.mini_project.library.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Book borrowing and return management")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Issue a book to a member")
    public ResponseEntity<ApiResponse<LoanResponse>> issueLoan(
            @RequestBody @Valid LoanRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        LoanResponse loan = loanService.issueLoan(request, currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Book issued successfully", loan));
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Return a borrowed book")
    public ResponseEntity<ApiResponse<LoanResponse>> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Book returned successfully", loanService.returnBook(id)));
    }

    @PatchMapping("/{id}/pay-fine")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Mark overdue fine as paid")
    public ResponseEntity<ApiResponse<LoanResponse>> payFine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Fine marked as paid", loanService.payFine(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "List all loans (paginated)")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> getAllLoans(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getAllLoans(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','MEMBER')")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoanById(id)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','MEMBER')")
    @Operation(summary = "Get all loans for a specific user")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> getLoansByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoansByUser(userId, pageable)));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "List all overdue loans")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> getOverdueLoans(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getOverdueLoans(pageable)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Filter loans by status (ACTIVE, RETURNED, OVERDUE)")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> getLoansByStatus(
            @PathVariable LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoansByStatus(status, pageable)));
    }

    @PostMapping("/sync-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Mark all past-due active loans as OVERDUE")
    public ResponseEntity<ApiResponse<Void>> syncOverdue() {
        loanService.syncOverdueStatuses();
        return ResponseEntity.ok(ApiResponse.success("Overdue statuses synced", null));
    }
}
