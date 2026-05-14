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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;

    @InjectMocks UserService userService;

    private Role memberRole;
    private Role librarianRole;
    private Role adminRole;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        memberRole = new Role(ERole.ROLE_MEMBER);
        memberRole.setId(1L);

        librarianRole = new Role(ERole.ROLE_LIBRARIAN);
        librarianRole.setId(2L);

        adminRole = new Role(ERole.ROLE_ADMIN);
        adminRole.setId(3L);

        activeUser = User.builder()
            .id(1L).username("alice").email("alice@example.com")
            .fullName("Alice Smith").phoneNumber("555-1234")
            .active(true).roles(new HashSet<>(Set.of(memberRole))).build();

        inactiveUser = User.builder()
            .id(2L).username("bob").email("bob@example.com")
            .fullName("Bob Jones").active(false)
            .roles(new HashSet<>(Set.of(memberRole))).build();
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("returns paginated list of all users")
        void returnsPage_ofAllUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            given(userRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(activeUser, inactiveUser)));

            var result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns UserResponse when user exists")
        void returnsUser_whenFound() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            UserResponse result = userService.getUserById(1L);

            assertThat(result.getUsername()).isEqualTo("alice");
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getRoles()).contains("ROLE_MEMBER");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user does not exist")
        void throwsNotFound_whenMissing() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("updates email and fullName when new email is unique")
        void updatesFields_whenEmailIsUnique() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("newalice@example.com");
            request.setFullName("Alice Updated");

            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.existsByEmail("newalice@example.com")).willReturn(false);
            given(userRepository.save(activeUser)).willReturn(activeUser);

            UserResponse result = userService.updateUser(1L, request);

            assertThat(activeUser.getEmail()).isEqualTo("newalice@example.com");
            assertThat(activeUser.getFullName()).isEqualTo("Alice Updated");
        }

        @Test
        @DisplayName("throws DuplicateResourceException when new email is taken by another user")
        void throwsDuplicate_whenEmailTakenByAnother() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("taken@example.com");

            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.existsByEmail("taken@example.com")).willReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
        }

        @Test
        @DisplayName("skips duplicate email check when email is unchanged")
        void skipsEmailCheck_whenEmailUnchanged() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("alice@example.com");
            request.setFullName("Alice Updated");

            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            assertThatCode(() -> userService.updateUser(1L, request)).doesNotThrowAnyException();
            then(userRepository).should(never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("only updates phoneNumber when only phoneNumber is provided")
        void updatesPhoneNumber_whenOnlyPhoneProvided() {
            UpdateUserRequest request = new UpdateUserRequest();
            request.setPhoneNumber("999-8888");

            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            userService.updateUser(1L, request);

            assertThat(activeUser.getPhoneNumber()).isEqualTo("999-8888");
            assertThat(activeUser.getFullName()).isEqualTo("Alice Smith");
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("sets active=false when user is currently active")
        void deactivatesUser_whenCurrentlyActive() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            assertThatCode(() -> userService.deactivateUser(1L)).doesNotThrowAnyException();

            assertThat(activeUser.isActive()).isFalse();
            then(userRepository).should().save(activeUser);
        }

        @Test
        @DisplayName("throws BadRequestException when user is already deactivated")
        void throwsBadRequest_whenAlreadyDeactivated() {
            given(userRepository.findById(2L)).willReturn(Optional.of(inactiveUser));

            assertThatThrownBy(() -> userService.deactivateUser(2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already deactivated");
        }
    }

    @Nested
    @DisplayName("activateUser")
    class ActivateUser {

        @Test
        @DisplayName("sets active=true when user is currently inactive")
        void activatesUser_whenCurrentlyInactive() {
            given(userRepository.findById(2L)).willReturn(Optional.of(inactiveUser));
            given(userRepository.save(inactiveUser)).willReturn(inactiveUser);

            assertThatCode(() -> userService.activateUser(2L)).doesNotThrowAnyException();

            assertThat(inactiveUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws BadRequestException when user is already active")
        void throwsBadRequest_whenAlreadyActive() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.activateUser(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already active");
        }
    }

    @Nested
    @DisplayName("assignRole")
    class AssignRole {

        @Test
        @DisplayName("adds role to user when role name is valid")
        void assignsRole_whenRoleIsValid() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(roleRepository.findByName(ERole.ROLE_LIBRARIAN)).willReturn(Optional.of(librarianRole));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            userService.assignRole(1L, "LIBRARIAN");

            assertThat(activeUser.getRoles()).contains(librarianRole);
        }

        @Test
        @DisplayName("accepts ROLE_ADMIN prefix format")
        void assignsRole_withPrefixFormat() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(roleRepository.findByName(ERole.ROLE_ADMIN)).willReturn(Optional.of(adminRole));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            userService.assignRole(1L, "ROLE_ADMIN");

            assertThat(activeUser.getRoles()).contains(adminRole);
        }

        @Test
        @DisplayName("throws BadRequestException when role name is invalid")
        void throwsBadRequest_whenRoleNameIsInvalid() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.assignRole(1L, "SUPERUSER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role");
        }
    }

    @Nested
    @DisplayName("removeRole")
    class RemoveRole {

        @Test
        @DisplayName("removes role from user when role is present")
        void removesRole_whenRoleIsPresent() {
            activeUser.getRoles().add(librarianRole);
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            userService.removeRole(1L, "LIBRARIAN");

            assertThat(activeUser.getRoles()).doesNotContain(librarianRole);
        }

        @Test
        @DisplayName("is a no-op when user does not have the role")
        void isNoOp_whenRoleNotPresent() {
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(userRepository.save(activeUser)).willReturn(activeUser);

            assertThatCode(() -> userService.removeRole(1L, "LIBRARIAN"))
                .doesNotThrowAnyException();
        }
    }
}
