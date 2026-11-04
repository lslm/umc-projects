package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

/** Obra do acervo de uma biblioteca (ISBN unico por biblioteca). */
@Entity
@Table(name = "books", uniqueConstraints = @UniqueConstraint(columnNames = {"biblioteca_id", "isbn"}))
public class Book extends TenantEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(nullable = false, length = 50)
    private String isbn;

    @Column(length = 150)
    private String publisher;

    private Integer edition;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(length = 50)
    private String language;

    private Integer pages;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "publisher_id")
    private UUID publisherId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private boolean archived;

    protected Book() {
        // construtor exigido pelo JPA/Hibernate
    }

    public Book(UUID bibliotecaId) {
        super(bibliotecaId);
    }

    public void update(String title, String description, String author, String isbn,
                       String publisher, Integer edition, LocalDate publishedAt,
                       String language, Integer pages,
                       UUID authorId, UUID publisherId, UUID categoryId) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.edition = edition;
        this.publishedAt = publishedAt;
        this.language = language;
        this.pages = pages;
        this.authorId = authorId;
        this.publisherId = publisherId;
        this.categoryId = categoryId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getPublisher() { return publisher; }
    public Integer getEdition() { return edition; }
    public LocalDate getPublishedAt() { return publishedAt; }
    public String getLanguage() { return language; }
    public Integer getPages() { return pages; }
    public UUID getAuthorId() { return authorId; }
    public UUID getPublisherId() { return publisherId; }
    public UUID getCategoryId() { return categoryId; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
