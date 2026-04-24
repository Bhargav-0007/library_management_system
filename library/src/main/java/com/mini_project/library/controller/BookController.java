package com.mini_project.library.controller;

import com.mini_project.library.dto.BookRequest;
import com.mini_project.library.entity.Book;
import com.mini_project.library.service.BookService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/test")
    public String testBook(@RequestBody Map<String, Object> body) {
        return "Received: " + body;
    }

    @PostMapping("/string-test")
    public String test(@RequestBody String body) {
        return body;
    }

    @PostMapping
    public Book addBook(@RequestBody @Valid BookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());

        return bookService.addBook(book);
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "Deleted";
    }
}