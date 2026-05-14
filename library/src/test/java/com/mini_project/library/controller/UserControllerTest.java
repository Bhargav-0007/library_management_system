package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.UpdateUserRequest;
import com.mini_project.library.dto.response.UserResponse;
import com.mini_project.library.exception.BadRequestException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.UserService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean UserService userService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private UserResponse activeUserResponse() {
        return UserResponse.builder()
            .id(1L).username("alice").email("alice@example.com")
            .fullName("Alice Smith").phoneNumber("555-1234")
            .active(true).roles(Set.of("ROLE_MEMBER"))
            .build();
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/users - returns paginated list of users")
    void getAllUsers_returnsPage() throws Exception {
        given(userService.getAllUsers(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(activeUserResponse())));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    @DisplayName("GET /api/users - returns 403 for MEMBER role")
    void getAllUsers_returns403_forMember() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/users/{id} - returns user when found")
    void getUserById_returnsUser_whenFound() throws Exception {
        given(userService.getUserById(1L)).willReturn(activeUserResponse());

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("alice"))
            .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("GET /api/users/{id} - returns 404 when user does not exist")
    void getUserById_returns404_whenNotFound() throws Exception {
        given(userService.getUserById(99L))
            .willThrow(new ResourceNotFoundException("User", "id", 99L));

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PUT /api/users/{id} - updates user profile successfully")
    void updateUser_returnsUpdatedUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Alice Updated");
        request.setPhoneNumber("999-8888");

        UserResponse updatedResponse = UserResponse.builder()
            .id(1L).username("alice").email("alice@example.com")
            .fullName("Alice Updated").phoneNumber("999-8888")
            .active(true).roles(Set.of("ROLE_MEMBER"))
            .build();

        given(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).willReturn(updatedResponse);

        mockMvc.perform(put("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Alice Updated"))
            .andExpect(jsonPath("$.data.phoneNumber").value("999-8888"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/deactivate - deactivates an active user")
    void deactivateUser_returnsOk_whenUserIsActive() throws Exception {
        willDoNothing().given(userService).deactivateUser(1L);

        mockMvc.perform(patch("/api/users/1/deactivate").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PATCH /api/users/{id}/deactivate - returns 403 for LIBRARIAN role")
    void deactivateUser_returns403_forLibrarian() throws Exception {
        mockMvc.perform(patch("/api/users/1/deactivate").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/deactivate - returns 400 when user already deactivated")
    void deactivateUser_returns400_whenAlreadyDeactivated() throws Exception {
        willThrow(new BadRequestException("User is already deactivated"))
            .given(userService).deactivateUser(1L);

        mockMvc.perform(patch("/api/users/1/deactivate").with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/activate - activates an inactive user")
    void activateUser_returnsOk_whenUserIsInactive() throws Exception {
        willDoNothing().given(userService).activateUser(1L);

        mockMvc.perform(patch("/api/users/1/activate").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/roles/assign - assigns role to user")
    void assignRole_returnsUpdatedUser() throws Exception {
        given(userService.assignRole(1L, "LIBRARIAN")).willReturn(
            UserResponse.builder()
                .id(1L).username("alice").email("alice@example.com")
                .active(true).roles(Set.of("ROLE_MEMBER", "ROLE_LIBRARIAN"))
                .build()
        );

        mockMvc.perform(patch("/api/users/1/roles/assign")
                .with(csrf())
                .param("role", "LIBRARIAN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    @DisplayName("PATCH /api/users/{id}/roles/assign - returns 403 for LIBRARIAN role")
    void assignRole_returns403_forLibrarian() throws Exception {
        mockMvc.perform(patch("/api/users/1/roles/assign")
                .with(csrf())
                .param("role", "ADMIN"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/roles/remove - removes role from user")
    void removeRole_returnsUpdatedUser() throws Exception {
        given(userService.removeRole(1L, "LIBRARIAN")).willReturn(activeUserResponse());

        mockMvc.perform(patch("/api/users/1/roles/remove")
                .with(csrf())
                .param("role", "LIBRARIAN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
