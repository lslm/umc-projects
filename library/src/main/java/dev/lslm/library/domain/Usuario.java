package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public enum Perfil { ADMIN_PLATAFORMA, ADMIN_BIBLIOTECA, BIBLIOTECARIO, LEITOR }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Nulo apenas para o administrador da plataforma. */
    @Column(name = "biblioteca_id")
    private UUID bibliotecaId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;

    /** Leitor vinculado quando o perfil e LEITOR. */
    @Column(name = "leitor_id")
    private UUID leitorId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Usuario() {
    }

    public Usuario(UUID bibliotecaId, String nome, String email, String senhaHash, Perfil perfil, UUID leitorId) {
        this.bibliotecaId = bibliotecaId;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.leitorId = leitorId;
    }

    public UUID getId() { return id; }
    public UUID getBibliotecaId() { return bibliotecaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public UUID getLeitorId() { return leitorId; }
    public void setLeitorId(UUID leitorId) { this.leitorId = leitorId; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
}
