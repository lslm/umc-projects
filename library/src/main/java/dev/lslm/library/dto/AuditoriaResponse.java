package dev.lslm.library.dto;

import dev.lslm.library.domain.Auditoria;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaResponse(UUID id, UUID bibliotecaId, UUID usuarioId, String acao, String entidade,
                                UUID entidadeId, String resultado, String origem, String requestId,
                                String detalhes, Instant criadaEm) {

    public static AuditoriaResponse from(Auditoria a) {
        return new AuditoriaResponse(a.getId(), a.getBibliotecaId(), a.getUsuarioId(), a.getAcao(), a.getEntidade(),
                a.getEntidadeId(), a.getResultado(), a.getOrigem(), a.getRequestId(), a.getDetalhes(), a.getCriadaEm());
    }
}
