package com.mini_project.library.controller;

import com.mini_project.library.dto.request.AuthorRequest;
import com.mini_project.library.dto.response.ApiResponse;
import com.mini_project.library.dto.response.AuthorResponse;
import com.mini_project.library.service.AuthorService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Authors", description = "Author management")
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    @Operation(summary = "List all authors (paginated)")
    public ResponseEntity<ApiResponse<Page<AuthorResponse>>> getAllAuthors(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(authorService.getAllAuthors(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(authorService.getAuthorById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search authors by name")
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> searchAuthors(@RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.success(authorService.searchAuthors(name)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Create a new author")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@RequestBody @Valid AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Author created", authorService.createAuthor(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Update author details")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long id, @RequestBody @Valid AuthorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Author updated", authorService.updateAuthor(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Delete an author")
    public ResponseEntity<ApiResponse<Void>> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success("Author deleted", null));
    }
}
