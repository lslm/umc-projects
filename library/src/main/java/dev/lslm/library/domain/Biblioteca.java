package dev.lslm.library.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Biblioteca contratante (tenant) com plano, configuracoes e limites proprios. */
@Entity
@Table(name = "bibliotecas")
public class Biblioteca {

    public enum Plano {
        BASICO(100, 1_000), PROFISSIONAL(1_000, 10_000), ENTERPRISE(1_000_000, 10_000_000);

        private final int limiteLeitores;
        private final int limiteExemplares;

        Plano(int limiteLeitores, int limiteExemplares) {
            this.limiteLeitores = limiteLeitores;
            this.limiteExemplares = limiteExemplares;
        }

        public int getLimiteLeitores() {
            return limiteLeitores;
        }

        public int getLimiteExemplares() {
            return limiteExemplares;
        }
    }

    public enum AssinaturaStatus { SEM_ASSINATURA, PENDENTE, ATIVA, INADIMPLENTE, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano = Plano.BASICO;

    @Enumerated(EnumType.STRING)
    @Column(name = "assinatura_status", nullable = false, length = 20)
    private AssinaturaStatus assinaturaStatus = AssinaturaStatus.SEM_ASSINATURA;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_pendente", length = 20)
    private Plano planoPendente;

    @Column(name = "limite_leitores", nullable = false)
    private int limiteLeitores;

    @Column(name = "limite_exemplares", nullable = false)
    private int limiteExemplares;

    @Column(name = "dias_emprestimo", nullable = false)
    private int diasEmprestimo = 14;

    @Column(name = "limite_itens_emprestimo", nullable = false)
    private int limiteItensEmprestimo = 5;

    @Column(name = "limite_renovacoes", nullable = false)
    private int limiteRenovacoes = 2;

    @Column(name = "multa_diaria", nullable = false, precision = 10, scale = 2)
    private BigDecimal multaDiaria = new BigDecimal("1.00");

    @Column(name = "dias_aviso_vencimento", nullable = false)
    private int diasAvisoVencimento = 2;

    @Column(name = "dias_retirada_reserva", nullable = false)
    private int diasRetiradaReserva = 3;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm = Instant.now();

    protected Biblioteca() {
    }

    public Biblioteca(String nome, Plano plano) {
        this.nome = nome;
        aplicarPlano(plano);
    }

    /** Define o plano e redefine os limites de leitores/exemplares para os padroes dele. */
    public void aplicarPlano(Plano plano) {
        this.plano = plano;
        this.limiteLeitores = plano.getLimiteLeitores();
        this.limiteExemplares = plano.getLimiteExemplares();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Plano getPlano() { return plano; }
    public AssinaturaStatus getAssinaturaStatus() { return assinaturaStatus; }
    public void setAssinaturaStatus(AssinaturaStatus s) { this.assinaturaStatus = s; }
    public Plano getPlanoPendente() { return planoPendente; }
    public void setPlanoPendente(Plano p) { this.planoPendente = p; }
    public int getLimiteLeitores() { return limiteLeitores; }
    public void setLimiteLeitores(int v) { this.limiteLeitores = v; }
    public int getLimiteExemplares() { return limiteExemplares; }
    public void setLimiteExemplares(int v) { this.limiteExemplares = v; }
    public int getDiasEmprestimo() { return diasEmprestimo; }
    public void setDiasEmprestimo(int v) { this.diasEmprestimo = v; }
    public int getLimiteItensEmprestimo() { return limiteItensEmprestimo; }
    public void setLimiteItensEmprestimo(int v) { this.limiteItensEmprestimo = v; }
    public int getLimiteRenovacoes() { return limiteRenovacoes; }
    public void setLimiteRenovacoes(int v) { this.limiteRenovacoes = v; }
    public BigDecimal getMultaDiaria() { return multaDiaria; }
    public void setMultaDiaria(BigDecimal v) { this.multaDiaria = v; }
    public int getDiasAvisoVencimento() { return diasAvisoVencimento; }
    public void setDiasAvisoVencimento(int v) { this.diasAvisoVencimento = v; }
    public int getDiasRetiradaReserva() { return diasRetiradaReserva; }
    public void setDiasRetiradaReserva(int v) { this.diasRetiradaReserva = v; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public Instant getCriadaEm() { return criadaEm; }
}
