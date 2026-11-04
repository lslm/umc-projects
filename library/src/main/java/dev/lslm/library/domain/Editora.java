package dev.lslm.library.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "editoras", uniqueConstraints = @UniqueConstraint(columnNames = {"biblioteca_id", "nome"}))
public class Editora extends Nomeado {

    protected Editora() {
    }

    public Editora(UUID bibliotecaId, String nome) {
        super(bibliotecaId, nome);
    }
}
