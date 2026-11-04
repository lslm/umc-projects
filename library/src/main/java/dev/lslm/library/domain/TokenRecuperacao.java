package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_recuperacao_senha")
public class TokenRecuperacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean usado;

    protected TokenRecuperacao() {
    }

    public TokenRecuperacao(UUID usuarioId, String token, Instant expiraEm) {
        this.usuarioId = usuarioId;
        this.token = token;
        this.expiraEm = expiraEm;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isUsado() { return usado; }
    public void marcarUsado() { this.usado = true; }
}
