package dev.lslm.library.service;

import dev.lslm.library.domain.Book;
import dev.lslm.library.dto.BookRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.exception.BookNotFoundException;
import dev.lslm.library.repository.AutorRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.CategoriaRepository;
import dev.lslm.library.repository.EditoraRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Validacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Acervo de obras (RF04). Todas as operacoes sao restritas a biblioteca do contexto (RF18). */
@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final ExemplarRepository exemplarRepository;
    private final AutorRepository autorRepository;
    private final EditoraRepository editoraRepository;
    private final CategoriaRepository categoriaRepository;
    private final AuditService audit;

    public BookService(BookRepository bookRepository, ExemplarRepository exemplarRepository,
                       AutorRepository autorRepository, EditoraRepository editoraRepository,
                       CategoriaRepository categoriaRepository, AuditService audit) {
        this.bookRepository = bookRepository;
        this.exemplarRepository = exemplarRepository;
        this.autorRepository = autorRepository;
        this.editoraRepository = editoraRepository;
        this.categoriaRepository = categoriaRepository;
        this.audit = audit;
    }

    public Book create(BookRequest request) {
        UUID bib = RequestContext.bibliotecaId();
        String isbn = Validacao.texto(request.isbn(), "isbn");
        bookRepository.findByBibliotecaIdAndIsbn(bib, isbn).ifPresent(existing -> {
            throw ApiException.badRequest("ISBN_DUPLICADO", "Já existe um livro cadastrado com o ISBN " + isbn + ".");
        });

        Book book = new Book(bib);
        aplicar(book, request, bib);
        book = bookRepository.save(book);
        audit.registrar("LIVRO_CRIADO", "Book", book.getId(), "isbn=" + isbn + "; titulo=" + book.getTitle());
        return book;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll(boolean incluirArquivados) {
        UUID bib = RequestContext.bibliotecaId();
        return incluirArquivados ? bookRepository.findByBibliotecaId(bib)
                : bookRepository.findByBibliotecaIdAndArchivedFalse(bib);
    }

    @Transactional(readOnly = true)
    public Book findById(UUID id) {
        return bookRepository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book update(UUID id, BookRequest request) {
        UUID bib = RequestContext.bibliotecaId();
        Book book = findById(id);
        String isbn = Validacao.texto(request.isbn(), "isbn");
        bookRepository.findByBibliotecaIdAndIsbn(bib, isbn).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw ApiException.badRequest("ISBN_DUPLICADO", "Já existe um livro cadastrado com o ISBN " + isbn + ".");
            }
        });
        aplicar(book, request, bib);
        book = bookRepository.save(book);
        audit.registrar("LIVRO_ATUALIZADO", "Book", id, "isbn=" + isbn);
        return book;
    }

    public Book arquivar(UUID id, boolean arquivar) {
        Book book = findById(id);
        book.setArchived(arquivar);
        audit.registrar(arquivar ? "LIVRO_ARQUIVADO" : "LIVRO_DESARQUIVADO", "Book", id, null);
        return book;
    }

    public void delete(UUID id) {
        Book book = findById(id);
        if (exemplarRepository.existsByBibliotecaIdAndLivroId(book.getBibliotecaId(), id)) {
            throw ApiException.conflict("LIVRO_COM_EXEMPLARES",
                    "O livro possui exemplares cadastrados; arquive-o em vez de removê-lo.");
        }
        bookRepository.delete(book);
        audit.registrar("LIVRO_REMOVIDO", "Book", id, "isbn=" + book.getIsbn());
    }

    private void aplicar(Book book, BookRequest r, UUID bib) {
        String title = Validacao.texto(r.title(), "title");
        String author = r.author();
        String publisher = r.publisher();
        if (r.authorId() != null) {
            var a = autorRepository.findByIdAndBibliotecaId(r.authorId(), bib).orElseThrow(
                    () -> ApiException.notFound("AUTOR_NAO_ENCONTRADO", "Autor não encontrado."));
            if (author == null || author.isBlank()) {
                author = a.getNome();
            }
        }
        if (r.publisherId() != null) {
            var e = editoraRepository.findByIdAndBibliotecaId(r.publisherId(), bib).orElseThrow(
                    () -> ApiException.notFound("EDITORA_NAO_ENCONTRADA", "Editora não encontrada."));
            if (publisher == null || publisher.isBlank()) {
                publisher = e.getNome();
            }
        }
        if (r.categoryId() != null) {
            categoriaRepository.findByIdAndBibliotecaId(r.categoryId(), bib).orElseThrow(
                    () -> ApiException.notFound("CATEGORIA_NAO_ENCONTRADA", "Categoria não encontrada."));
        }
        book.update(title, r.description(), Validacao.texto(author, "author"), r.isbn().trim(), publisher,
                r.edition(), r.publishedAt(), r.language(), r.pages(),
                r.authorId(), r.publisherId(), r.categoryId());
    }
}
