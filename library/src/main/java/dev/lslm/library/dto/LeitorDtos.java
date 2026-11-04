package dev.lslm.library.dto;

import dev.lslm.library.domain.Leitor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LeitorDtos {

    private LeitorDtos() {
    }

    public record LeitorRequest(String nome, String email, String documento, String telefone) {
    }

    public record BloqueioRequest(String motivo) {
    }

    public record DispositivoRequest(String fcmToken) {
    }

    public record LeitorResponse(UUID id, String nome, String email, String documento, String telefone,
                                 boolean bloqueado, String motivoBloqueio, boolean dispositivoRegistrado,
                                 Instant criadoEm) {
        public static LeitorResponse from(Leitor l) {
            return new LeitorResponse(l.getId(), l.getNome(), l.getEmail(), l.getDocumento(), l.getTelefone(),
                    l.isBloqueado(), l.getMotivoBloqueio(), l.getFcmToken() != null, l.getCriadoEm());
        }
    }

    public record AreaLeitorResponse(LeitorResponse leitor,
                                     List<CirculacaoDtos.EmprestimoResponse> emprestimosAtuais,
                                     List<CirculacaoDtos.EmprestimoResponse> historico,
                                     List<CirculacaoDtos.ReservaResponse> reservas,
                                     List<CirculacaoDtos.MultaResponse> multasPendentes,
                                     List<NotificacaoDtos.NotificacaoResponse> notificacoes,
                                     long notificacoesNaoLidas) {
    }
}
