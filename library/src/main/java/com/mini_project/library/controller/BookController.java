package com.mini_project.library.controller;

import com.mini_project.library.dto.request.BookRequest;
import com.mini_project.library.dto.response.ApiResponse;
import com.mini_project.library.dto.response.BookResponse;
import com.mini_project.library.service.BookService;
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

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Books", description = "Book catalog management")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Search and filter books (paginated)")
    public ResponseEntity<ApiResponse<Page<BookResponse>>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
            bookService.searchBooks(title, author, category, isbn, availableOnly, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<ApiResponse<BookResponse>> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getBookById(id)));
    }

    @GetMapping("/isbn/{isbn}")
    @Operation(summary = "Get book by ISBN")
    public ResponseEntity<ApiResponse<BookResponse>> getBookByIsbn(@PathVariable String isbn) {
        return ResponseEntity.ok(ApiResponse.success(bookService.getBookByIsbn(isbn)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Add a new book to the catalog")
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody @Valid BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Book created", bookService.createBook(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Update book details")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(
            @PathVariable Long id, @RequestBody @Valid BookRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book updated", bookService.updateBook(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Remove a book from the catalog")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success("Book deleted", null));
    }
}
