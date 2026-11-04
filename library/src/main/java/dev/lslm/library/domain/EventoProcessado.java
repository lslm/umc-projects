package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Identificador de evento de webhook ja tratado (idempotencia, RNF12). */
@Entity
@Table(name = "eventos_processados")
public class EventoProcessado {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    protected EventoProcessado() {
    }

    public EventoProcessado(String id, String tipo, Instant processadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.processadoEm = processadoEm;
    }

    public String getId() { return id; }
}
