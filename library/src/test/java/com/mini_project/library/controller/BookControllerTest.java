package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.BookRequest;
import com.mini_project.library.dto.response.BookResponse;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean BookService bookService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private BookResponse sampleBookResponse() {
        return BookResponse.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(3).availableCopies(3).available(true)
            .build();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/books - returns paginated books")
    void searchBooks_returnsPage() throws Exception {
        given(bookService.searchBooks(any(), any(), any(), any(), anyBoolean(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(sampleBookResponse())));

        mockMvc.perform(get("/api/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/books/{id} - returns book when found")
    void getBookById_found() throws Exception {
        given(bookService.getBookById(1L)).willReturn(sampleBookResponse());

        mockMvc.perform(get("/api/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isbn").value("978-0132350884"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/books/{id} - returns 404 when not found")
    void getBookById_notFound() throws Exception {
        given(bookService.getBookById(99L)).willThrow(new ResourceNotFoundException("Book", "id", 99L));

        mockMvc.perform(get("/api/books/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/books - creates book with valid request")
    void createBook_success() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Clean Code");
        request.setIsbn("978-0132350884");
        request.setTotalCopies(3);

        given(bookService.createBook(any(BookRequest.class))).willReturn(sampleBookResponse());

        mockMvc.perform(post("/api/books")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/books - returns 400 when title is blank")
    void createBook_validationFailure() throws Exception {
        BookRequest request = new BookRequest();
        request.setIsbn("978-0132350884");
        // title intentionally missing

        mockMvc.perform(post("/api/books")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("DELETE /api/books/{id} - returns 403 for MEMBER role")
    void deleteBook_forbiddenForMember() throws Exception {
        mockMvc.perform(delete("/api/books/1").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/books/isbn/{isbn} - returns book when found")
    void getBookByIsbn_found() throws Exception {
        given(bookService.getBookByIsbn("978-0132350884")).willReturn(sampleBookResponse());

        mockMvc.perform(get("/api/books/isbn/978-0132350884"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isbn").value("978-0132350884"))
            .andExpect(jsonPath("$.data.title").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/books/isbn/{isbn} - returns 404 when ISBN not found")
    void getBookByIsbn_notFound() throws Exception {
        given(bookService.getBookByIsbn("000-0000000000"))
            .willThrow(new ResourceNotFoundException("Book", "ISBN", "000-0000000000"));

        mockMvc.perform(get("/api/books/isbn/000-0000000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PUT /api/books/{id} - updates book with valid request")
    void updateBook_success() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Clean Code");
        request.setIsbn("978-0132350884");
        request.setTotalCopies(5);

        BookResponse updated = BookResponse.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(5).availableCopies(5).available(true)
            .build();
        given(bookService.updateBook(eq(1L), any(BookRequest.class))).willReturn(updated);

        mockMvc.perform(put("/api/books/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalCopies").value(5));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PUT /api/books/{id} - returns 400 when title is blank")
    void updateBook_validationFailure() throws Exception {
        BookRequest request = new BookRequest();
        request.setIsbn("978-0132350884");
        request.setTotalCopies(3);

        mockMvc.perform(put("/api/books/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("PUT /api/books/{id} - returns 403 for MEMBER role")
    void updateBook_forbiddenForMember() throws Exception {
        BookRequest request = new BookRequest();
        request.setTitle("Clean Code");
        request.setIsbn("978-0132350884");
        request.setTotalCopies(3);

        mockMvc.perform(put("/api/books/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }
}
