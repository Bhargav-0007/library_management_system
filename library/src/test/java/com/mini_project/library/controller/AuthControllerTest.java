package com.mini_project.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_project.library.dto.request.LoginRequest;
import com.mini_project.library.dto.request.RegisterRequest;
import com.mini_project.library.dto.response.AuthResponse;
import com.mini_project.library.security.CustomUserDetailsService;
import com.mini_project.library.security.JwtTokenProvider;
import com.mini_project.library.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean AuthService authService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
            .accessToken("jwt-token")
            .tokenType("Bearer")
            .userId(1L)
            .username("john")
            .email("john@example.com")
            .roles(Set.of("ROLE_MEMBER"))
            .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 201 with token on success")
    void register_returnsCreated_whenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        given(authService.register(any(RegisterRequest.class))).willReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("john"))
            .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when username is blank")
    void register_returns400_whenUsernameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when email format is invalid")
    void register_returns400_whenEmailIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("not-an-email");
        request.setPassword("secret123");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 400 when password is too short")
    void register_returns400_whenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("short");
        request.setFullName("John Doe");

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 200 with token on valid credentials")
    void login_returnsOk_whenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("secret123");

        given(authService.login(any(LoginRequest.class))).willReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/login - returns 401 when credentials are wrong")
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrongpass");

        given(authService.login(any(LoginRequest.class)))
            .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
