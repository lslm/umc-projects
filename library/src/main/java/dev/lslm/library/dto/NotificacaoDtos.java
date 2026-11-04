package dev.lslm.library.dto;

import dev.lslm.library.domain.Notificacao;

import java.time.Instant;
import java.util.UUID;

public final class NotificacaoDtos {

    private NotificacaoDtos() {
    }

    public record ComunicadoRequest(String titulo, String mensagem, UUID leitorId) {
    }

    public record NotificacaoResponse(UUID id, UUID leitorId, Notificacao.Tipo tipo, String titulo, String mensagem,
                                      boolean lida, Instant criadaEm) {
        public static NotificacaoResponse from(Notificacao n) {
            return new NotificacaoResponse(n.getId(), n.getLeitorId(), n.getTipo(), n.getTitulo(), n.getMensagem(),
                    n.isLida(), n.getCriadaEm());
        }
    }

    public record VerificacaoPrazosResponse(int avisosVencimento, int avisosAtraso, int reservasExpiradas) {
    }
}
