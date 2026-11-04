package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emprestimos")
public class Emprestimo extends TenantEntity {

    public enum Status { ATIVO, DEVOLVIDO }

    @Column(name = "leitor_id", nullable = false, updatable = false)
    private UUID leitorId;

    @Column(name = "exemplar_id", nullable = false, updatable = false)
    private UUID exemplarId;

    @Column(name = "livro_id", nullable = false, updatable = false)
    private UUID livroId;

    @Column(name = "emprestado_em", nullable = false)
    private Instant emprestadoEm;

    @Column(name = "vencimento_em", nullable = false)
    private Instant vencimentoEm;

    @Column(name = "devolvido_em")
    private Instant devolvidoEm;

    @Column(nullable = false)
    private int renovacoes;

    @Column(name = "dias_atraso", nullable = false)
    private long diasAtraso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ATIVO;

    @Column(name = "aviso_vencimento_enviado", nullable = false)
    private boolean avisoVencimentoEnviado;

    @Column(name = "aviso_atraso_enviado", nullable = false)
    private boolean avisoAtrasoEnviado;

    protected Emprestimo() {
    }

    public Emprestimo(UUID bibliotecaId, UUID leitorId, UUID exemplarId, UUID livroId,
                      Instant emprestadoEm, Instant vencimentoEm) {
        super(bibliotecaId);
        this.leitorId = leitorId;
        this.exemplarId = exemplarId;
        this.livroId = livroId;
        this.emprestadoEm = emprestadoEm;
        this.vencimentoEm = vencimentoEm;
    }

    /** Dias de atraso (arredondados para cima) de um vencimento em relacao a uma data de referencia. */
    public static long calcularDiasAtraso(Instant vencimento, Instant referencia) {
        long segundos = referencia.getEpochSecond() - vencimento.getEpochSecond();
        return segundos <= 0 ? 0 : (segundos + 86_399) / 86_400;
    }

    public void devolver(Instant quando) {
        this.devolvidoEm = quando;
        this.diasAtraso = calcularDiasAtraso(vencimentoEm, quando);
        this.status = Status.DEVOLVIDO;
    }

    public void renovar(Instant novoVencimento) {
        this.vencimentoEm = novoVencimento;
        this.renovacoes++;
        this.avisoVencimentoEnviado = false;
        this.avisoAtrasoEnviado = false;
    }

    public UUID getLeitorId() { return leitorId; }
    public UUID getExemplarId() { return exemplarId; }
    public UUID getLivroId() { return livroId; }
    public Instant getEmprestadoEm() { return emprestadoEm; }
    public Instant getVencimentoEm() { return vencimentoEm; }
    public Instant getDevolvidoEm() { return devolvidoEm; }
    public int getRenovacoes() { return renovacoes; }
    public long getDiasAtraso() { return diasAtraso; }
    public Status getStatus() { return status; }
    public boolean isAvisoVencimentoEnviado() { return avisoVencimentoEnviado; }
    public void setAvisoVencimentoEnviado(boolean v) { this.avisoVencimentoEnviado = v; }
    public boolean isAvisoAtrasoEnviado() { return avisoAtrasoEnviado; }
    public void setAvisoAtrasoEnviado(boolean v) { this.avisoAtrasoEnviado = v; }
}
