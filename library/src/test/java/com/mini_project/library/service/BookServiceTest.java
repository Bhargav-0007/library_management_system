package com.mini_project.library.service;

import com.mini_project.library.dto.request.BookRequest;
import com.mini_project.library.dto.response.BookResponse;
import com.mini_project.library.entity.Book;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.AuthorRepository;
import com.mini_project.library.repository.BookRepository;
import com.mini_project.library.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock AuthorRepository authorRepository;
    @Mock CategoryRepository categoryRepository;

    @InjectMocks BookService bookService;

    private Book sampleBook;
    private BookRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleBook = Book.builder()
            .id(1L).title("Clean Code").isbn("978-0132350884")
            .totalCopies(3).availableCopies(3).build();

        sampleRequest = new BookRequest();
        sampleRequest.setTitle("Clean Code");
        sampleRequest.setIsbn("978-0132350884");
        sampleRequest.setTotalCopies(3);
    }

    @Nested
    @DisplayName("getBookById")
    class GetBookById {

        @Test
        void returnsBook_whenFound() {
            given(bookRepository.findById(1L)).willReturn(Optional.of(sampleBook));

            BookResponse result = bookService.getBookById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Clean Code");
        }

        @Test
        void throwsNotFound_whenMissing() {
            given(bookRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
        }
    }

    @Nested
    @DisplayName("createBook")
    class CreateBook {

        @Test
        void createsBook_whenIsbnIsUnique() {
            given(bookRepository.existsByIsbn("978-0132350884")).willReturn(false);
            given(bookRepository.save(any(Book.class))).willReturn(sampleBook);

            BookResponse result = bookService.createBook(sampleRequest);

            assertThat(result.getTitle()).isEqualTo("Clean Code");
            then(bookRepository).should().save(any(Book.class));
        }

        @Test
        void throwsDuplicate_whenIsbnAlreadyExists() {
            given(bookRepository.existsByIsbn("978-0132350884")).willReturn(true);

            assertThatThrownBy(() -> bookService.createBook(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ISBN");
        }
    }

    @Nested
    @DisplayName("deleteBook")
    class DeleteBook {

        @Test
        void deletesBook_whenFound() {
            given(bookRepository.findById(1L)).willReturn(Optional.of(sampleBook));
            willDoNothing().given(bookRepository).delete(sampleBook);

            assertThatCode(() -> bookService.deleteBook(1L)).doesNotThrowAnyException();
            then(bookRepository).should().delete(sampleBook);
        }

        @Test
        void throwsNotFound_whenMissing() {
            given(bookRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
