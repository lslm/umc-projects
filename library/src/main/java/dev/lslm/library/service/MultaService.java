package dev.lslm.library.service;

import dev.lslm.library.domain.Multa;
import dev.lslm.library.domain.Multa.Status;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.MultaRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Multas e pendencias (RF11): ajuste, isencao e pagamento sempre com justificativa auditavel. */
@Service
@Transactional
public class MultaService {

    private final MultaRepository repository;
    private final AuditService audit;
    private final Clock clock;

    public MultaService(MultaRepository repository, AuditService audit, Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Multa> listar(UUID leitorId, Status status, Pageable pageable) {
        Specification<Multa> spec = Specs.<Multa>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("leitorId", leitorId))
                .and(Specs.eq("status", status));
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Multa> pendentesDoLeitor(UUID leitorId) {
        return repository.findAll(Specs.<Multa>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("leitorId", leitorId)).and(Specs.eq("status", Status.PENDENTE)));
    }

    @Transactional(readOnly = true)
    public Multa buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("MULTA_NAO_ENCONTRADA", "Multa não encontrada."));
    }

    public Multa ajustar(UUID id, BigDecimal novoValor, String justificativa) {
        Multa m = pendente(id);
        String j = Validacao.texto(justificativa, "justificativa");
        if (Validacao.obrigatorio(novoValor, "valor").signum() < 0) {
            throw new IllegalArgumentException("O valor da multa não pode ser negativo.");
        }
        BigDecimal anterior = m.getValor();
        m.setValor(novoValor.setScale(2, RoundingMode.HALF_UP));
        registrar(m, j);
        audit.registrar("MULTA_AJUSTADA", "Multa", id,
                "de=" + anterior + "; para=" + m.getValor() + "; justificativa=" + j);
        return m;
    }

    public Multa isentar(UUID id, String justificativa) {
        Multa m = pendente(id);
        String j = Validacao.texto(justificativa, "justificativa");
        m.setStatus(Status.ISENTA);
        registrar(m, j);
        audit.registrar("MULTA_ISENTADA", "Multa", id, "valor=" + m.getValor() + "; justificativa=" + j);
        return m;
    }

    public Multa pagar(UUID id, String justificativa) {
        Multa m = pendente(id);
        m.setStatus(Status.PAGA);
        registrar(m, justificativa != null && !justificativa.isBlank() ? justificativa.trim() : "Pagamento registrado");
        audit.registrar("MULTA_PAGA", "Multa", id, "valor=" + m.getValor());
        return m;
    }

    private Multa pendente(UUID id) {
        Multa m = buscar(id);
        if (m.getStatus() != Status.PENDENTE) {
            throw ApiException.conflict("MULTA_NAO_PENDENTE", "A multa já está " + m.getStatus() + ".");
        }
        return m;
    }

    private void registrar(Multa m, String justificativa) {
        m.setJustificativa(justificativa);
        m.setAtualizadaEm(clock.instant());
    }
}
