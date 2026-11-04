package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leitores", uniqueConstraints = @UniqueConstraint(columnNames = {"biblioteca_id", "email"}))
public class Leitor extends TenantEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String documento;

    @Column(length = 30)
    private String telefone;

    @Column(nullable = false)
    private boolean bloqueado;

    @Column(name = "motivo_bloqueio", length = 500)
    private String motivoBloqueio;

    /** Token de dispositivo do Firebase Cloud Messaging (app Flutter ou navegador). */
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Leitor() {
    }

    public Leitor(UUID bibliotecaId, String nome, String email, String documento, String telefone) {
        super(bibliotecaId);
        atualizar(nome, email, documento, telefone);
    }

    public void atualizar(String nome, String email, String documento, String telefone) {
        this.nome = nome;
        this.email = email;
        this.documento = documento;
        this.telefone = telefone;
    }

    public void bloquear(String motivo) {
        this.bloqueado = true;
        this.motivoBloqueio = motivo;
    }

    public void desbloquear() {
        this.bloqueado = false;
        this.motivoBloqueio = null;
    }

    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getDocumento() { return documento; }
    public String getTelefone() { return telefone; }
    public boolean isBloqueado() { return bloqueado; }
    public String getMotivoBloqueio() { return motivoBloqueio; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public Instant getCriadoEm() { return criadoEm; }
}
