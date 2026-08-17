package dev.lslm.library.dto;

import dev.lslm.library.domain.Book;

import java.time.LocalDate;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String description,
        String author,
        String isbn,
        String publisher,
        Integer edition,
        LocalDate publishedAt,
        String language,
        Integer pages
) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublisher(),
                book.getEdition(),
                book.getPublishedAt(),
                book.getLanguage(),
                book.getPages()
        );
    }
}
