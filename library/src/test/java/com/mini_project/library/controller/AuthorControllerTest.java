package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.AuthorRequest;
import com.mini_project.library.dto.response.AuthorResponse;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.AuthorService;
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

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean AuthorService authorService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private AuthorResponse sampleAuthorResponse() {
        return AuthorResponse.builder()
            .id(1L).name("Robert C. Martin")
            .biography("Author of Clean Code")
            .nationality("American")
            .build();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/authors - returns paginated list of authors")
    void getAllAuthors_returnsPage() throws Exception {
        given(authorService.getAllAuthors(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(sampleAuthorResponse())));

        mockMvc.perform(get("/api/authors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].name").value("Robert C. Martin"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/authors/{id} - returns author when found")
    void getAuthorById_returnsAuthor_whenFound() throws Exception {
        given(authorService.getAuthorById(1L)).willReturn(sampleAuthorResponse());

        mockMvc.perform(get("/api/authors/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.nationality").value("American"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/authors/{id} - returns 404 when author does not exist")
    void getAuthorById_returns404_whenNotFound() throws Exception {
        given(authorService.getAuthorById(99L))
            .willThrow(new ResourceNotFoundException("Author", "id", 99L));

        mockMvc.perform(get("/api/authors/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/authors/search - returns matching authors by name")
    void searchAuthors_returnsMatches() throws Exception {
        given(authorService.searchAuthors("martin")).willReturn(List.of(sampleAuthorResponse()));

        mockMvc.perform(get("/api/authors/search").param("name", "martin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Robert C. Martin"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/authors - creates author with valid request")
    void createAuthor_returnsCreated_whenRequestIsValid() throws Exception {
        AuthorRequest request = new AuthorRequest();
        request.setName("Robert C. Martin");
        request.setBiography("Author of Clean Code");
        request.setNationality("American");

        given(authorService.createAuthor(any(AuthorRequest.class))).willReturn(sampleAuthorResponse());

        mockMvc.perform(post("/api/authors")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Robert C. Martin"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/authors - returns 400 when name is blank")
    void createAuthor_returns400_whenNameIsBlank() throws Exception {
        AuthorRequest request = new AuthorRequest();

        mockMvc.perform(post("/api/authors")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/authors - returns 409 when author name already exists")
    void createAuthor_returns409_whenNameIsDuplicate() throws Exception {
        AuthorRequest request = new AuthorRequest();
        request.setName("Robert C. Martin");

        given(authorService.createAuthor(any(AuthorRequest.class)))
            .willThrow(new DuplicateResourceException("Author", "name", "Robert C. Martin"));

        mockMvc.perform(post("/api/authors")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("POST /api/authors - returns 403 for MEMBER role")
    void createAuthor_returns403_forMember() throws Exception {
        AuthorRequest request = new AuthorRequest();
        request.setName("Someone");

        mockMvc.perform(post("/api/authors")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PUT /api/authors/{id} - updates author with valid request")
    void updateAuthor_returnsUpdatedAuthor() throws Exception {
        AuthorRequest request = new AuthorRequest();
        request.setName("Uncle Bob");
        request.setBiography("Updated bio");
        request.setNationality("British");

        AuthorResponse updated = AuthorResponse.builder()
            .id(1L).name("Uncle Bob").biography("Updated bio").nationality("British").build();

        given(authorService.updateAuthor(eq(1L), any(AuthorRequest.class))).willReturn(updated);

        mockMvc.perform(put("/api/authors/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Uncle Bob"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("DELETE /api/authors/{id} - deletes author successfully")
    void deleteAuthor_returnsOk_whenAuthorHasNoBooks() throws Exception {
        willDoNothing().given(authorService).deleteAuthor(1L);

        mockMvc.perform(delete("/api/authors/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("DELETE /api/authors/{id} - returns 403 for MEMBER role")
    void deleteAuthor_returns403_forMember() throws Exception {
        mockMvc.perform(delete("/api/authors/1").with(csrf()))
            .andExpect(status().isForbidden());
    }
}
