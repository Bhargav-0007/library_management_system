package com.mini_project.library.service;

import com.mini_project.library.dto.request.AuthorRequest;
import com.mini_project.library.dto.response.AuthorResponse;
import com.mini_project.library.entity.Author;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    @Transactional(readOnly = true)
    public Page<AuthorResponse> getAllAuthors(Pageable pageable) {
        return authorRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<AuthorResponse> searchAuthors(String name) {
        return authorRepository.findByNameContainingIgnoreCase(name)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        if (authorRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Author", "name", request.getName());
        }
        Author author = Author.builder()
            .name(request.getName())
            .biography(request.getBiography())
            .nationality(request.getNationality())
            .build();
        return toResponse(authorRepository.save(author));
    }

    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        Author author = findById(id);
        author.setName(request.getName());
        author.setBiography(request.getBiography());
        author.setNationality(request.getNationality());
        return toResponse(authorRepository.save(author));
    }

    @Transactional
    public void deleteAuthor(Long id) {
        Author author = findById(id);
        if (!author.getBooks().isEmpty()) {
            throw new IllegalStateException("Cannot delete author with associated books");
        }
        authorRepository.delete(author);
        log.info("Deleted author id={}", id);
    }

    public Author findById(Long id) {
        return authorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id));
    }

    public AuthorResponse toResponse(Author author) {
        return AuthorResponse.builder()
            .id(author.getId())
            .name(author.getName())
            .biography(author.getBiography())
            .nationality(author.getNationality())
            .build();
    }
}
