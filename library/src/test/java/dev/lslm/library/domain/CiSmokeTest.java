package dev.lslm.library.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste simples apenas para validar que o workflow de CI executa os testes em PRs.
 * Para ver o CI falhar, troque o valor esperado abaixo.
 */
class CiSmokeTest {

    @Test
    void ciDeveExecutarTestesNoPullRequest() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
