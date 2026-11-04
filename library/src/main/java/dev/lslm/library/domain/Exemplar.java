package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** Copia fisica de uma obra. */
@Entity
@Table(name = "exemplares", uniqueConstraints = @UniqueConstraint(columnNames = {"biblioteca_id", "codigo_barras"}))
public class Exemplar extends TenantEntity {

    public enum Estado { OTIMO, BOM, REGULAR, RUIM }

    /** EMPRESTADO e RESERVADO sao controlados pelo sistema; os demais, pela equipe. */
    public enum Situacao { DISPONIVEL, EMPRESTADO, RESERVADO, EM_MANUTENCAO, PERDIDO, BAIXADO }

    @Column(name = "livro_id", nullable = false, updatable = false)
    private UUID livroId;

    @Column(name = "codigo_barras", nullable = false, length = 50)
    private String codigoBarras;

    @Column(length = 100)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.BOM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao = Situacao.DISPONIVEL;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Exemplar() {
    }

    public Exemplar(UUID bibliotecaId, UUID livroId, String codigoBarras, String localizacao, Estado estado) {
        super(bibliotecaId);
        this.livroId = livroId;
        this.codigoBarras = codigoBarras;
        this.localizacao = localizacao;
        this.estado = estado;
    }

    public UUID getLivroId() { return livroId; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Situacao getSituacao() { return situacao; }
    public void setSituacao(Situacao situacao) { this.situacao = situacao; }
    public Instant getCriadoEm() { return criadoEm; }
}
