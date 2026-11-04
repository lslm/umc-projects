package dev.lslm.library.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class RelatorioDtos {

    private RelatorioDtos() {
    }

    public record Circulacao(Instant de, Instant ate, long emprestimos, long devolucoes, long renovacoes,
                             long emprestimosAtivos, long emprestimosAtrasados, Map<LocalDate, Long> emprestimosPorDia) {
    }

    public record Atraso(UUID emprestimoId, UUID leitorId, String leitorNome, UUID livroId, String livroTitulo,
                         UUID exemplarId, Instant vencimentoEm, long diasAtraso, BigDecimal multaEstimada) {
    }

    public record LeitorAtivo(UUID leitorId, String leitorNome, long emprestimos) {
    }

    public record ObraEmprestada(UUID livroId, String titulo, String autor, long emprestimos) {
    }
}
