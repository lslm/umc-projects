package dev.lslm.library.repository;

import dev.lslm.library.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByBibliotecaIdAndIsbn(UUID bibliotecaId, String isbn);

    Optional<Book> findByIdAndBibliotecaId(UUID id, UUID bibliotecaId);

    List<Book> findByBibliotecaId(UUID bibliotecaId);

    List<Book> findByBibliotecaIdAndArchivedFalse(UUID bibliotecaId);

    boolean existsByBibliotecaIdAndAuthorId(UUID bibliotecaId, UUID authorId);

    boolean existsByBibliotecaIdAndPublisherId(UUID bibliotecaId, UUID publisherId);

    boolean existsByBibliotecaIdAndCategoryId(UUID bibliotecaId, UUID categoryId);
}
