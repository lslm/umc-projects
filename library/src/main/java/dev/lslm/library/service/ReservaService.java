package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.domain.Leitor;
import dev.lslm.library.domain.Notificacao.Tipo;
import dev.lslm.library.domain.Reserva;
import dev.lslm.library.domain.Reserva.Status;
import dev.lslm.library.dto.CirculacaoDtos.ReservaResponse;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EmprestimoRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.repository.ReservaRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Specs;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reservas e fila de espera (RF12). */
@Service
@Transactional
public class ReservaService {

    private static final EnumSet<Status> ATIVAS = EnumSet.of(Status.AGUARDANDO, Status.DISPONIVEL);

    private final ReservaRepository repository;
    private final BookRepository bookRepository;
    private final LeitorRepository leitorRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final NotificacaoService notificacaoService;
    private final AuditService audit;
    private final Clock clock;

    public ReservaService(ReservaRepository repository, BookRepository bookRepository,
                          LeitorRepository leitorRepository, ExemplarRepository exemplarRepository,
                          EmprestimoRepository emprestimoRepository, BibliotecaRepository bibliotecaRepository,
                          NotificacaoService notificacaoService, AuditService audit, Clock clock) {
        this.repository = repository;
        this.bookRepository = bookRepository;
        this.leitorRepository = leitorRepository;
        this.exemplarRepository = exemplarRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.bibliotecaRepository = bibliotecaRepository;
        this.notificacaoService = notificacaoService;
        this.audit = audit;
        this.clock = clock;
    }

    public ReservaResponse criar(UUID livroId, UUID leitorId) {
        UUID bib = RequestContext.bibliotecaId();
        Validacao.obrigatorio(livroId, "livroId");
        Validacao.obrigatorio(leitorId, "leitorId");

        Book livro = bookRepository.findByIdAndBibliotecaId(livroId, bib)
                .orElseThrow(() -> ApiException.notFound("LIVRO_NAO_ENCONTRADO", "Livro não encontrado."));
        if (livro.isArchived()) {
            throw ApiException.conflict("LIVRO_ARQUIVADO", "A obra está arquivada.");
        }
        Leitor leitor = leitorRepository.findByIdAndBibliotecaId(leitorId, bib)
                .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado."));
        if (leitor.isBloqueado()) {
            throw ApiException.conflict("LEITOR_BLOQUEADO", "Leitor bloqueado: " + leitor.getMotivoBloqueio());
        }
        if (exemplarRepository.existsByBibliotecaIdAndLivroIdAndSituacao(bib, livroId, Situacao.DISPONIVEL)) {
            throw ApiException.conflict("LIVRO_DISPONIVEL", "Há exemplar disponível; a reserva só é necessária quando não há.");
        }
        if (!exemplarRepository.existsByBibliotecaIdAndLivroIdAndSituacaoIn(bib, livroId,
                EnumSet.of(Situacao.EMPRESTADO, Situacao.RESERVADO))) {
            throw ApiException.conflict("LIVRO_SEM_EXEMPLARES", "Não há exemplares em circulação para esta obra.");
        }
        if (repository.existsByBibliotecaIdAndLeitorIdAndLivroIdAndStatusIn(bib, leitorId, livroId, ATIVAS)) {
            throw ApiException.conflict("RESERVA_DUPLICADA", "O leitor já possui uma reserva ativa para esta obra.");
        }
        if (emprestimoRepository.existsByBibliotecaIdAndLeitorIdAndLivroIdAndStatus(bib, leitorId, livroId,
                Emprestimo.Status.ATIVO)) {
            throw ApiException.conflict("LEITOR_JA_POSSUI_OBRA", "O leitor já está com um exemplar desta obra.");
        }

        Reserva r = repository.save(new Reserva(bib, livroId, leitorId, clock.instant()));
        audit.registrar("RESERVA_CRIADA", "Reserva", r.getId(), "livro=" + livroId + "; leitor=" + leitorId);
        return toResponses(List.of(r)).get(0);
    }

    public ReservaResponse cancelar(UUID id) {
        Reserva r = buscar(id);
        if (!ATIVAS.contains(r.getStatus())) {
            throw ApiException.conflict("RESERVA_NAO_CANCELAVEL", "Só é possível cancelar reservas ativas.");
        }
        boolean tinhaExemplar = r.getStatus() == Status.DISPONIVEL;
        r.setStatus(Status.CANCELADA);
        if (tinhaExemplar && r.getExemplarId() != null) {
            exemplarRepository.bloquear(r.getExemplarId(), r.getBibliotecaId()).ifPresent(this::atribuirExemplar);
        }
        audit.registrar("RESERVA_CANCELADA", "Reserva", id, null);
        return toResponses(List.of(r)).get(0);
    }

    @Transactional(readOnly = true)
    public Reserva buscar(UUID id) {
        return repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("RESERVA_NAO_ENCONTRADA", "Reserva não encontrada."));
    }

    @Transactional(readOnly = true)
    public ReservaResponse detalhar(UUID id) {
        return toResponses(List.of(buscar(id))).get(0);
    }

    @Transactional(readOnly = true)
    public Page<Reserva> listar(UUID livroId, UUID leitorId, Status status, Pageable pageable) {
        Specification<Reserva> spec = Specs.<Reserva>eq("bibliotecaId", RequestContext.bibliotecaId())
                .and(Specs.eq("livroId", livroId))
                .and(Specs.eq("leitorId", leitorId))
                .and(Specs.eq("status", status));
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> reservasAtivasDoLeitor(UUID leitorId) {
        return toResponses(repository.findByBibliotecaIdAndLeitorIdAndStatusInOrderByCriadaEmAsc(
                RequestContext.bibliotecaId(), leitorId, ATIVAS));
    }

    /**
     * Chamado quando um exemplar fica livre: separa-o para o primeiro da fila (RESERVADO) e avisa o leitor,
     * ou o marca como DISPONIVEL se nao ha fila.
     */
    public Reserva atribuirExemplar(Exemplar exemplar) {
        UUID bib = exemplar.getBibliotecaId();
        var proxima = repository.findFirstByBibliotecaIdAndLivroIdAndStatusOrderByCriadaEmAsc(
                bib, exemplar.getLivroId(), Status.AGUARDANDO);
        if (proxima.isEmpty()) {
            exemplar.setSituacao(Situacao.DISPONIVEL);
            return null;
        }
        Biblioteca cfg = bibliotecaRepository.findById(bib).orElseThrow();
        Reserva r = proxima.get();
        Instant agora = clock.instant();
        r.setStatus(Status.DISPONIVEL);
        r.setExemplarId(exemplar.getId());
        r.setDisponivelAte(agora.plus(cfg.getDiasRetiradaReserva(), ChronoUnit.DAYS));
        exemplar.setSituacao(Situacao.RESERVADO);

        String titulo = bookRepository.findById(exemplar.getLivroId()).map(Book::getTitle).orElse("obra reservada");
        notificacaoService.enviar(bib, r.getLeitorId(), Tipo.RESERVA_DISPONIVEL, "Reserva disponível",
                "A obra \"" + titulo + "\" está disponível para retirada até " + r.getDisponivelAte() + ".");
        audit.registrarPara(bib, "RESERVA_DISPONIBILIZADA", "Reserva", r.getId(), "exemplar=" + exemplar.getId());
        return r;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> toResponses(List<Reserva> reservas) {
        Map<UUID, String> titulos = bookRepository
                .findAllById(reservas.stream().map(Reserva::getLivroId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle, (a, b) -> a));
        return reservas.stream().map(r -> new ReservaResponse(r.getId(), r.getLivroId(),
                titulos.get(r.getLivroId()), r.getLeitorId(), r.getStatus(), posicao(r), r.getExemplarId(),
                r.getCriadaEm(), r.getDisponivelAte())).toList();
    }

    /** 0 = exemplar ja separado; 1.. = posicao na fila; fora da fila = 0. */
    private int posicao(Reserva r) {
        if (r.getStatus() != Status.AGUARDANDO) {
            return 0;
        }
        return (int) repository.countByBibliotecaIdAndLivroIdAndStatusAndCriadaEmBefore(
                r.getBibliotecaId(), r.getLivroId(), Status.AGUARDANDO, r.getCriadaEm()) + 1;
    }
}
