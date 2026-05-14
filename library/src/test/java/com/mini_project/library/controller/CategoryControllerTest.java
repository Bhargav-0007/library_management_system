package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.CategoryRequest;
import com.mini_project.library.dto.response.CategoryResponse;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean CategoryService categoryService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private CategoryResponse sampleCategoryResponse() {
        return CategoryResponse.builder()
            .id(1L).name("Science Fiction").description("Sci-fi genre books").build();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/categories - returns paginated list of categories")
    void getAllCategories_returnsPage() throws Exception {
        given(categoryService.getAllCategories(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(sampleCategoryResponse())));

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].name").value("Science Fiction"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/categories/{id} - returns category when found")
    void getCategoryById_returnsCategory_whenFound() throws Exception {
        given(categoryService.getCategoryById(1L)).willReturn(sampleCategoryResponse());

        mockMvc.perform(get("/api/categories/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.description").value("Sci-fi genre books"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/categories/{id} - returns 404 when category does not exist")
    void getCategoryById_returns404_whenNotFound() throws Exception {
        given(categoryService.getCategoryById(99L))
            .willThrow(new ResourceNotFoundException("Category", "id", 99L));

        mockMvc.perform(get("/api/categories/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/categories - creates category with valid request")
    void createCategory_returnsCreated_whenRequestIsValid() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Science Fiction");
        request.setDescription("Sci-fi genre books");

        given(categoryService.createCategory(any(CategoryRequest.class)))
            .willReturn(sampleCategoryResponse());

        mockMvc.perform(post("/api/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Science Fiction"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/categories - returns 400 when name is blank")
    void createCategory_returns400_whenNameIsBlank() throws Exception {
        CategoryRequest request = new CategoryRequest();

        mockMvc.perform(post("/api/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("POST /api/categories - returns 409 when category name already exists")
    void createCategory_returns409_whenNameIsDuplicate() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Science Fiction");

        given(categoryService.createCategory(any(CategoryRequest.class)))
            .willThrow(new DuplicateResourceException("Category", "name", "Science Fiction"));

        mockMvc.perform(post("/api/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("POST /api/categories - returns 403 for MEMBER role")
    void createCategory_returns403_forMember() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Fantasy");

        mockMvc.perform(post("/api/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PUT /api/categories/{id} - updates category with valid request")
    void updateCategory_returnsUpdatedCategory() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Fantasy");
        request.setDescription("Fantasy genre books");

        CategoryResponse updated = CategoryResponse.builder()
            .id(1L).name("Fantasy").description("Fantasy genre books").build();

        given(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).willReturn(updated);

        mockMvc.perform(put("/api/categories/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Fantasy"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("DELETE /api/categories/{id} - deletes category successfully")
    void deleteCategory_returnsOk_whenCategoryExists() throws Exception {
        willDoNothing().given(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("DELETE /api/categories/{id} - returns 404 when category does not exist")
    void deleteCategory_returns404_whenNotFound() throws Exception {
        willThrow(new ResourceNotFoundException("Category", "id", 99L))
            .given(categoryService).deleteCategory(99L);

        mockMvc.perform(delete("/api/categories/99").with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("DELETE /api/categories/{id} - returns 403 for MEMBER role")
    void deleteCategory_returns403_forMember() throws Exception {
        mockMvc.perform(delete("/api/categories/1").with(csrf()))
            .andExpect(status().isForbidden());
    }
}
