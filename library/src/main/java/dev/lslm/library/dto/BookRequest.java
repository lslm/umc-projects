package dev.lslm.library.dto;

import java.time.LocalDate;

public record BookRequest(
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
}
