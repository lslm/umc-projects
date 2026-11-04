package dev.lslm.library.controller;

import dev.lslm.library.domain.Emprestimo.Status;
import dev.lslm.library.dto.CirculacaoDtos.DevolucaoRequest;
import dev.lslm.library.dto.CirculacaoDtos.DevolucaoResponse;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoRequest;
import dev.lslm.library.dto.CirculacaoDtos.EmprestimoResponse;
import dev.lslm.library.dto.PageResponse;
import dev.lslm.library.service.EmprestimoService;
import dev.lslm.library.support.Paginacao;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** RF08 (emprestimos), RF09 (devolucoes) e RF10 (renovacoes). */
@RestController
@RequestMapping("/api/v1/emprestimos")
public class EmprestimoController {

    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmprestimoResponse> criar(@RequestBody EmprestimoRequest req) {
        EmprestimoResponse r = service.criar(req);
        return ResponseEntity.created(URI.create("/api/v1/emprestimos/" + r.id())).body(r);
    }

    @GetMapping
    public PageResponse<EmprestimoResponse> listar(@RequestParam(required = false) Status status,
                                                   @RequestParam(required = false) UUID leitorId,
                                                   @RequestParam(required = false) UUID livroId,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        var p = service.listar(status, leitorId, livroId, Paginacao.de(page, perPage, Sort.by("emprestadoEm").descending()));
        return PageResponse.ofList(service.toResponses(p.getContent()), p);
    }

    @GetMapping("/{id}")
    public EmprestimoResponse buscar(@PathVariable UUID id) {
        return service.detalhar(id);
    }

    /** Corpo opcional: { "devolvidoEm": "2026-08-12T14:30:00Z" } para devolucoes retroativas. */
    @PostMapping("/{id}/devolucao")
    public DevolucaoResponse devolver(@PathVariable UUID id, @RequestBody(required = false) DevolucaoRequest req) {
        return service.devolver(id, req != null ? req.devolvidoEm() : null);
    }

    @PostMapping("/{id}/renovacao")
    public EmprestimoResponse renovar(@PathVariable UUID id) {
        return service.renovar(id);
    }
}
