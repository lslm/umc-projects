package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacoes")
public class Notificacao extends TenantEntity {

    public enum Tipo { VENCIMENTO_PROXIMO, ATRASO, RESERVA_DISPONIVEL, COMUNICADO }

    @Column(name = "leitor_id", nullable = false, updatable = false)
    private UUID leitorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Notificacao() {
    }

    public Notificacao(UUID bibliotecaId, UUID leitorId, Tipo tipo, String titulo, String mensagem, Instant criadaEm) {
        super(bibliotecaId);
        this.leitorId = leitorId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.criadaEm = criadaEm;
    }

    public UUID getLeitorId() { return leitorId; }
    public Tipo getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    public Instant getCriadaEm() { return criadaEm; }
}
