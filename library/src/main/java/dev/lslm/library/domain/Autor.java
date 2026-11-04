package dev.lslm.library.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "autores", uniqueConstraints = @UniqueConstraint(columnNames = {"biblioteca_id", "nome"}))
public class Autor extends Nomeado {

    protected Autor() {
    }

    public Autor(UUID bibliotecaId, String nome) {
        super(bibliotecaId, nome);
    }
}
