package dev.lslm.library.service;

import dev.lslm.library.domain.Autor;
import dev.lslm.library.repository.AutorRepository;
import dev.lslm.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AutorService extends NomeadoService<Autor> {

    private final BookRepository bookRepository;

    public AutorService(AutorRepository repository, AuditService audit, BookRepository bookRepository) {
        super(repository, audit, "Autor");
        this.bookRepository = bookRepository;
    }

    @Override
    protected Autor novo(UUID bibliotecaId, String nome) {
        return new Autor(bibliotecaId, nome);
    }

    @Override
    protected boolean emUso(UUID bibliotecaId, UUID id) {
        return bookRepository.existsByBibliotecaIdAndAuthorId(bibliotecaId, id);
    }
}
