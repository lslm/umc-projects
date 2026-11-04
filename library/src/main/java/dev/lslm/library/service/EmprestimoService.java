package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Emprestimo.Status;
import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.domain.Leitor;
import dev.lslm.library.domain.Multa;
import dev.lslm.library.domain.Reserva;
import dev.lslm.library.dto.CirculacaoDtos.DevolucaoResponse;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoRequest;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoResponse;
import dev.lslm.library.dto.CirculacaoDtos.MultaResponse;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EmprestimoRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.repository.MultaRepository;
import dev.lslm.library.repository.ReservaRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Emprestimos, devolucoes e renovacoes (RF08, RF09, RF10). Operacoes criticas sao transacionais (RNF06). */
@Service
@Transactional
public class EmprestimoService {

    private final EmprestimoRepository repository;
    private final ExemplarRepository exemplarRepository;
    private final BookRepository bookRepository;
    private final MultaRepository multaRepository;
    private final ReservaRepository reservaRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final LeitorService leitorService;
    private final ReservaService reservaService;
    private final AuditService audit;
    private final Clock clock;

    public EmprestimoService(EmprestimoRepository repository, ExemplarRepository exemplarRepository,
                             BookRepository bookRepository, MultaRepository multaRepository,
                             ReservaRepository reservaRepository, BibliotecaRepository bibliotecaRepository,
                             LeitorService leitorService, ReservaService reservaService,
                             AuditService audit, Clock clock) {
        this.repository = repository;
        this.exemplarRepository = exemplarRepository;
        this.bookRepository = bookRepository;
        this.multaRepository = multaRepository;
        this.reservaRepository = reservaRepository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.leitorService = leitorService;
        this.reservaService = reservaService;
        this.audit = audit;
        this.clock = clock;
    }

    public EmprestimoResponse criar(EmprestimoRequest req) {
        UUID bib = RequestContext.bibliotecaId();
        UUID leitorId = Validacao.obrigatorio(req.leitorId(), "leitorId");
        UUID exemplarId = Validacao.obrigatorio(req.exemplarId(), "exemplarId");
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        Leitor leitor = leitorService.buscar(leitorId);
        Instant agora = clock.instant();

        if (leitor.isBloqueado()) {
            throw ApiException.conflict("LEITOR_BLOQUEADO", "Leitor bloqueado: " + leitor.getMotivoBloqueio());
        }
        if (multaRepository.existsByBibliotecaIdAndLeitorIdAndStatus(bib, leitorId, Multa.Status.PENDENTE)) {
            throw ApiException.conflict("LEITOR_COM_MULTA_PENDENTE", "O leitor possui multas pendentes.");
        }
        if (repository.existsByBibliotecaIdAndLeitorIdAndStatusAndVencimentoEmBefore(bib, leitorId, Status.ATIVO, agora)) {
            throw ApiException.conflict("LEITOR_COM_ATRASO", "O leitor possui empréstimos em atraso.");
        }
        if (repository.countByBibliotecaIdAndLeitorIdAndStatus(bib, leitorId, Status.ATIVO)
                >= cfg.getLimiteItensEmprestimo()) {
            throw ApiException.conflict("LIMITE_ITENS_EXCEDIDO",
                    "Limite de " + cfg.getLimiteItensEmprestimo() + " itens emprestados atingido.");
        }

        Exemplar ex = exemplarRepository.bloquear(exemplarId, bib)
                .orElseThrow(() -> ApiException.notFound("EXEMPLAR_NAO_ENCONTRADO", "Exemplar não encontrado."));
        Reserva atendida = null;
        if (ex.getSituacao() == Situacao.RESERVADO) {
            atendida = reservaRepository.findFirstByBibliotecaIdAndExemplarIdAndStatus(bib, ex.getId(),
                            Reserva.Status.DISPONIVEL)
                    .filter(r -> r.getLeitorId().equals(leitorId))
                    .orElseThrow(() -> ApiException.conflict("EXEMPLAR_RESERVADO",
                            "Exemplar separado para a reserva de outro leitor."));
        } else if (ex.getSituacao() != Situacao.DISPONIVEL) {
            throw ApiException.conflict("LIVRO_INDISPONIVEL", "Não há exemplar disponível.");
        }

        if (atendida != null) {
            atendida.setStatus(Reserva.Status.ATENDIDA);
        }
        ex.setSituacao(Situacao.EMPRESTADO);
        Emprestimo e = repository.save(new Emprestimo(bib, leitorId, ex.getId(), ex.getLivroId(), agora,
                agora.plus(cfg.getDiasEmprestimo(), ChronoUnit.DAYS)));
        audit.registrar("EMPRESTIMO_CRIADO", "Emprestimo", e.getId(),
                "leitor=" + leitorId + "; exemplar=" + exemplarId + "; vencimento=" + e.getVencimentoEm());
        return toResponses(List.of(e)).get(0);
    }

    public DevolucaoResponse devolver(UUID id, Instant devolvidoEm) {
        UUID bib = RequestContext.bibliotecaId();
        Emprestimo e = buscar(id);
        if (e.getStatus() == Status.DEVOLVIDO) {
            throw ApiException.conflict("EMPRESTIMO_JA_DEVOLVIDO", "Este empréstimo já foi devolvido.");
        }
        Instant quando = devolvidoEm != null ? devolvidoEm : clock.instant();
        if (quando.isBefore(e.getEmprestadoEm())) {
            throw ApiException.badRequest("DATA_INVALIDA", "A devolução não pode ser anterior ao empréstimo.");
        }
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        Exemplar ex = exemplarRepository.bloquear(e.getExemplarId(), bib).orElseThrow();

        e.devolver(quando);
        MultaResponse multaResp = null;
        if (e.getDiasAtraso() > 0) {
            BigDecimal valor = cfg.getMultaDiaria().multiply(BigDecimal.valueOf(e.getDiasAtraso()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            Multa multa = multaRepository.save(new Multa(bib, e.getId(), e.getLeitorId(), valor, clock.instant()));
            multaResp = MultaResponse.from(multa);
            audit.registrar("MULTA_GERADA", "Multa", multa.getId(), "dias=" + e.getDiasAtraso() + "; valor=" + valor);
        }
        Reserva reserva = reservaService.atribuirExemplar(ex); // libera o exemplar ou separa para a fila
        audit.registrar("EMPRESTIMO_DEVOLVIDO", "Emprestimo", id,
                "diasAtraso=" + e.getDiasAtraso() + "; exemplar=" + ex.getSituacao());
        return new DevolucaoResponse(toResponses(List.of(e)).get(0), e.getDiasAtraso(), multaResp,
                ex.getSituacao().name(), reserva != null ? reserva.getId() : null);
    }

    public EmprestimoResponse renovar(UUID id) {
        UUID bib = RequestContext.bibliotecaId();
        Emprestimo e = buscar(id);
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        Instant agora = clock.instant();

        if (e.getStatus() != Status.ATIVO) {
            throw ApiException.conflict("EMPRESTIMO_JA_DEVOLVIDO", "Só é possível renovar empréstimos ativos.");
        }
        if (leitorService.buscar(e.getLeitorId()).isBloqueado()) {
            throw ApiException.conflict("LEITOR_BLOQUEADO", "Leitor bloqueado.");
        }
        if (e.getRenovacoes() >= cfg.getLimiteRenovacoes()) {
            throw ApiException.conflict("LIMITE_RENOVACOES_ATINGIDO",
                    "Limite de " + cfg.getLimiteRenovacoes() + " renovações atingido.");
        }
        if (e.getVencimentoEm().isBefore(agora)) {
            throw ApiException.conflict("EMPRESTIMO_ATRASADO", "Empréstimos em atraso não podem ser renovados.");
        }
        if (reservaRepository.existsByBibliotecaIdAndLivroIdAndStatus(bib, e.getLivroId(), Reserva.Status.AGUARDANDO)) {
            throw ApiException.conflict("RESERVA_PENDENTE", "Há reserva pendente para esta obra.");
        }
        e.renovar(e.getVencimentoEm().plus(cfg.getDiasEmprestimo(), ChronoUnit.DAYS));
        audit.registrar("EMPRESTIMO_RENOVADO", "Emprestimo", id,
                "renovacoes=" + e.getRenovacoes() + "; vencimento=" + e.getVencimentoEm());
        return toResponses(List.of(e)).get(0);
    }

    @Transactional(readOnly = true)
    public Emprestimo buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("EMPRESTIMO_NAO_ENCONTRADO", "Empréstimo não encontrado."));
    }

    @Transactional(readOnly = true)
    public EmprestimoResponse detalhar(UUID id) {
        return toResponses(List.of(buscar(id))).get(0);
    }

    @Transactional(readOnly = true)
    public Page<Emprestimo> listar(Status status, UUID leitorId, UUID livroId, Pageable pageable) {
        Specification<Emprestimo> spec = Specs.<Emprestimo>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("status", status))
                .and(Specs.eq("leitorId", leitorId))
                .and(Specs.eq("livroId", livroId));
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponse> porLeitor(UUID leitorId, Status status) {
        return toResponses(repository.findByBibliotecaIdAndLeitorIdOrderByEmprestadoEmDesc(
                RequestContext.bibliotecaId(), leitorId).stream()
                .filter(e -> status == null || e.getStatus() == status).toList());
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponse> toResponses(List<Emprestimo> lista) {
        Instant agora = clock.instant();
        Map<UUID, String> titulos = bookRepository
                .findAllById(lista.stream().map(Emprestimo::getLivroId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle, (a, b) -> a));
        return lista.stream().map(e -> new EmprestimoResponse(e.getId(), e.getLeitorId(), e.getLivroId(),
                titulos.get(e.getLivroId()), e.getExemplarId(), e.getEmprestadoEm(), e.getVencimentoEm(),
                e.getDevolvidoEm(), e.getRenovacoes(), e.getStatus(),
                e.getStatus() == Status.ATIVO ? Emprestimo.calcularDiasAtraso(e.getVencimentoEm(), agora)
                        : e.getDiasAtraso())).toList();
    }
}
