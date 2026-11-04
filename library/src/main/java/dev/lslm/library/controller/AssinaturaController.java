package dev.lslm.library.controller;

import dev.lslm.library.dto.AssinaturaDtos.AssinaturaResponse;
import dev.lslm.library.dto.AssinaturaDtos.CheckoutRequest;
import dev.lslm.library.dto.AssinaturaDtos.CheckoutResponse;
import dev.lslm.library.dto.AssinaturaDtos.FaturamentoResponse;
import dev.lslm.library.dto.AssinaturaDtos.WebhookEvent;
import dev.lslm.library.dto.AssinaturaDtos.WebhookResponse;
import dev.lslm.library.service.AssinaturaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF17 - assinatura SaaS (Stripe simulado). */
@RestController
@RequestMapping("/api/v1/assinatura")
public class AssinaturaController {

    private final AssinaturaService service;

    public AssinaturaController(AssinaturaService service) {
        this.service = service;
    }

    @GetMapping
    public AssinaturaResponse consultar() {
        return service.consultar();
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestBody CheckoutRequest req) {
        return service.iniciarCheckout(req.plano());
    }

    @GetMapping("/faturamento")
    public FaturamentoResponse faturamento() {
        return service.acessarFaturamento();
    }

    /** Endpoint que o Stripe chamaria; identifica a biblioteca pelo corpo, nao por cabecalho. */
    @PostMapping("/webhook")
    public WebhookResponse webhook(@RequestBody WebhookEvent event) {
        return service.processarWebhook(event);
    }
}
