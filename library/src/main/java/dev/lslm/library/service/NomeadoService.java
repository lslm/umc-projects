package dev.lslm.library.service;

import dev.lslm.library.domain.Nomeado;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.NomeadoRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Validacao;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD generico de autores, editoras e categorias (RF04), sempre dentro do tenant. */
@Transactional
public abstract class NomeadoService<T extends Nomeado> {

    private final NomeadoRepository<T> repository;
    private final AuditService audit;
    private final String rotulo;

    protected NomeadoService(NomeadoRepository<T> repository, AuditService audit, String rotulo) {
        this.repository = repository;
        this.audit = audit;
        this.rotulo = rotulo;
    }

    protected abstract T novo(UUID bibliotecaId, String nome);

    protected abstract boolean emUso(UUID bibliotecaId, UUID id);

    public T criar(String nome) {
        UUID bib = RequestContext.bibliotecaId();
        String n = Validacao.texto(nome, "nome");
        if (repository.existsByBibliotecaIdAndNomeIgnoreCase(bib, n)) {
            throw ApiException.conflict("NOME_DUPLICADO", "Já existe " + rotulo + " com o nome '" + n + "'.");
        }
        T t = repository.save(novo(bib, n));
        audit.registrar("CRIADO", rotulo, t.getId(), "nome=" + n);
        return t;
    }

    @Transactional(readOnly = true)
    public List<T> listar() {
        return repository.findByBibliotecaIdOrderByNome(RequestContext.bibliotecaId());
    }

    @Transactional(readOnly = true)
    public T buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("CADASTRO_NAO_ENCONTRADO",
                        capitalizar(rotulo) + " não encontrado(a)."));
    }

    public T atualizar(UUID id, String nome) {
        T t = buscar(id);
        String n = Validacao.texto(nome, "nome");
        if (!n.equalsIgnoreCase(t.getNome())
                && repository.existsByBibliotecaIdAndNomeIgnoreCase(t.getBibliotecaId(), n)) {
            throw ApiException.conflict("NOME_DUPLICADO", "Já existe " + rotulo + " com o nome '" + n + "'.");
        }
        t.setNome(n);
        audit.registrar("ATUALIZADO", rotulo, t.getId(), "nome=" + n);
        return t;
    }

    public void remover(UUID id) {
        T t = buscar(id);
        if (emUso(t.getBibliotecaId(), id)) {
            throw ApiException.conflict("CADASTRO_EM_USO", "Não é possível remover: há obras vinculadas.");
        }
        repository.delete(t);
        audit.registrar("REMOVIDO", rotulo, id, "nome=" + t.getNome());
    }

    private static String capitalizar(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
