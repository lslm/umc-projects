package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Book;
import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Notificacao.Tipo;
import dev.lslm.library.domain.Reserva;
import dev.lslm.library.dto.NotificacaoDtos.VerificacaoPrazosResponse;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.BookRepository;
import dev.lslm.library.repository.EmprestimoRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Avisos de vencimento/atraso e expiracao de reservas nao retiradas (RF13). */
@Service
@Transactional
public class PrazoService {

    private final BibliotecaRepository bibliotecaRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final ReservaRepository reservaRepository;
    private final ExemplarRepository exemplarRepository;
    private final BookRepository bookRepository;
    private final NotificacaoService notificacaoService;
    private final ReservaService reservaService;
    private final AuditService audit;
    private final Clock clock;

    public PrazoService(BibliotecaRepository bibliotecaRepository, EmprestimoRepository emprestimoRepository,
                        ReservaRepository reservaRepository, ExemplarRepository exemplarRepository,
                        BookRepository bookRepository, NotificacaoService notificacaoService,
                        ReservaService reservaService, AuditService audit, Clock clock) {
        this.bibliotecaRepository = bibliotecaRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.reservaRepository = reservaRepository;
        this.exemplarRepository = exemplarRepository;
        this.bookRepository = bookRepository;
        this.notificacaoService = notificacaoService;
        this.reservaService = reservaService;
        this.audit = audit;
        this.clock = clock;
    }

    public VerificacaoPrazosResponse verificar(UUID bibliotecaId) {
        Biblioteca cfg = bibliotecaRepository.findById(bibliotecaId)
                .orElseThrow(() -> ApiException.notFound("BIBLIOTECA_NAO_ENCONTRADA", "Biblioteca não encontrada."));
        Instant agora = clock.instant();
        Instant limite = agora.plus(cfg.getDiasAvisoVencimento(), ChronoUnit.DAYS);
        int vencimento = 0;
        int atraso = 0;

        for (Emprestimo e : emprestimoRepository.findByBibliotecaIdAndStatus(bibliotecaId, Emprestimo.Status.ATIVO)) {
            String titulo = bookRepository.findById(e.getLivroId()).map(Book::getTitle).orElse("obra");
            if (e.getVencimentoEm().isBefore(agora)) {
                if (!e.isAvisoAtrasoEnviado()) {
                    notificacaoService.enviar(bibliotecaId, e.getLeitorId(), Tipo.ATRASO, "Empréstimo em atraso",
                            "O empréstimo de \"" + titulo + "\" venceu em " + e.getVencimentoEm()
                                    + ". Devolva o quanto antes para evitar mais multa.");
                    e.setAvisoAtrasoEnviado(true);
                    atraso++;
                }
            } else if (!e.getVencimentoEm().isAfter(limite) && !e.isAvisoVencimentoEnviado()) {
                notificacaoService.enviar(bibliotecaId, e.getLeitorId(), Tipo.VENCIMENTO_PROXIMO,
                        "Devolução próxima",
                        "O empréstimo de \"" + titulo + "\" vence em " + e.getVencimentoEm() + ".");
                e.setAvisoVencimentoEnviado(true);
                vencimento++;
            }
        }

        int expiradas = 0;
        for (Reserva r : reservaRepository.findByBibliotecaIdAndStatusAndDisponivelAteBefore(
                bibliotecaId, Reserva.Status.DISPONIVEL, agora)) {
            r.setStatus(Reserva.Status.EXPIRADA);
            exemplarRepository.bloquear(r.getExemplarId(), bibliotecaId).ifPresent(reservaService::atribuirExemplar);
            audit.registrarPara(bibliotecaId, "RESERVA_EXPIRADA", "Reserva", r.getId(), null);
            expiradas++;
        }
        audit.registrarPara(bibliotecaId, "PRAZOS_VERIFICADOS", "Biblioteca", bibliotecaId,
                "vencimento=" + vencimento + "; atraso=" + atraso + "; reservasExpiradas=" + expiradas);
        return new VerificacaoPrazosResponse(vencimento, atraso, expiradas);
    }
}
