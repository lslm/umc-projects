package dev.lslm.library.service;

import dev.lslm.library.domain.Editora;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EditoraRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EditoraService extends NomeadoService<Editora> {

    private final BookRepository bookRepository;

    public EditoraService(EditoraRepository repository, AuditService audit, BookRepository bookRepository) {
        super(repository, audit, "Editora");
        this.bookRepository = bookRepository;
    }

    @Override
    protected Editora novo(UUID bibliotecaId, String nome) {
        return new Editora(bibliotecaId, nome);
    }

    @Override
    protected boolean emUso(UUID bibliotecaId, UUID id) {
        return bookRepository.existsByBibliotecaIdAndPublisherId(bibliotecaId, id);
    }
}
