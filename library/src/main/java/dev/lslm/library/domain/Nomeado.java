package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/** Cadastro simples por tenant com apenas um nome (autor, editora, categoria). */
@MappedSuperclass
public abstract class Nomeado extends TenantEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    protected Nomeado() {
    }

    protected Nomeado(UUID bibliotecaId, String nome) {
        super(bibliotecaId);
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
