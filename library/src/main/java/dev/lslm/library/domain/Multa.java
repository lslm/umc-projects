package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "multas")
public class Multa extends TenantEntity {

    public enum Status { PENDENTE, PAGA, ISENTA }

    @Column(name = "emprestimo_id", nullable = false, updatable = false)
    private UUID emprestimoId;

    @Column(name = "leitor_id", nullable = false, updatable = false)
    private UUID leitorId;

    @Column(name = "valor_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOriginal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;

    @Column(length = 500)
    private String justificativa;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected Multa() {
    }

    public Multa(UUID bibliotecaId, UUID emprestimoId, UUID leitorId, BigDecimal valor, Instant criadaEm) {
        super(bibliotecaId);
        this.emprestimoId = emprestimoId;
        this.leitorId = leitorId;
        this.valorOriginal = valor;
        this.valor = valor;
        this.criadaEm = criadaEm;
        this.atualizadaEm = criadaEm;
    }

    public UUID getEmprestimoId() { return emprestimoId; }
    public UUID getLeitorId() { return leitorId; }
    public BigDecimal getValorOriginal() { return valorOriginal; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(Instant atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
