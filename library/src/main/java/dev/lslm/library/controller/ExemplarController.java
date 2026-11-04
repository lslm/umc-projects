package dev.lslm.library.controller;

import dev.lslm.library.domain.Exemplar.Situacao;
import dev.lslm.library.dto.AcervoDtos.ExemplarRequest;
import dev.lslm.library.dto.AcervoDtos.ExemplarResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.ExemplarService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** RF05 - exemplares fisicos. */
@RestController
@RequestMapping("/api/v1/exemplares")
public class ExemplarController {

    private final ExemplarService service;

    public ExemplarController(ExemplarService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExemplarResponse> criar(@RequestBody ExemplarRequest req) {
        ExemplarResponse r = ExemplarResponse.from(service.criar(req));
        return ResponseEntity.created(URI.create("/api/v1/exemplares/" + r.id())).body(r);
    }

    @GetMapping
    public PageResponse<ExemplarResponse> listar(@RequestParam(required = false) UUID livroId,
                                                 @RequestParam(required = false) Situacao situacao,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return PageResponse.of(service.listar(livroId, situacao, Paginacao.de(page, perPage, Sort.by("criadoEm"))),
                ExemplarResponse::from);
    }

    @GetMapping("/{id}")
    public ExemplarResponse buscar(@PathVariable UUID id) {
        return ExemplarResponse.from(service.buscar(id));
    }

    @PutMapping("/{id}")
    public ExemplarResponse atualizar(@PathVariable UUID id, @RequestBody ExemplarRequest req) {
        return ExemplarResponse.from(service.atualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
