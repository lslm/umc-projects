package dev.lslm.library.service;

import dev.lslm.library.domain.Categoria;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CategoriaService extends NomeadoService<Categoria> {

    private final BookRepository bookRepository;

    public CategoriaService(CategoriaRepository repository, AuditService audit, BookRepository bookRepository) {
        super(repository, audit, "Categoria");
        this.bookRepository = bookRepository;
    }

    @Override
    protected Categoria novo(UUID bibliotecaId, String nome) {
        return new Categoria(bibliotecaId, nome);
    }

    @Override
    protected boolean emUso(UUID bibliotecaId, UUID id) {
        return bookRepository.existsByBibliotecaIdAndCategoryId(bibliotecaId, id);
    }
}
