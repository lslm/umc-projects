package dev.lslm.library.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios puros (sem Spring, sem mocks) das regras de prazo de Emprestimo.
 * Cada teste segue as 4 fases: Setup, Exercise, Verify, Teardown.
 */
class EmprestimoTest {

    private static final Instant EMPRESTADO_EM = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant VENCIMENTO = EMPRESTADO_EM.plus(14, ChronoUnit.DAYS);

    private Emprestimo novoEmprestimo() {
        return new Emprestimo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EMPRESTADO_EM, VENCIMENTO);
    }

    // ---------- calcularDiasAtraso: funcao pura, valores de fronteira ----------

    @ParameterizedTest(name = "{0}s apos o vencimento => {1} dia(s) de atraso")
    @CsvSource({
            "-86400, 0",   // devolvido um dia antes
            "-1,     0",   // um segundo antes
            "0,      0",   // exatamente no vencimento: sem atraso
            "1,      1",   // um segundo depois: ja conta 1 dia (arredonda para cima)
            "86399,  1",
            "86400,  1",   // exatamente 24h
            "86401,  2",   // 24h + 1s: segundo dia
            "259200, 3"    // 3 dias exatos
    })
    void calcularDiasAtraso_arredondaParaCimaEZeraQuandoNaoHaAtraso(long segundosAposVencimento, long esperado) {
        // Setup
        Instant referencia = VENCIMENTO.plusSeconds(segundosAposVencimento);

        // Exercise
        long dias = Emprestimo.calcularDiasAtraso(VENCIMENTO, referencia);

        // Verify
        assertThat(dias).isEqualTo(esperado);

        // Teardown: nada a limpar (funcao pura, sem estado externo)
    }

    // ---------- devolver: transicao de estado ----------

    @Test
    void devolver_noPrazo_marcaComoDevolvidoSemAtraso() {
        // Setup
        Emprestimo emprestimo = novoEmprestimo();
        Instant devolucao = VENCIMENTO.minus(2, ChronoUnit.DAYS);

        // Exercise
        emprestimo.devolver(devolucao);

        // Verify
        assertThat(emprestimo.getStatus()).isEqualTo(Emprestimo.Status.DEVOLVIDO);
        assertThat(emprestimo.getDevolvidoEm()).isEqualTo(devolucao);
        assertThat(emprestimo.getDiasAtraso()).isZero();

        // Teardown: nada a limpar (objeto em memoria)
    }

    @Test
    void devolver_comAtraso_registraDiasDeAtrasoArredondadosParaCima() {
        // Setup
        Emprestimo emprestimo = novoEmprestimo();
        Instant devolucao = VENCIMENTO.plus(2, ChronoUnit.DAYS).plusSeconds(1); // 2 dias + 1s

        // Exercise
        emprestimo.devolver(devolucao);

        // Verify
        assertThat(emprestimo.getStatus()).isEqualTo(Emprestimo.Status.DEVOLVIDO);
        assertThat(emprestimo.getDiasAtraso()).isEqualTo(3);

        // Teardown: nada a limpar (objeto em memoria)
    }

    // ---------- renovar: transicao de estado ----------

    @Test
    void renovar_incrementaRenovacoesEEstendeOVencimento() {
        // Setup
        Emprestimo emprestimo = novoEmprestimo();
        Instant novoVencimento = VENCIMENTO.plus(14, ChronoUnit.DAYS);

        // Exercise
        emprestimo.renovar(novoVencimento);

        // Verify
        assertThat(emprestimo.getVencimentoEm()).isEqualTo(novoVencimento);
        assertThat(emprestimo.getRenovacoes()).isEqualTo(1);
        assertThat(emprestimo.getStatus()).isEqualTo(Emprestimo.Status.ATIVO);

        // Teardown: nada a limpar (objeto em memoria)
    }

    @Test
    void renovar_duasVezes_acumulaRenovacoes() {
        // Setup
        Emprestimo emprestimo = novoEmprestimo();

        // Exercise
        emprestimo.renovar(VENCIMENTO.plus(14, ChronoUnit.DAYS));
        emprestimo.renovar(VENCIMENTO.plus(28, ChronoUnit.DAYS));

        // Verify
        assertThat(emprestimo.getRenovacoes()).isEqualTo(2);

        // Teardown: nada a limpar (objeto em memoria)
    }

    @Test
    void renovar_zeraOsAvisosJaEnviadosParaOcorrerNovamenteNoNovoPrazo() {
        // Setup
        Emprestimo emprestimo = novoEmprestimo();
        emprestimo.setAvisoVencimentoEnviado(true);
        emprestimo.setAvisoAtrasoEnviado(true);

        // Exercise
        emprestimo.renovar(VENCIMENTO.plus(14, ChronoUnit.DAYS));

        // Verify
        assertThat(emprestimo.isAvisoVencimentoEnviado()).isFalse();
        assertThat(emprestimo.isAvisoAtrasoEnviado()).isFalse();

        // Teardown: nada a limpar (objeto em memoria)
    }
}
