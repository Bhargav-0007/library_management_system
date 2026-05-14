package com.mini_project.library.service;

import com.mini_project.library.dto.request.AuthorRequest;
import com.mini_project.library.dto.response.AuthorResponse;
import com.mini_project.library.entity.Author;
import com.mini_project.library.entity.Book;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.AuthorRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock AuthorRepository authorRepository;

    @InjectMocks AuthorService authorService;

    private Author sampleAuthor;
    private AuthorRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleAuthor = Author.builder()
            .id(1L).name("Robert C. Martin")
            .biography("Software engineer and author known for Clean Code")
            .nationality("American")
            .books(new HashSet<>())
            .build();

        sampleRequest = new AuthorRequest();
        sampleRequest.setName("Robert C. Martin");
        sampleRequest.setBiography("Software engineer and author known for Clean Code");
        sampleRequest.setNationality("American");
    }

    @Nested
    @DisplayName("getAllAuthors")
    class GetAllAuthors {

        @Test
        @DisplayName("returns paginated list of authors")
        void returnsPage_ofAllAuthors() {
            Pageable pageable = PageRequest.of(0, 20);
            given(authorRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(sampleAuthor)));

            var result = authorService.getAllAuthors(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Robert C. Martin");
        }
    }

    @Nested
    @DisplayName("getAuthorById")
    class GetAuthorById {

        @Test
        @DisplayName("returns AuthorResponse when author exists")
        void returnsAuthor_whenFound() {
            given(authorRepository.findById(1L)).willReturn(Optional.of(sampleAuthor));

            AuthorResponse result = authorService.getAuthorById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Robert C. Martin");
            assertThat(result.getNationality()).isEqualTo("American");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when author does not exist")
        void throwsNotFound_whenMissing() {
            given(authorRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authorService.getAuthorById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
        }
    }

    @Nested
    @DisplayName("searchAuthors")
    class SearchAuthors {

        @Test
        @DisplayName("returns matching authors in case-insensitive search")
        void returnsMatchingAuthors() {
            given(authorRepository.findByNameContainingIgnoreCase("martin"))
                .willReturn(List.of(sampleAuthor));

            List<AuthorResponse> results = authorService.searchAuthors("martin");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Robert C. Martin");
        }

        @Test
        @DisplayName("returns empty list when no author name matches")
        void returnsEmptyList_whenNoMatch() {
            given(authorRepository.findByNameContainingIgnoreCase("xyz")).willReturn(List.of());

            List<AuthorResponse> results = authorService.searchAuthors("xyz");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("createAuthor")
    class CreateAuthor {

        @Test
        @DisplayName("saves and returns new author when name is unique")
        void createsAuthor_whenNameIsUnique() {
            given(authorRepository.existsByNameIgnoreCase("Robert C. Martin")).willReturn(false);
            given(authorRepository.save(any(Author.class))).willReturn(sampleAuthor);

            AuthorResponse result = authorService.createAuthor(sampleRequest);

            assertThat(result.getName()).isEqualTo("Robert C. Martin");
            assertThat(result.getBiography()).isEqualTo("Software engineer and author known for Clean Code");
            then(authorRepository).should().save(any(Author.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when author name already exists")
        void throwsDuplicate_whenNameAlreadyExists() {
            given(authorRepository.existsByNameIgnoreCase("Robert C. Martin")).willReturn(true);

            assertThatThrownBy(() -> authorService.createAuthor(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Author");

            then(authorRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateAuthor")
    class UpdateAuthor {

        @Test
        @DisplayName("updates all fields when author is found")
        void updatesAuthor_whenFound() {
            given(authorRepository.findById(1L)).willReturn(Optional.of(sampleAuthor));
            given(authorRepository.save(sampleAuthor)).willReturn(sampleAuthor);

            AuthorRequest updateRequest = new AuthorRequest();
            updateRequest.setName("Uncle Bob");
            updateRequest.setBiography("Updated biography");
            updateRequest.setNationality("British");

            authorService.updateAuthor(1L, updateRequest);

            assertThat(sampleAuthor.getName()).isEqualTo("Uncle Bob");
            assertThat(sampleAuthor.getBiography()).isEqualTo("Updated biography");
            assertThat(sampleAuthor.getNationality()).isEqualTo("British");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when author does not exist")
        void throwsNotFound_whenMissing() {
            given(authorRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authorService.updateAuthor(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author");
        }
    }

    @Nested
    @DisplayName("deleteAuthor")
    class DeleteAuthor {

        @Test
        @DisplayName("deletes author when no books are associated")
        void deletesAuthor_whenNoBooksAssociated() {
            given(authorRepository.findById(1L)).willReturn(Optional.of(sampleAuthor));
            willDoNothing().given(authorRepository).delete(sampleAuthor);

            assertThatCode(() -> authorService.deleteAuthor(1L)).doesNotThrowAnyException();
            then(authorRepository).should().delete(sampleAuthor);
        }

        @Test
        @DisplayName("throws IllegalStateException when author has associated books")
        void throwsIllegalState_whenAuthorHasBooks() {
            sampleAuthor.getBooks().add(Book.builder().id(10L).title("Clean Code").build());
            given(authorRepository.findById(1L)).willReturn(Optional.of(sampleAuthor));

            assertThatThrownBy(() -> authorService.deleteAuthor(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete author");

            then(authorRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when author does not exist")
        void throwsNotFound_whenMissing() {
            given(authorRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authorService.deleteAuthor(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
