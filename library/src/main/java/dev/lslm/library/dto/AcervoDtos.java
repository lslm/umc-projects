package dev.lslm.library.dto;

import dev.lslm.library.domain.Exemplar;
import dev.lslm.library.domain.Exemplar.Estado;
import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.domain.Nomeado;

import java.time.Instant;
import java.util.UUID;

public final class AcervoDtos {

    private AcervoDtos() {
    }

    public record NomeadoRequest(String nome) {
    }

    public record NomeadoResponse(UUID id, String nome) {
        public static NomeadoResponse from(Nomeado n) {
            return new NomeadoResponse(n.getId(), n.getNome());
        }
    }

    public record ExemplarRequest(UUID livroId, String codigoBarras, String localizacao, Estado estado,
                                  Situacao situacao) {
    }

    public record ExemplarResponse(UUID id, UUID livroId, String codigoBarras, String localizacao, Estado estado,
                                   Situacao situacao, Instant criadoEm) {
        public static ExemplarResponse from(Exemplar e) {
            return new ExemplarResponse(e.getId(), e.getLivroId(), e.getCodigoBarras(), e.getLocalizacao(),
                    e.getEstado(), e.getSituacao(), e.getCriadoEm());
        }
    }

    public record CatalogoItem(BookResponse livro, long totalExemplares, long exemplaresDisponiveis,
                               boolean disponivel) {
    }
}
