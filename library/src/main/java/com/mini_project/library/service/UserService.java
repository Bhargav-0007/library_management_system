package com.mini_project.library.service;

import com.mini_project.library.dto.request.UpdateUserRequest;
import com.mini_project.library.dto.response.UserResponse;
import com.mini_project.library.entity.Role;
import com.mini_project.library.entity.User;
import com.mini_project.library.entity.enums.ERole;
import com.mini_project.library.exception.BadRequestException;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.RoleRepository;
import com.mini_project.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUserById(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = findUserById(id);
        if (!user.isActive()) {
            throw new BadRequestException("User is already deactivated");
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getUsername());
    }

    @Transactional
    public void activateUser(Long id) {
        User user = findUserById(id);
        if (user.isActive()) {
            throw new BadRequestException("User is already active");
        }
        user.setActive(true);
        userRepository.save(user);
        log.info("Activated user: {}", user.getUsername());
    }

    @Transactional
    public UserResponse assignRole(Long userId, String roleName) {
        User user = findUserById(userId);
        ERole eRole = parseRole(roleName);
        Role role = roleRepository.findByName(eRole)
            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
        user.getRoles().add(role);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse removeRole(Long userId, String roleName) {
        User user = findUserById(userId);
        ERole eRole = parseRole(roleName);
        user.getRoles().removeIf(r -> r.getName() == eRole);
        return toResponse(userRepository.save(user));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private ERole parseRole(String roleName) {
        try {
            return ERole.valueOf(roleName.toUpperCase().startsWith("ROLE_")
                ? roleName.toUpperCase()
                : "ROLE_" + roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + roleName);
        }
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .active(user.isActive())
            .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
            .createdAt(user.getCreatedAt())
            .build();
    }
}
