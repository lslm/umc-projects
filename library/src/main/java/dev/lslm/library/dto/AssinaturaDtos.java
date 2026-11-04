package dev.lslm.library.dto;

import dev.lslm.library.domain.Biblioteca.AssinaturaStatus;
import dev.lslm.library.domain.Biblioteca.Plano;

import java.util.List;
import java.util.UUID;

public final class AssinaturaDtos {

    private AssinaturaDtos() {
    }

    public record PlanoInfo(Plano plano, int limiteLeitores, int limiteExemplares) {
    }

    public record AssinaturaResponse(UUID bibliotecaId, Plano plano, AssinaturaStatus status, Plano planoPendente,
                                     int limiteLeitores, int limiteExemplares, long leitoresCadastrados,
                                     long exemplaresCadastrados, List<PlanoInfo> planosDisponiveis) {
    }

    public record CheckoutRequest(Plano plano) {
    }

    public record CheckoutResponse(String sessionId, String checkoutUrl, Plano plano, AssinaturaStatus status) {
    }

    public record FaturamentoResponse(String portalUrl, AssinaturaStatus status, Plano plano) {
    }

    /** Evento simplificado no estilo Stripe: id, type, e a biblioteca/plano afetados. */
    public record WebhookEvent(String id, String type, UUID bibliotecaId, Plano plano) {
    }

    public record WebhookResponse(boolean processado, boolean duplicado, String mensagem) {
    }
}
