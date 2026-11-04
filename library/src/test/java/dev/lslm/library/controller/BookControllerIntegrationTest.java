package dev.lslm.library.controller;

import dev.lslm.library.dto.BookRequest;
import dev.lslm.library.dto.BookResponse;
import dev.lslm.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integracao do BookController.
 *
 * Fluxo real exercitado em cada teste:
 * RestClient -> HTTP real -> BookController -> BookService -> BookRepository -> H2 em memoria
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    private static final String BASE_PATH = "/api/v1/livros";

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository bookRepository;

    private RestClient client;

    @BeforeEach
    void configurarCliente() {
        // Cria uma biblioteca (tenant) e a informa em todas as requisicoes via X-Biblioteca-Id
        RestClient semTenant = RestClient.builder().baseUrl("http://localhost:" + port).build();
        Map<String, Object> biblioteca = semTenant.post().uri("/api/v1/bibliotecas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", "Biblioteca Teste " + System.nanoTime()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("X-Biblioteca-Id", String.valueOf(biblioteca.get("id")))
                // Nao lanca excecao em 4xx/5xx: alguns testes esperam 400 ou 404
                .defaultStatusHandler(status -> true, (req, res) -> {})
                .build();
    }

    // ---------- GET /api/v1/livros ----------

    @Test
    void listar_comLivroCadastrado_deveRetornar200ComLivroNaLista() {
        long ts = System.nanoTime();
        UUID id = criarLivro("Livro Listagem " + ts, "BK-LST-" + ts);

        ResponseEntity<List<BookResponse>> response = client.get().uri(BASE_PATH)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(BookResponse::id)
                .contains(id);
    }

    // ---------- GET /api/v1/livros/{id} ----------

    @Test
    void buscarPorId_comIdExistente_deveRetornar200ComTodosOsCampos() {
        long ts = System.nanoTime();
        UUID id = criarLivro("Clean Code " + ts, "BK-GET-" + ts);

        ResponseEntity<BookResponse> response = client.get().uri(BASE_PATH + "/" + id)
                .retrieve()
                .toEntity(BookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookResponse body = response.getBody();
        assertThat(body.id()).isEqualTo(id);
        assertThat(body.title()).isEqualTo("Clean Code " + ts);
        assertThat(body.author()).isEqualTo("Robert C. Martin");
        assertThat(body.isbn()).isEqualTo("BK-GET-" + ts);
        assertThat(body.publisher()).isEqualTo("Editora Teste");
        assertThat(body.edition()).isEqualTo(1);
        assertThat(body.publishedAt()).isEqualTo(LocalDate.of(2008, 8, 1));
        assertThat(body.language()).isEqualTo("pt-BR");
        assertThat(body.pages()).isEqualTo(464);
    }

    @Test
    void buscarPorId_comIdInexistente_deveRetornar404ComMensagem() {
        UUID idInexistente = UUID.randomUUID();

        ResponseEntity<Map<String, String>> response = client.get().uri(BASE_PATH + "/" + idInexistente)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message")).contains(idInexistente.toString());
    }

    // ---------- POST /api/v1/livros ----------

    @Test
    void criar_comDadosValidos_deveRetornar201ComIdELocation() {
        long ts = System.nanoTime();
        BookRequest request = buildBookRequest("Novo Livro " + ts, "BK-NEW-" + ts);

        ResponseEntity<BookResponse> response = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookResponse body = response.getBody();
        assertThat(body.id()).isNotNull();
        assertThat(body.title()).isEqualTo("Novo Livro " + ts);
        assertThat(body.isbn()).isEqualTo("BK-NEW-" + ts);
        assertThat(response.getHeaders().getLocation())
                .hasToString(BASE_PATH + "/" + body.id());

        // Checa persistencia: o livro realmente foi gravado no banco
        assertThat(bookRepository.findById(body.id())).isPresent();
    }

    @Test
    void criar_comIsbnDuplicado_deveRetornar400ComMensagem() {
        long ts = System.nanoTime();
        String isbn = "BK-DUP-" + ts;
        criarLivro("Original " + ts, isbn);

        ResponseEntity<Map<String, String>> response = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBookRequest("Duplicado " + ts, isbn))
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).contains(isbn);
    }

    // ---------- PUT /api/v1/livros/{id} ----------

    @Test
    void atualizar_comIdExistente_deveRetornar200ComTituloAtualizado() {
        long ts = System.nanoTime();
        UUID id = criarLivro("Original " + ts, "BK-UPD-" + ts);
        BookRequest updateRequest = buildBookRequest("Atualizado " + ts, "BK-UPD-" + ts);

        ResponseEntity<BookResponse> response = client.put().uri(BASE_PATH + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .toEntity(BookResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(id);
        assertThat(response.getBody().title()).isEqualTo("Atualizado " + ts);

        // Checa persistencia: o titulo novo foi gravado no banco
        assertThat(bookRepository.findById(id))
                .get()
                .extracting(book -> book.getTitle())
                .isEqualTo("Atualizado " + ts);
    }

    @Test
    void atualizar_comIdInexistente_deveRetornar404() {
        long ts = System.nanoTime();

        ResponseEntity<Void> response = client.put().uri(BASE_PATH + "/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBookRequest("Fantasma " + ts, "BK-GHO-" + ts))
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- DELETE /api/v1/livros/{id} ----------

    @Test
    void remover_comIdExistente_deveRetornar204ELivroDeixaDeExistir() {
        UUID id = criarLivro("Para Deletar", "BK-DEL-" + System.nanoTime());

        ResponseEntity<Void> response = client.delete().uri(BASE_PATH + "/" + id)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Efeito observavel: buscar o mesmo id agora retorna 404
        ResponseEntity<Void> busca = client.get().uri(BASE_PATH + "/" + id)
                .retrieve()
                .toBodilessEntity();
        assertThat(busca.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void remover_comIdInexistente_deveRetornar404() {
        ResponseEntity<Void> response = client.delete().uri(BASE_PATH + "/" + UUID.randomUUID())
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- Helpers ----------

    private BookRequest buildBookRequest(String title, String isbn) {
        return new BookRequest(
                title,
                "Resumo de teste",
                "Robert C. Martin",
                isbn,
                "Editora Teste",
                1,
                LocalDate.of(2008, 8, 1),
                "pt-BR",
                464
        );
    }

    /** Cria um livro pelo endpoint real (nao por SQL direto) e devolve o id gerado. */
    private UUID criarLivro(String title, String isbn) {
        return client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBookRequest(title, isbn))
                .retrieve()
                .toEntity(BookResponse.class)
                .getBody()
                .id();
    }
}
