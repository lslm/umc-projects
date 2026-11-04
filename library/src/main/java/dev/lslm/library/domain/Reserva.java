package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Reserva de uma obra: AGUARDANDO na fila, DISPONIVEL quando um exemplar foi separado para o leitor. */
@Entity
@Table(name = "reservas")
public class Reserva extends TenantEntity {

    public enum Status { AGUARDANDO, DISPONIVEL, ATENDIDA, CANCELADA, EXPIRADA }

    @Column(name = "livro_id", nullable = false, updatable = false)
    private UUID livroId;

    @Column(name = "leitor_id", nullable = false, updatable = false)
    private UUID leitorId;

    @Column(name = "exemplar_id")
    private UUID exemplarId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AGUARDANDO;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "disponivel_ate")
    private Instant disponivelAte;

    protected Reserva() {
    }

    public Reserva(UUID bibliotecaId, UUID livroId, UUID leitorId, Instant criadaEm) {
        super(bibliotecaId);
        this.livroId = livroId;
        this.leitorId = leitorId;
        this.criadaEm = criadaEm;
    }

    public UUID getLivroId() { return livroId; }
    public UUID getLeitorId() { return leitorId; }
    public UUID getExemplarId() { return exemplarId; }
    public void setExemplarId(UUID exemplarId) { this.exemplarId = exemplarId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getDisponivelAte() { return disponivelAte; }
    public void setDisponivelAte(Instant disponivelAte) { this.disponivelAte = disponivelAte; }
}
