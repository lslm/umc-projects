package dev.lslm.library.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxos de ponta a ponta: emprestimo, fila de reservas, devolucao com multa, renovacao,
 * isolamento entre bibliotecas, login e webhook idempotente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CirculacaoIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    @LocalServerPort
    private int port;

    private RestClient plataforma;
    private RestClient client;
    private String bibliotecaId;

    @BeforeEach
    void setUp() {
        plataforma = cliente(null);
        bibliotecaId = (String) post(plataforma, "/api/v1/bibliotecas", Map.of("nome", "Biblioteca " + System.nanoTime())).get("id");
        client = cliente(bibliotecaId);
    }

    @Test
    void fluxoCompleto_emprestimoReservaDevolucaoComMultaEFila() {
        String livro = (String) post(client, "/api/v1/livros", livroBody("ISBN-" + System.nanoTime())).get("id");
        String exemplar = (String) post(client, "/api/v1/exemplares",
                Map.of("livroId", livro, "codigoBarras", "EX-" + System.nanoTime(), "localizacao", "A1")).get("id");
        String ana = leitor("ana");
        String bruno = leitor("bruno");

        // Empréstimo
        Map<String, Object> emprestimo = post(client, "/api/v1/emprestimos", Map.of("leitorId", ana, "exemplarId", exemplar));
        assertThat(emprestimo.get("status")).isEqualTo("ATIVO");
        assertThat(get(client, "/api/v1/exemplares/" + exemplar).get("situacao")).isEqualTo("EMPRESTADO");

        // Sem exemplar disponível: empréstimo recusado e reserva permitida
        ResponseEntity<Map<String, Object>> recusado = client.post().uri("/api/v1/emprestimos")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("leitorId", bruno, "exemplarId", exemplar))
                .retrieve().toEntity(MAP);
        assertThat(recusado.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(recusado.getBody().get("code")).isEqualTo("LIVRO_INDISPONIVEL");

        Map<String, Object> reserva = post(client, "/api/v1/reservas", Map.of("livroId", livro, "leitorId", bruno));
        assertThat(reserva.get("status")).isEqualTo("AGUARDANDO");
        assertThat(reserva.get("posicao")).isEqualTo(1);

        // Renovação bloqueada por reserva pendente
        ResponseEntity<Map<String, Object>> renovacao = client.post()
                .uri("/api/v1/emprestimos/" + emprestimo.get("id") + "/renovacao").retrieve().toEntity(MAP);
        assertThat(renovacao.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(renovacao.getBody().get("code")).isEqualTo("RESERVA_PENDENTE");

        // Devolução com 3 dias de atraso: multa de 3 x 1,00 e exemplar separado para a reserva
        Instant devolucao = Instant.parse((String) emprestimo.get("vencimentoEm")).plus(3, ChronoUnit.DAYS).minusSeconds(60);
        Map<String, Object> dev = post(client, "/api/v1/emprestimos/" + emprestimo.get("id") + "/devolucao",
                Map.of("devolvidoEm", devolucao.toString()));
        assertThat(((Number) dev.get("diasAtraso")).longValue()).isEqualTo(3);
        assertThat(((Map<?, ?>) dev.get("multa")).get("valor").toString()).isEqualTo("3.0");
        assertThat(dev.get("situacaoExemplar")).isEqualTo("RESERVADO");
        assertThat(get(client, "/api/v1/reservas/" + reserva.get("id")).get("status")).isEqualTo("DISPONIVEL");

        // Bruno foi notificado e tem multa? (não: a multa é da Ana, que fica bloqueada de novos empréstimos)
        Map<String, Object> areaBruno = get(client, "/api/v1/leitores/" + bruno + "/area");
        assertThat((List<?>) areaBruno.get("notificacoes")).hasSize(1);
        Map<String, Object> multa = ((List<Map<String, Object>>) get(client, "/api/v1/leitores/" + ana + "/area")
                .get("multasPendentes")).get(0);

        ResponseEntity<Map<String, Object>> semJustificativa = client.post()
                .uri("/api/v1/multas/" + multa.get("id") + "/isencao")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of()).retrieve().toEntity(MAP);
        assertThat(semJustificativa.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> isenta = post(client, "/api/v1/multas/" + multa.get("id") + "/isencao",
                Map.of("justificativa", "Greve de transporte"));
        assertThat(isenta.get("status")).isEqualTo("ISENTA");

        // Empréstimo para quem reservou
        Map<String, Object> segundo = post(client, "/api/v1/emprestimos", Map.of("leitorId", bruno, "exemplarId", exemplar));
        assertThat(segundo.get("status")).isEqualTo("ATIVO");
        assertThat(get(client, "/api/v1/reservas/" + reserva.get("id")).get("status")).isEqualTo("ATENDIDA");

        // Renovação permitida agora que não há fila
        Map<String, Object> renovado = post(client, "/api/v1/emprestimos/" + segundo.get("id") + "/renovacao", null);
        assertThat(renovado.get("renovacoes")).isEqualTo(1);

        // Relatórios e auditoria
        Map<String, Object> circ = get(client, "/api/v1/relatorios/circulacao");
        assertThat(((Number) circ.get("emprestimos")).longValue()).isEqualTo(2);
        assertThat(client.get().uri("/api/v1/relatorios/obras-mais-emprestadas").retrieve().toEntity(String.class).getBody()).contains("Clean Code");
        Map<String, Object> auditoria = get(client, "/api/v1/auditoria?acao=MULTA_ISENTADA");
        assertThat(((Number) auditoria.get("total")).longValue()).isEqualTo(1);
    }

    @Test
    void isolamento_outraBibliotecaNaoEnxergaDados() {
        String livro = (String) post(client, "/api/v1/livros", livroBody("ISO-" + System.nanoTime())).get("id");

        String outra = (String) post(plataforma, "/api/v1/bibliotecas", Map.of("nome", "Outra " + System.nanoTime())).get("id");
        ResponseEntity<Map<String, Object>> r = cliente(outra).get().uri("/api/v1/livros/" + livro).retrieve().toEntity(MAP);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map<String, Object>> semTenant = plataforma.get().uri("/api/v1/livros").retrieve().toEntity(MAP);
        assertThat(semTenant.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(semTenant.getBody().get("code")).isEqualTo("BIBLIOTECA_NAO_INFORMADA");
    }

    @Test
    void catalogo_buscaPorTituloEDisponibilidade() {
        String livro = (String) post(client, "/api/v1/livros", livroBody("CAT-" + System.nanoTime())).get("id");
        post(client, "/api/v1/exemplares", Map.of("livroId", livro, "codigoBarras", "C-" + System.nanoTime()));

        Map<String, Object> disponiveis = get(client, "/api/v1/catalogo/busca?q=clean&disponivel=true");
        assertThat(((Number) disponiveis.get("total")).longValue()).isEqualTo(1);
        Map<String, Object> indisponiveis = get(client, "/api/v1/catalogo/busca?q=clean&disponivel=false");
        assertThat(((Number) indisponiveis.get("total")).longValue()).isZero();
    }

    @Test
    void autenticacao_loginRefreshERecuperacaoDeSenha() {
        post(client, "/api/v1/usuarios", Map.of("nome", "Maria", "email", "maria" + System.nanoTime() + "@x.com",
                "senha", "senha-forte-1", "perfil", "BIBLIOTECARIO"));
        String email = "joao" + System.nanoTime() + "@x.com";
        post(client, "/api/v1/usuarios", Map.of("nome", "João", "email", email, "senha", "senha-forte-1", "perfil", "ADMIN_BIBLIOTECA"));

        Map<String, Object> login = post(plataforma, "/api/v1/auth/login", Map.of("email", email, "senha", "senha-forte-1"));
        assertThat(login.get("accessToken")).isNotNull();

        ResponseEntity<Map<String, Object>> errada = plataforma.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("email", email, "senha", "errada"))
                .retrieve().toEntity(MAP);
        assertThat(errada.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Map<String, Object> novo = post(plataforma, "/api/v1/auth/refresh", Map.of("refreshToken", login.get("refreshToken")));
        assertThat(novo.get("refreshToken")).isNotEqualTo(login.get("refreshToken"));
        // o refresh token antigo foi rotacionado
        ResponseEntity<Map<String, Object>> reuso = plataforma.post().uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("refreshToken", login.get("refreshToken")))
                .retrieve().toEntity(MAP);
        assertThat(reuso.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map<String, Object>> me = plataforma.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + novo.get("accessToken")).retrieve().toEntity(MAP);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email")).isEqualTo(email);

        ResponseEntity<Map<String, Object>> esqueci = plataforma.post().uri("/api/v1/auth/esqueci-senha")
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("email", email)).retrieve().toEntity(MAP);
        assertThat(esqueci.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void assinatura_checkoutEWebhookIdempotente() {
        Map<String, Object> checkout = post(client, "/api/v1/assinatura/checkout", Map.of("plano", "PROFISSIONAL"));
        assertThat(checkout.get("status")).isEqualTo("PENDENTE");

        Map<String, Object> evento = Map.of("id", "evt_" + System.nanoTime(), "type", "checkout.session.completed",
                "bibliotecaId", bibliotecaId);
        assertThat(post(plataforma, "/api/v1/assinatura/webhook", evento).get("processado")).isEqualTo(true);
        Map<String, Object> repetido = post(plataforma, "/api/v1/assinatura/webhook", evento);
        assertThat(repetido.get("duplicado")).isEqualTo(true);

        Map<String, Object> assinatura = get(client, "/api/v1/assinatura");
        assertThat(assinatura.get("status")).isEqualTo("ATIVA");
        assertThat(assinatura.get("plano")).isEqualTo("PROFISSIONAL");
        assertThat(assinatura.get("limiteLeitores")).isEqualTo(1000);
    }

    // ---------- helpers ----------

    private RestClient cliente(String tenant) {
        RestClient.Builder b = RestClient.builder().baseUrl("http://localhost:" + port);
        if (tenant != null) {
            b.defaultHeader("X-Biblioteca-Id", tenant);
        }
        return b.defaultStatusHandler(status -> true, (req, res) -> {}).build();
    }

    private Map<String, Object> post(RestClient c, String uri, Object body) {
        RestClient.RequestBodySpec spec = c.post().uri(uri).contentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> r = (body == null ? spec : spec.body(body)).retrieve().toEntity(MAP);
        assertThat(r.getStatusCode().is2xxSuccessful()).as("POST %s -> %s %s", uri, r.getStatusCode(), r.getBody()).isTrue();
        return r.getBody();
    }

    private Map<String, Object> get(RestClient c, String uri) {
        ResponseEntity<Map<String, Object>> r = c.get().uri(uri).retrieve().toEntity(MAP);
        assertThat(r.getStatusCode().is2xxSuccessful()).as("GET %s -> %s %s", uri, r.getStatusCode(), r.getBody()).isTrue();
        return r.getBody();
    }

    private String leitor(String nome) {
        return (String) post(client, "/api/v1/leitores",
                Map.of("nome", nome, "email", nome + UUID.randomUUID() + "@x.com")).get("id");
    }

    private Map<String, Object> livroBody(String isbn) {
        return Map.of("title", "Clean Code", "author", "Robert Martin", "isbn", isbn);
    }
}
