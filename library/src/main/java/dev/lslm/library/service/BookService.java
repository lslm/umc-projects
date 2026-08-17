package dev.lslm.library.service;

import dev.lslm.library.domain.Book;
import dev.lslm.library.dto.BookRequest;
import dev.lslm.library.exception.BookNotFoundException;
import dev.lslm.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book create(BookRequest request) {
        bookRepository.findByIsbn(request.isbn()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Já existe um livro cadastrado com o ISBN " + request.isbn() + ".");
        });

        Book book = new Book(
                request.title(),
                request.description(),
                request.author(),
                request.isbn(),
                request.publisher(),
                request.edition(),
                request.publishedAt(),
                request.language(),
                request.pages()
        );

        return bookRepository.save(book);
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book update(UUID id, BookRequest request) {
        Book book = findById(id);
        book.update(
                request.title(),
                request.description(),
                request.author(),
                request.isbn(),
                request.publisher(),
                request.edition(),
                request.publishedAt(),
                request.language(),
                request.pages()
        );
        return bookRepository.save(book);
    }

    public void delete(UUID id) {
        Book book = findById(id);
        bookRepository.delete(book);
    }
}
