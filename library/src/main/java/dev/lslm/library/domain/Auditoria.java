package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Trilha de auditoria: quem fez o que, quando, onde (biblioteca/origem) e com qual resultado. */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "biblioteca_id")
    private UUID bibliotecaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 60)
    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(length = 60)
    private String origem;

    @Column(name = "request_id", length = 60)
    private String requestId;

    @Column(length = 1000)
    private String detalhes;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Auditoria() {
    }

    public Auditoria(UUID bibliotecaId, UUID usuarioId, String acao, String entidade, UUID entidadeId,
                     String resultado, String origem, String requestId, String detalhes, Instant criadaEm) {
        this.bibliotecaId = bibliotecaId;
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.resultado = resultado;
        this.origem = origem;
        this.requestId = requestId;
        this.detalhes = detalhes == null || detalhes.length() <= 1000 ? detalhes : detalhes.substring(0, 1000);
        this.criadaEm = criadaEm;
    }

    public UUID getId() { return id; }
    public UUID getBibliotecaId() { return bibliotecaId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public UUID getEntidadeId() { return entidadeId; }
    public String getResultado() { return resultado; }
    public String getOrigem() { return origem; }
    public String getRequestId() { return requestId; }
    public String getDetalhes() { return detalhes; }
    public Instant getCriadaEm() { return criadaEm; }
}
