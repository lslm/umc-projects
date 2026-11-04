package dev.lslm.library.controller;

import dev.lslm.library.domain.Book;
import dev.lslm.library.dto.BookRequest;
import dev.lslm.library.dto.BookResponse;
import dev.lslm.library.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/livros")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> listar(@RequestParam(defaultValue = "false") boolean incluirArquivados) {
        return bookService.findAll(incluirArquivados).stream()
                .map(BookResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public BookResponse buscarPorId(@PathVariable UUID id) {
        return BookResponse.from(bookService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookResponse> criar(@RequestBody BookRequest request) {
        Book book = bookService.create(request);
        BookResponse response = BookResponse.from(book);
        return ResponseEntity.created(URI.create("/api/v1/livros/" + book.getId())).body(response);
    }

    @PutMapping("/{id}")
    public BookResponse atualizar(@PathVariable UUID id, @RequestBody BookRequest request) {
        Book book = bookService.update(id, request);
        return BookResponse.from(book);
    }

    @PostMapping("/{id}/arquivamento")
    public BookResponse arquivar(@PathVariable UUID id) {
        return BookResponse.from(bookService.arquivar(id, true));
    }

    @PostMapping("/{id}/desarquivamento")
    public BookResponse desarquivar(@PathVariable UUID id) {
        return BookResponse.from(bookService.arquivar(id, false));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
