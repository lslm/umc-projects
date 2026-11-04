package dev.lslm.library.dto;

import dev.lslm.library.domain.Emprestimo;
import dev.lslm.library.domain.Multa;
import dev.lslm.library.domain.Reserva;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CirculacaoDtos {

    private CirculacaoDtos() {
    }

    public record EmprestimoRequest(UUID leitorId, UUID exemplarId) {
    }

    /** devolvidoEm e opcional (padrao: agora); util para registrar devolucoes retroativas. */
    public record DevolucaoRequest(Instant devolvidoEm) {
    }

    public record EmprestimoResponse(UUID id, UUID leitorId, UUID livroId, String livroTitulo, UUID exemplarId,
                                     Instant emprestadoEm, Instant vencimentoEm, Instant devolvidoEm,
                                     int renovacoes, Emprestimo.Status status, long diasAtraso) {
    }

    public record DevolucaoResponse(EmprestimoResponse emprestimo, long diasAtraso, MultaResponse multa,
                                    String situacaoExemplar, UUID reservaAtendidaId) {
    }

    public record ReservaRequest(UUID livroId, UUID leitorId) {
    }

    public record ReservaResponse(UUID id, UUID livroId, String livroTitulo, UUID leitorId, Reserva.Status status,
                                  int posicao, UUID exemplarId, Instant criadaEm, Instant disponivelAte) {
    }

    public record MultaResponse(UUID id, UUID emprestimoId, UUID leitorId, BigDecimal valorOriginal, BigDecimal valor,
                                Multa.Status status, String justificativa, Instant criadaEm, Instant atualizadaEm) {
        public static MultaResponse from(Multa m) {
            return new MultaResponse(m.getId(), m.getEmprestimoId(), m.getLeitorId(), m.getValorOriginal(),
                    m.getValor(), m.getStatus(), m.getJustificativa(), m.getCriadaEm(), m.getAtualizadaEm());
        }
    }

    public record AjusteMultaRequest(BigDecimal valor, String justificativa) {
    }

    public record JustificativaRequest(String justificativa) {
    }
}
