package dev.lslm.library.exception;

import java.util.UUID;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(UUID id) {
        super("Livro não encontrado com o id: " + id);
    }
}
