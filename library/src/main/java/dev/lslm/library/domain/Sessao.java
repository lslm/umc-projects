package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Sessao de login: access token de vida curta e refresh token revogavel. */
@Entity
@Table(name = "sessoes")
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "access_token", nullable = false, unique = true)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, unique = true)
    private String refreshToken;

    @Column(name = "access_expira_em", nullable = false)
    private Instant accessExpiraEm;

    @Column(name = "refresh_expira_em", nullable = false)
    private Instant refreshExpiraEm;

    @Column(nullable = false)
    private boolean revogada;

    protected Sessao() {
    }

    public Sessao(UUID usuarioId, String accessToken, String refreshToken, Instant accessExpiraEm, Instant refreshExpiraEm) {
        this.usuarioId = usuarioId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiraEm = accessExpiraEm;
        this.refreshExpiraEm = refreshExpiraEm;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getAccessExpiraEm() { return accessExpiraEm; }
    public Instant getRefreshExpiraEm() { return refreshExpiraEm; }
    public boolean isRevogada() { return revogada; }
    public void revogar() { this.revogada = true; }
}
