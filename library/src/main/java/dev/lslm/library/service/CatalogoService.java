package dev.lslm.library.service;

import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.dto.AcervoDtos.CatalogoItem;
import dev.lslm.library.dto.BookResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pesquisa no catalogo (RF06) por titulo, autor, ISBN, categoria e disponibilidade. */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final BookRepository bookRepository;
    private final ExemplarRepository exemplarRepository;

    public CatalogoService(BookRepository bookRepository, ExemplarRepository exemplarRepository) {
        this.bookRepository = bookRepository;
        this.exemplarRepository = exemplarRepository;
    }

    public PageResponse<CatalogoItem> buscar(String q, String titulo, String autor, String isbn, UUID categoriaId,
                                             Boolean disponivel, Pageable pageable) {
        UUID bib = RequestContext.bibliotecaId();
        Specification<Book> spec = Specs.<Book>eq("bibliotecaId", bib)
                .and((root, query, cb) -> cb.isFalse(root.get("archived")))
                .and(Specs.eq("categoryId", categoriaId))
                .and(contem("title", titulo))
                .and(contem("author", autor))
                .and(contem("isbn", isbn));

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("author")), like),
                    cb.like(cb.lower(root.get("isbn")), like)));
        }
        if (disponivel != null) {
            boolean quer = disponivel;
            spec = spec.and((root, query, cb) -> {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<Exemplar> ex = sub.from(Exemplar.class);
                sub.select(ex.get("id")).where(
                        cb.equal(ex.get("livroId"), root.get("id")),
                        cb.equal(ex.get("situacao"), Situacao.DISPONIVEL));
                Predicate existe = cb.exists(sub);
                return quer ? existe : cb.not(existe);
            });
        }

        Page<Book> page = bookRepository.findAll(spec, pageable);
        Map<UUID, long[]> contagens = new HashMap<>();
        if (!page.isEmpty()) {
            List<UUID> ids = page.getContent().stream().map(Book::getId).toList();
            for (Object[] row : exemplarRepository.contagemPorLivro(bib, ids, Situacao.DISPONIVEL)) {
                contagens.put((UUID) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            }
        }
        return PageResponse.of(page, b -> {
            long[] c = contagens.getOrDefault(b.getId(), new long[]{0, 0});
            return new CatalogoItem(BookResponse.from(b), c[0], c[1], c[1] > 0);
        });
    }

    private static Specification<Book> contem(String campo, String texto) {
        return (root, query, cb) -> texto == null || texto.isBlank() ? cb.conjunction()
                : cb.like(cb.lower(root.get(campo)), "%" + texto.trim().toLowerCase() + "%");
    }
}
