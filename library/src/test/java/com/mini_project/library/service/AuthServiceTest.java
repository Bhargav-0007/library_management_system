package com.mini_project.library.service;

import com.mini_project.library.dto.request.LoginRequest;
import com.mini_project.library.dto.request.RegisterRequest;
import com.mini_project.library.dto.response.AuthResponse;
import com.mini_project.library.entity.Role;
import com.mini_project.library.entity.User;
import com.mini_project.library.entity.enums.ERole;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.repository.RoleRepository;
import com.mini_project.library.repository.UserRepository;
import com.mini_project.library.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider jwtTokenProvider;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private Role memberRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        memberRole = new Role(ERole.ROLE_MEMBER);
        memberRole.setId(1L);

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("john");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("secret123");
        registerRequest.setFullName("John Doe");
        registerRequest.setPhoneNumber("1234567890");

        savedUser = User.builder()
            .id(1L).username("john").email("john@example.com")
            .fullName("John Doe").phoneNumber("1234567890")
            .active(true).roles(new HashSet<>(Set.of(memberRole))).build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns token and user info when username and email are unique")
        void registersUser_whenUsernameAndEmailAreUnique() {
            given(userRepository.existsByUsername("john")).willReturn(false);
            given(userRepository.existsByEmail("john@example.com")).willReturn(false);
            given(roleRepository.findByName(ERole.ROLE_MEMBER)).willReturn(Optional.of(memberRole));
            given(passwordEncoder.encode("secret123")).willReturn("hashed");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtTokenProvider.generateToken("john")).willReturn("jwt-token");

            AuthResponse result = authService.register(registerRequest);

            assertThat(result.getUsername()).isEqualTo("john");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getAccessToken()).isEqualTo("jwt-token");
            assertThat(result.getRoles()).contains("ROLE_MEMBER");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when username is already taken")
        void throwsDuplicate_whenUsernameAlreadyExists() {
            given(userRepository.existsByUsername("john")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateResourceException when email is already registered")
        void throwsDuplicate_whenEmailAlreadyExists() {
            given(userRepository.existsByUsername("john")).willReturn(false);
            given(userRepository.existsByEmail("john@example.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
        }

        @Test
        @DisplayName("throws IllegalStateException when ROLE_MEMBER not found in database")
        void throwsIllegalState_whenDefaultRoleNotFound() {
            given(userRepository.existsByUsername("john")).willReturn(false);
            given(userRepository.existsByEmail("john@example.com")).willReturn(false);
            given(roleRepository.findByName(ERole.ROLE_MEMBER)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MEMBER role not found");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUpLogin() {
            loginRequest = new LoginRequest();
            loginRequest.setUsername("john");
            loginRequest.setPassword("secret123");
        }

        @Test
        @DisplayName("returns JWT token when credentials are valid")
        void returnsToken_whenCredentialsAreValid() {
            Authentication auth = mock(Authentication.class);
            given(auth.getName()).willReturn("john");
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(auth);
            given(jwtTokenProvider.generateToken("john")).willReturn("jwt-token");
            given(userRepository.findByUsername("john")).willReturn(Optional.of(savedUser));

            AuthResponse result = authService.login(loginRequest);

            assertThat(result.getAccessToken()).isEqualTo("jwt-token");
            assertThat(result.getUsername()).isEqualTo("john");
            assertThat(result.getRoles()).contains("ROLE_MEMBER");
        }

        @Test
        @DisplayName("propagates BadCredentialsException when authentication fails")
        void throwsBadCredentials_whenAuthenticationFails() {
            given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        }
    }
}
