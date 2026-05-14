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

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
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

    @Nested
    @DisplayName("getBookByIsbn")
    class GetBookByIsbn {

        @Test
        void returnsBook_whenFound() {
            given(bookRepository.findByIsbn("978-0132350884")).willReturn(Optional.of(sampleBook));

            BookResponse result = bookService.getBookByIsbn("978-0132350884");

            assertThat(result.getIsbn()).isEqualTo("978-0132350884");
            assertThat(result.getTitle()).isEqualTo("Clean Code");
        }

        @Test
        void throwsNotFound_whenIsbnMissing() {
            given(bookRepository.findByIsbn("000-0000000000")).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookByIsbn("000-0000000000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
        }
    }

    @Nested
    @DisplayName("updateBook")
    class UpdateBook {

        @Test
        void updatesBook_whenIsbnUnchanged() {
            given(bookRepository.findById(1L)).willReturn(Optional.of(sampleBook));
            given(bookRepository.save(any(Book.class))).willReturn(sampleBook);

            BookResponse result = bookService.updateBook(1L, sampleRequest);

            assertThat(result.getTitle()).isEqualTo("Clean Code");
            then(bookRepository).should().save(any(Book.class));
            then(bookRepository).should(never()).existsByIsbn(any());
        }

        @Test
        void updatesBook_whenNewIsbnIsUnique() {
            BookRequest updateRequest = new BookRequest();
            updateRequest.setTitle("Clean Code");
            updateRequest.setIsbn("978-9999999999");
            updateRequest.setTotalCopies(3);

            given(bookRepository.findById(1L)).willReturn(Optional.of(sampleBook));
            given(bookRepository.existsByIsbn("978-9999999999")).willReturn(false);
            given(bookRepository.save(any(Book.class))).willReturn(sampleBook);

            assertThatCode(() -> bookService.updateBook(1L, updateRequest)).doesNotThrowAnyException();
        }

        @Test
        void throwsDuplicate_whenNewIsbnAlreadyTaken() {
            BookRequest updateRequest = new BookRequest();
            updateRequest.setTitle("Clean Code");
            updateRequest.setIsbn("978-9999999999");
            updateRequest.setTotalCopies(3);

            given(bookRepository.findById(1L)).willReturn(Optional.of(sampleBook));
            given(bookRepository.existsByIsbn("978-9999999999")).willReturn(true);

            assertThatThrownBy(() -> bookService.updateBook(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ISBN");
        }

        @Test
        void throwsNotFound_whenMissing() {
            given(bookRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.updateBook(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book");
        }
    }

    @Nested
    @DisplayName("searchBooks")
    class SearchBooks {

        @Test
        void returnsPageOfBooks() {
            Pageable pageable = PageRequest.of(0, 20);
            given(bookRepository.searchBooks(null, null, null, null, false, pageable))
                .willReturn(new PageImpl<>(List.of(sampleBook)));

            var result = bookService.searchBooks(null, null, null, null, false, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        }

        @Test
        void returnsEmptyPage_whenNoMatch() {
            Pageable pageable = PageRequest.of(0, 20);
            given(bookRepository.searchBooks("xyz", null, null, null, false, pageable))
                .willReturn(new PageImpl<>(List.of()));

            var result = bookService.searchBooks("xyz", null, null, null, false, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }
}
