package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/** Base das entidades de negocio: toda linha pertence a uma biblioteca (tenant). */
@MappedSuperclass
public abstract class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "biblioteca_id", nullable = false, updatable = false)
    private UUID bibliotecaId;

    protected TenantEntity() {
    }

    protected TenantEntity(UUID bibliotecaId) {
        this.bibliotecaId = bibliotecaId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBibliotecaId() {
        return bibliotecaId;
    }
}
