package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Leitor;
import dev.lslm.library.dto.LeitorDtos.LeitorRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Cadastro e bloqueio de leitores (RF07). */
@Service
@Transactional
public class LeitorService {

    private final LeitorRepository repository;
    private final BibliotecaRepository bibliotecaRepository;
    private final AuditService audit;

    public LeitorService(LeitorRepository repository, BibliotecaRepository bibliotecaRepository, AuditService audit) {
        this.repository = repository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.audit = audit;
    }

    public Leitor criar(LeitorRequest req) {
        UUID bib = RequestContext.bibliotecaId();
        String nome = Validacao.texto(req.nome(), "nome");
        String email = Validacao.texto(req.email(), "email").toLowerCase();
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        if (repository.countByBibliotecaId(bib) >= cfg.getLimiteLeitores()) {
            throw ApiException.conflict("LIMITE_PLANO_EXCEDIDO",
                    "Limite de " + cfg.getLimiteLeitores() + " leitores do plano atingido.");
        }
        if (repository.existsByBibliotecaIdAndEmailIgnoreCase(bib, email)) {
            throw ApiException.conflict("LEITOR_JA_CADASTRADO", "Já existe um leitor com o e-mail " + email + ".");
        }
        Leitor l = repository.save(new Leitor(bib, nome, email, req.documento(), req.telefone()));
        audit.registrar("LEITOR_CRIADO", "Leitor", l.getId(), "email=" + email);
        return l;
    }

    @Transactional(readOnly = true)
    public Page<Leitor> listar(String q, Boolean bloqueado, Pageable pageable) {
        Specification<Leitor> spec = Specs.<Leitor>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("bloqueado", bloqueado));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nome")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Leitor buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado."));
    }

    public Leitor atualizar(UUID id, LeitorRequest req) {
        Leitor l = buscar(id);
        String email = Validacao.texto(req.email(), "email").toLowerCase();
        if (!email.equalsIgnoreCase(l.getEmail())
                && repository.existsByBibliotecaIdAndEmailIgnoreCase(l.getBibliotecaId(), email)) {
            throw ApiException.conflict("LEITOR_JA_CADASTRADO", "Já existe um leitor com o e-mail " + email + ".");
        }
        l.atualizar(Validacao.texto(req.nome(), "nome"), email, req.documento(), req.telefone());
        audit.registrar("LEITOR_ATUALIZADO", "Leitor", id, "email=" + email);
        return l;
    }

    public Leitor bloquear(UUID id, String motivo) {
        Leitor l = buscar(id);
        l.bloquear(Validacao.texto(motivo, "motivo"));
        audit.registrar("LEITOR_BLOQUEADO", "Leitor", id, "motivo=" + motivo);
        return l;
    }

    public Leitor desbloquear(UUID id) {
        Leitor l = buscar(id);
        l.desbloquear();
        audit.registrar("LEITOR_DESBLOQUEADO", "Leitor", id, null);
        return l;
    }

    /** Registra o token do dispositivo (FCM) para receber notificacoes push. */
    public Leitor registrarDispositivo(UUID id, String fcmToken) {
        Leitor l = buscar(id);
        l.setFcmToken(Validacao.texto(fcmToken, "fcmToken"));
        audit.registrar("DISPOSITIVO_REGISTRADO", "Leitor", id, null);
        return l;
    }
}
