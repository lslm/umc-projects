package dev.lslm.library.dto;

import java.time.LocalDate;
import java.util.UUID;

public record BookRequest(
        String title,
        String description,
        String author,
        String isbn,
        String publisher,
        Integer edition,
        LocalDate publishedAt,
        String language,
        Integer pages,
        UUID authorId,
        UUID publisherId,
        UUID categoryId
) {

    /** Forma simples (sem vinculo com autor/editora/categoria cadastrados). */
    public BookRequest(String title, String description, String author, String isbn, String publisher,
                       Integer edition, LocalDate publishedAt, String language, Integer pages) {
        this(title, description, author, isbn, publisher, edition, publishedAt, language, pages, null, null, null);
    }
}
