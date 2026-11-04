package dev.lslm.library.service;

import dev.lslm.library.domain.Auditoria;
import dev.lslm.library.repository.AuditoriaRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/** Trilha de auditoria (RF16): usuario, biblioteca, data, origem, acao e resultado. */
@Service
public class AuditService {

    private final AuditoriaRepository repository;
    private final Clock clock;

    public AuditService(AuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void registrar(String acao, String entidade, UUID entidadeId, String detalhes) {
        registrarPara(RequestContext.bibliotecaIdOrNull(), acao, entidade, entidadeId, detalhes);
    }

    public void registrarPara(UUID bibliotecaId, String acao, String entidade, UUID entidadeId, String detalhes) {
        repository.save(new Auditoria(bibliotecaId, RequestContext.usuarioId(), acao, entidade, entidadeId,
                "SUCESSO", RequestContext.origem(), RequestContext.requestId(), detalhes, clock.instant()));
    }

    public Page<Auditoria> buscar(String acao, String entidade, UUID usuarioId, Pageable pageable) {
        Specification<Auditoria> spec = Specs.<Auditoria>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("acao", acao))
                .and(Specs.eq("entidade", entidade))
                .and(Specs.eq("usuarioId", usuarioId));
        return repository.findAll(spec, pageable);
    }
}
