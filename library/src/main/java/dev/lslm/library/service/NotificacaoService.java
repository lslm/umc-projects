package dev.lslm.library.service;

import dev.lslm.library.domain.Leitor;
import dev.lslm.library.domain.Notificacao;
import dev.lslm.library.domain.Notificacao.Tipo;
import dev.lslm.library.dto.NotificacaoDtos.ComunicadoRequest;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.repository.NotificacaoRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Validacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Notificacoes (RF13): grava na area do leitor e "chama" o Firebase Cloud Messaging (simulado). */
@Service
@Transactional
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final LeitorRepository leitorRepository;
    private final AuditService audit;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository repository, LeitorRepository leitorRepository,
                              AuditService audit, Clock clock) {
        this.repository = repository;
        this.leitorRepository = leitorRepository;
        this.audit = audit;
        this.clock = clock;
    }

    public Notificacao enviar(UUID bibliotecaId, UUID leitorId, Tipo tipo, String titulo, String mensagem) {
        Leitor leitor = leitorRepository.findByIdAndBibliotecaId(leitorId, bibliotecaId)
                .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado."));
        return enviar(leitor, tipo, titulo, mensagem);
    }

    public Notificacao enviar(Leitor leitor, Tipo tipo, String titulo, String mensagem) {
        Notificacao n = repository.save(new Notificacao(leitor.getBibliotecaId(), leitor.getId(), tipo, titulo,
                mensagem, clock.instant()));
        System.out.println("[FCM] Integração com Firebase Cloud Messaging chamada -> leitor=" + leitor.getNome()
                + " | dispositivo=" + (leitor.getFcmToken() != null ? leitor.getFcmToken() : "(não registrado)")
                + " | tipo=" + tipo + " | título=" + titulo + " | mensagem=" + mensagem);
        return n;
    }

    /** Comunicado para um leitor ou, sem leitorId, para todos os leitores da biblioteca. */
    public int comunicado(ComunicadoRequest req) {
        UUID bib = RequestContext.bibliotecaId();
        String titulo = Validacao.texto(req.titulo(), "titulo");
        String mensagem = Validacao.texto(req.mensagem(), "mensagem");
        List<Leitor> destinatarios = req.leitorId() != null
                ? List.of(leitorRepository.findByIdAndBibliotecaId(req.leitorId(), bib)
                        .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado.")))
                : leitorRepository.findByBibliotecaId(bib);
        destinatarios.forEach(l -> enviar(l, Tipo.COMUNICADO, titulo, mensagem));
        audit.registrar("COMUNICADO_ENVIADO", "Notificacao", null,
                "titulo=" + titulo + "; destinatarios=" + destinatarios.size());
        return destinatarios.size();
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(UUID leitorId, Pageable pageable) {
        UUID bib = RequestContext.bibliotecaId();
        exigirLeitor(bib, leitorId);
        return repository.findByBibliotecaIdAndLeitorIdOrderByCriadaEmDesc(bib, leitorId, pageable);
    }

    @Transactional(readOnly = true)
    public long naoLidas(UUID leitorId) {
        return repository.countByBibliotecaIdAndLeitorIdAndLidaFalse(RequestContext.bibliotecaId(), leitorId);
    }

    public Notificacao marcarLida(UUID id) {
        Notificacao n = repository.findByIdAndBibliotecaId(id, RequestContext.bibliotecaId())
                .orElseThrow(() -> ApiException.notFound("NOTIFICACAO_NAO_ENCONTRADA", "Notificação não encontrada."));
        n.setLida(true);
        return n;
    }

    private void exigirLeitor(UUID bib, UUID leitorId) {
        leitorRepository.findByIdAndBibliotecaId(leitorId, bib)
                .orElseThrow(() -> ApiException.notFound("LEITOR_NAO_ENCONTRADO", "Leitor não encontrado."));
    }
}
