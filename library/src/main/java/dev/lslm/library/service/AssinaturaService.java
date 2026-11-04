package dev.lslm.library.service;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Biblioteca.AssinaturaStatus;
import dev.lslm.library.domain.Biblioteca.Plano;
import dev.lslm.library.domain.EventoProcessado;
import dev.lslm.library.dto.AssinaturaDtos.AssinaturaResponse;
import dev.lslm.library.dto.AssinaturaDtos.CheckoutResponse;
import dev.lslm.library.dto.AssinaturaDtos.FaturamentoResponse;
import dev.lslm.library.dto.AssinaturaDtos.PlanoInfo;
import dev.lslm.library.dto.AssinaturaDtos.WebhookEvent;
import dev.lslm.library.dto.AssinaturaDtos.WebhookResponse;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.BibliotecaRepository;
import dev.lslm.library.repository.EventoProcessadoRepository;
import dev.lslm.library.repository.ExemplarRepository;
import dev.lslm.library.repository.LeitorRepository;
import dev.lslm.library.support.RequestContext;
import dev.lslm.library.support.Validacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.UUID;

/** Assinatura SaaS (RF17). O Stripe e simulado: as chamadas apenas imprimem uma mensagem. */
@Service
@Transactional
public class AssinaturaService {

    private final BibliotecaRepository bibliotecaRepository;
    private final LeitorRepository leitorRepository;
    private final ExemplarRepository exemplarRepository;
    private final EventoProcessadoRepository eventoRepository;
    private final AuditService audit;
    private final Clock clock;

    public AssinaturaService(BibliotecaRepository bibliotecaRepository, LeitorRepository leitorRepository,
                             ExemplarRepository exemplarRepository, EventoProcessadoRepository eventoRepository,
                             AuditService audit, Clock clock) {
        this.bibliotecaRepository = bibliotecaRepository;
        this.leitorRepository = leitorRepository;
        this.exemplarRepository = exemplarRepository;
        this.eventoRepository = eventoRepository;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AssinaturaResponse consultar() {
        UUID bib = RequestContext.bibliotecaId();
        Biblioteca b = biblioteca(bib);
        return new AssinaturaResponse(bib, b.getPlano(), b.getAssinaturaStatus(), b.getPlanoPendente(),
                b.getLimiteLeitores(), b.getLimiteExemplares(), leitorRepository.countByBibliotecaId(bib),
                exemplarRepository.countByBibliotecaId(bib),
                Arrays.stream(Plano.values())
                        .map(p -> new PlanoInfo(p, p.getLimiteLeitores(), p.getLimiteExemplares())).toList());
    }

    public CheckoutResponse iniciarCheckout(Plano plano) {
        UUID bib = RequestContext.bibliotecaId();
        Validacao.obrigatorio(plano, "plano");
        Biblioteca b = biblioteca(bib);
        String sessionId = "cs_test_" + UUID.randomUUID().toString().replace("-", "");
        System.out.println("[STRIPE] Integração com Stripe chamada -> criar Checkout Session | biblioteca=" + bib
                + " | plano=" + plano + " | sessionId=" + sessionId);
        b.setPlanoPendente(plano);
        b.setAssinaturaStatus(AssinaturaStatus.PENDENTE);
        audit.registrar("CHECKOUT_INICIADO", "Biblioteca", bib, "plano=" + plano + "; sessionId=" + sessionId);
        return new CheckoutResponse(sessionId, "https://checkout.stripe.com/c/pay/" + sessionId, plano,
                b.getAssinaturaStatus());
    }

    public FaturamentoResponse acessarFaturamento() {
        UUID bib = RequestContext.bibliotecaId();
        Biblioteca b = biblioteca(bib);
        System.out.println("[STRIPE] Integração com Stripe chamada -> criar sessão do portal de faturamento | biblioteca=" + bib);
        audit.registrar("FATURAMENTO_ACESSADO", "Biblioteca", bib, null);
        return new FaturamentoResponse("https://billing.stripe.com/p/session/" + UUID.randomUUID(),
                b.getAssinaturaStatus(), b.getPlano());
    }

    /**
     * Webhook idempotente (RNF12): o id do evento e gravado e um evento repetido nao tem efeito.
     * A validacao da assinatura (Stripe-Signature) nao e feita, pois nao ha integracao real.
     */
    public WebhookResponse processarWebhook(WebhookEvent ev) {
        String id = Validacao.texto(ev.id(), "id");
        String tipo = Validacao.texto(ev.type(), "type");
        System.out.println("[STRIPE] Webhook recebido -> evento=" + id + " | tipo=" + tipo);

        if (eventoRepository.existsById(id)) {
            return new WebhookResponse(false, true, "Evento já processado anteriormente.");
        }
        UUID bibId = Validacao.obrigatorio(ev.bibliotecaId(), "bibliotecaId");
        Biblioteca b = biblioteca(bibId);
        eventoRepository.save(new EventoProcessado(id, tipo, clock.instant()));

        switch (tipo) {
            case "checkout.session.completed" -> {
                Plano plano = ev.plano() != null ? ev.plano() : b.getPlanoPendente();
                if (plano == null) {
                    throw ApiException.badRequest("PLANO_NAO_INFORMADO", "Nenhum plano informado ou pendente.");
                }
                b.aplicarPlano(plano);
                b.setPlanoPendente(null);
                b.setAssinaturaStatus(AssinaturaStatus.ATIVA);
            }
            case "invoice.paid" -> b.setAssinaturaStatus(AssinaturaStatus.ATIVA);
            case "invoice.payment_failed" -> b.setAssinaturaStatus(AssinaturaStatus.INADIMPLENTE);
            case "customer.subscription.deleted" -> b.setAssinaturaStatus(AssinaturaStatus.CANCELADA);
            default -> {
                return new WebhookResponse(false, false, "Tipo de evento ignorado: " + tipo);
            }
        }
        audit.registrarPara(bibId, "ASSINATURA_" + tipo.toUpperCase().replace('.', '_'), "Biblioteca", bibId,
                "evento=" + id + "; status=" + b.getAssinaturaStatus() + "; plano=" + b.getPlano());
        return new WebhookResponse(true, false, "Evento processado.");
    }

    private Biblioteca biblioteca(UUID id) {
        return bibliotecaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BIBLIOTECA_NAO_ENCONTRADA", "Biblioteca não encontrada."));
    }
}
