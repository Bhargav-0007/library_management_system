package com.mini_project.library.service;

import com.mini_project.library.dto.request.BookRequest;
import com.mini_project.library.dto.response.AuthorResponse;
import com.mini_project.library.dto.response.BookResponse;
import com.mini_project.library.dto.response.CategoryResponse;
import com.mini_project.library.entity.Author;
import com.mini_project.library.entity.Book;
import com.mini_project.library.entity.Category;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.AuthorRepository;
import com.mini_project.library.repository.BookRepository;
import com.mini_project.library.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(String title, String author, String category,
                                          String isbn, boolean availableOnly, Pageable pageable) {
        return bookRepository.searchBooks(title, author, category, isbn, availableOnly, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public BookResponse getBookByIsbn(String isbn) {
        return toResponse(bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new ResourceNotFoundException("Book", "ISBN", isbn)));
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("Book", "ISBN", request.getIsbn());
        }
        Book book = Book.builder()
            .title(request.getTitle())
            .isbn(request.getIsbn())
            .description(request.getDescription())
            .publisher(request.getPublisher())
            .publishedYear(request.getPublishedYear())
            .language(request.getLanguage())
            .totalCopies(request.getTotalCopies())
            .availableCopies(request.getTotalCopies())
            .authors(resolveAuthors(request.getAuthorIds()))
            .categories(resolveCategories(request.getCategoryIds()))
            .build();
        Book saved = bookRepository.save(book);
        log.info("Created book '{}' (ISBN: {})", saved.getTitle(), saved.getIsbn());
        return toResponse(saved);
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = findById(id);
        if (!book.getIsbn().equals(request.getIsbn()) && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("Book", "ISBN", request.getIsbn());
        }
        int copyDelta = request.getTotalCopies() - book.getTotalCopies();
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setDescription(request.getDescription());
        book.setPublisher(request.getPublisher());
        book.setPublishedYear(request.getPublishedYear());
        book.setLanguage(request.getLanguage());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + copyDelta));
        book.setAuthors(resolveAuthors(request.getAuthorIds()));
        book.setCategories(resolveCategories(request.getCategoryIds()));
        return toResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        bookRepository.delete(findById(id));
        log.info("Deleted book id={}", id);
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
    }

    private Set<Author> resolveAuthors(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return ids.stream()
            .map(id -> authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id)))
            .collect(Collectors.toSet());
    }

    private Set<Category> resolveCategories(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return ids.stream()
            .map(id -> categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id)))
            .collect(Collectors.toSet());
    }

    public BookResponse toResponse(Book book) {
        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .isbn(book.getIsbn())
            .description(book.getDescription())
            .publisher(book.getPublisher())
            .publishedYear(book.getPublishedYear())
            .language(book.getLanguage())
            .totalCopies(book.getTotalCopies())
            .availableCopies(book.getAvailableCopies())
            .available(book.isAvailable())
            .authors(book.getAuthors().stream()
                .map(a -> AuthorResponse.builder()
                    .id(a.getId()).name(a.getName())
                    .biography(a.getBiography()).nationality(a.getNationality()).build())
                .collect(Collectors.toSet()))
            .categories(book.getCategories().stream()
                .map(c -> CategoryResponse.builder()
                    .id(c.getId()).name(c.getName()).description(c.getDescription()).build())
                .collect(Collectors.toSet()))
            .createdAt(book.getCreatedAt())
            .build();
    }
}
