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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role memberRole = roleRepository.findByName(ERole.ROLE_MEMBER)
            .orElseThrow(() -> new IllegalStateException("Default MEMBER role not found. Run database migrations."));

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .phoneNumber(request.getPhoneNumber())
            .roles(Set.of(memberRole))
            .active(true)
            .build();

        userRepository.save(user);
        log.info("Registered new member: {}", user.getUsername());

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication.getName());
        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow();

        log.info("User logged in: {}", user.getUsername());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

        return AuthResponse.builder()
            .accessToken(token)
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(roles)
            .build();
    }
}
