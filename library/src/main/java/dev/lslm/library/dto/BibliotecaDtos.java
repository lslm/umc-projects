package dev.lslm.library.dto;

import dev.lslm.library.domain.Biblioteca;
import dev.lslm.library.domain.Biblioteca.AssinaturaStatus;
import dev.lslm.library.domain.Biblioteca.Plano;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BibliotecaDtos {

    private BibliotecaDtos() {
    }

    /** Campos nulos mantem o valor atual (na atualizacao) ou usam o padrao (na criacao). */
    public record BibliotecaRequest(
            String nome,
            String email,
            Plano plano,
            Integer limiteLeitores,
            Integer limiteExemplares,
            Integer diasEmprestimo,
            Integer limiteItensEmprestimo,
            Integer limiteRenovacoes,
            BigDecimal multaDiaria,
            Integer diasAvisoVencimento,
            Integer diasRetiradaReserva
    ) {
    }

    public record BibliotecaResponse(
            UUID id,
            String nome,
            String email,
            Plano plano,
            AssinaturaStatus assinaturaStatus,
            int limiteLeitores,
            int limiteExemplares,
            int diasEmprestimo,
            int limiteItensEmprestimo,
            int limiteRenovacoes,
            BigDecimal multaDiaria,
            int diasAvisoVencimento,
            int diasRetiradaReserva,
            boolean ativa,
            Instant criadaEm
    ) {
        public static BibliotecaResponse from(Biblioteca b) {
            return new BibliotecaResponse(b.getId(), b.getNome(), b.getEmail(), b.getPlano(), b.getAssinaturaStatus(),
                    b.getLimiteLeitores(), b.getLimiteExemplares(), b.getDiasEmprestimo(),
                    b.getLimiteItensEmprestimo(), b.getLimiteRenovacoes(), b.getMultaDiaria(),
                    b.getDiasAvisoVencimento(), b.getDiasRetiradaReserva(), b.isAtiva(), b.getCriadaEm());
        }
    }
}
