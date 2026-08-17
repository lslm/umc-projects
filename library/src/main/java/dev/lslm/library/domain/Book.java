package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(length = 150)
    private String publisher;

    private Integer edition;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(length = 50)
    private String language;

    private Integer pages;

    protected Book() {
        // construtor exigido pelo JPA/Hibernate
    }

    public Book(String title, String description, String author, String isbn,
                String publisher, Integer edition, LocalDate publishedAt,
                String language, Integer pages) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.edition = edition;
        this.publishedAt = publishedAt;
        this.language = language;
        this.pages = pages;
    }

    public void update(String title, String description, String author, String isbn,
                        String publisher, Integer edition, LocalDate publishedAt,
                        String language, Integer pages) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.edition = edition;
        this.publishedAt = publishedAt;
        this.language = language;
        this.pages = pages;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getEdition() {
        return edition;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getPages() {
        return pages;
    }
}
