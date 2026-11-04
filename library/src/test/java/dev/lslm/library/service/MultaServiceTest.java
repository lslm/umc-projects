package dev.lslm.library.service;

import dev.lslm.library.domain.Multa;
import dev.lslm.library.domain.Multa.Status;
import dev.lslm.library.exception.ApiException;
import dev.lslm.library.repository.MultaRepository;
import dev.lslm.library.support.TenantTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios de MultaService: o repositorio e a auditoria sao dubles (Mockito) e o
 * tempo e controlado por um Clock fixo. Cada teste segue as 4 fases: Setup, Exercise, Verify, Teardown.
 */
@ExtendWith(MockitoExtension.class)
class MultaServiceTest {

    private static final UUID BIBLIOTECA = UUID.randomUUID();
    private static final UUID MULTA_ID = UUID.randomUUID();
    private static final Instant CRIADA_EM = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant AGORA = Instant.parse("2026-08-12T14:30:00Z");

    @Mock
    private MultaRepository repository;

    @Mock
    private AuditService audit;

    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private MultaService service() {
        return new MultaService(repository, audit, clock);
    }

    /** Fixture: multa de 3,00 no status informado, ja "encontrada" pelo repositorio do tenant. */
    private Multa multaNoStatus(Status status) {
        Multa multa = new Multa(BIBLIOTECA, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("3.00"), CRIADA_EM);
        multa.setStatus(status);
        when(repository.findByIdAndBibliotecaId(MULTA_ID, BIBLIOTECA)).thenReturn(Optional.of(multa));
        return multa;
    }

    private void definirTenant() {
        TenantTestSupport.definirTenant(BIBLIOTECA);
    }

    @AfterEach
    void teardown() {
        // Teardown compartilhado: o RequestContext e um ThreadLocal; nao pode vazar para outros testes
        TenantTestSupport.limpar();
    }

    // ---------- buscar ----------

    @Test
    void buscar_multaInexistente_lancaNaoEncontrada() {
        // Setup
        definirTenant();
        when(repository.findByIdAndBibliotecaId(MULTA_ID, BIBLIOTECA)).thenReturn(Optional.empty());

        // Exercise + Verify
        assertThatThrownBy(() -> service().buscar(MULTA_ID))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("MULTA_NAO_ENCONTRADA");
    }

    // ---------- ajustar ----------

    @Test
    void ajustar_multaPendente_atualizaValorJustificativaEDataEAudita() {
        // Setup
        definirTenant();
        Multa multa = multaNoStatus(Status.PENDENTE);

        // Exercise
        Multa resultado = service().ajustar(MULTA_ID, new BigDecimal("1.5"), "Atraso causado por greve");

        // Verify (estado)
        assertThat(resultado.getValor()).isEqualByComparingTo("1.50");
        assertThat(resultado.getValor().scale()).isEqualTo(2);
        assertThat(resultado.getValorOriginal()).isEqualByComparingTo("3.00"); // original preservado
        assertThat(resultado.getJustificativa()).isEqualTo("Atraso causado por greve");
        assertThat(resultado.getAtualizadaEm()).isEqualTo(AGORA);
        assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
        // Verify (comportamento): a acao ficou registrada na trilha de auditoria
        verify(audit).registrar(eq("MULTA_AJUSTADA"), eq("Multa"), eq(MULTA_ID),
                contains("justificativa=Atraso causado por greve"));

        // Teardown: feito em @AfterEach
    }

    @ParameterizedTest(name = "justificativa [{0}] e rejeitada")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void ajustar_semJustificativa_lancaErroENaoAlteraNemAudita(String justificativa) {
        // Setup
        definirTenant();
        Multa multa = multaNoStatus(Status.PENDENTE);

        // Exercise
        Throwable erro = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().ajustar(MULTA_ID, new BigDecimal("1.00"), justificativa));

        // Verify
        assertThat(erro).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("justificativa");
        assertThat(multa.getValor()).isEqualByComparingTo("3.00");
        verify(audit, never()).registrar(anyString(), anyString(), any(), any());

        // Teardown: feito em @AfterEach
    }

    @Test
    void ajustar_valorNegativo_lancaErroENaoAltera() {
        // Setup
        definirTenant();
        Multa multa = multaNoStatus(Status.PENDENTE);

        // Exercise
        Throwable erro = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().ajustar(MULTA_ID, new BigDecimal("-0.01"), "Correcao"));

        // Verify
        assertThat(erro).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negativo");
        assertThat(multa.getValor()).isEqualByComparingTo("3.00");
        verify(audit, never()).registrar(anyString(), anyString(), any(), any());

        // Teardown: feito em @AfterEach
    }

    @ParameterizedTest(name = "multa {0} nao pode ser ajustada")
    @EnumSource(value = Status.class, names = {"PAGA", "ISENTA"})
    void ajustar_multaNaoPendente_lancaConflito(Status statusAtual) {
        // Setup
        definirTenant();
        Multa multa = multaNoStatus(statusAtual);

        // Exercise
        Throwable erro = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().ajustar(MULTA_ID, new BigDecimal("1.00"), "Tentativa"));

        // Verify
        assertThat(erro).isInstanceOf(ApiException.class);
        assertThat(((ApiException) erro).getCode()).isEqualTo("MULTA_NAO_PENDENTE");
        assertThat(multa.getValor()).isEqualByComparingTo("3.00");
        assertThat(multa.getStatus()).isEqualTo(statusAtual);
        verify(audit, never()).registrar(anyString(), anyString(), any(), any());

        // Teardown: feito em @AfterEach
    }

    // ---------- isentar ----------

    @Test
    void isentar_multaPendente_mudaStatusParaIsentaERegistraJustificativa() {
        // Setup
        definirTenant();
        multaNoStatus(Status.PENDENTE);

        // Exercise
        Multa resultado = service().isentar(MULTA_ID, "Leitor com deficiencia de locomocao");

        // Verify
        assertThat(resultado.getStatus()).isEqualTo(Status.ISENTA);
        assertThat(resultado.getJustificativa()).isEqualTo("Leitor com deficiencia de locomocao");
        assertThat(resultado.getAtualizadaEm()).isEqualTo(AGORA);
        verify(audit).registrar(eq("MULTA_ISENTADA"), eq("Multa"), eq(MULTA_ID), contains("valor=3.00"));

        // Teardown: feito em @AfterEach
    }

    @ParameterizedTest(name = "justificativa [{0}] e rejeitada")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void isentar_semJustificativa_naoMudaStatus(String justificativa) {
        // Setup
        definirTenant();
        Multa multa = multaNoStatus(Status.PENDENTE);

        // Exercise
        Throwable erro = org.assertj.core.api.Assertions.catchThrowable(
                () -> service().isentar(MULTA_ID, justificativa));

        // Verify
        assertThat(erro).isInstanceOf(IllegalArgumentException.class);
        assertThat(multa.getStatus()).isEqualTo(Status.PENDENTE);
        verify(audit, never()).registrar(anyString(), anyString(), any(), any());

        // Teardown: feito em @AfterEach
    }

    // ---------- pagar ----------

    @Test
    void pagar_semJustificativa_usaTextoPadraoEMarcaComoPaga() {
        // Setup
        definirTenant();
        multaNoStatus(Status.PENDENTE);

        // Exercise
        Multa resultado = service().pagar(MULTA_ID, null);

        // Verify
        assertThat(resultado.getStatus()).isEqualTo(Status.PAGA);
        assertThat(resultado.getJustificativa()).isEqualTo("Pagamento registrado");
        assertThat(resultado.getAtualizadaEm()).isEqualTo(AGORA);
        verify(audit).registrar(eq("MULTA_PAGA"), eq("Multa"), eq(MULTA_ID), contains("valor=3.00"));

        // Teardown: feito em @AfterEach
    }

    @Test
    void pagar_comJustificativa_guardaOTextoInformadoSemEspacosNasPontas() {
        // Setup
        definirTenant();
        multaNoStatus(Status.PENDENTE);

        // Exercise
        Multa resultado = service().pagar(MULTA_ID, "  Pago no balcao em dinheiro  ");

        // Verify
        assertThat(resultado.getJustificativa()).isEqualTo("Pago no balcao em dinheiro");

        // Teardown: feito em @AfterEach
    }

    @Test
    void pagar_multaJaPaga_lancaConflito() {
        // Setup
        definirTenant();
        multaNoStatus(Status.PAGA);

        // Exercise
        Throwable erro = org.assertj.core.api.Assertions.catchThrowable(() -> service().pagar(MULTA_ID, null));

        // Verify
        assertThat(erro).isInstanceOf(ApiException.class);
        assertThat(((ApiException) erro).getCode()).isEqualTo("MULTA_NAO_PENDENTE");
        verify(audit, never()).registrar(anyString(), anyString(), any(), any());

        // Teardown: feito em @AfterEach
    }
}
